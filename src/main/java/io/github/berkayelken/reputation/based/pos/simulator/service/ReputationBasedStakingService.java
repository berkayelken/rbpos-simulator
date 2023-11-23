package io.github.berkayelken.reputation.based.pos.simulator.service;

import io.github.berkayelken.reputation.based.pos.simulator.domain.CoinAge;
import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.response.StakingSimulationResponse;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.Validator;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.ValidatorSituationEntity;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.ValidatorSituationPerBlock;
import io.github.berkayelken.reputation.based.pos.simulator.exception.ByzantineException;
import io.github.berkayelken.reputation.based.pos.simulator.service.factory.ValidatorsFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ReputationBasedStakingService implements StakingService {
	private final ValidatorsFactory validatorsFactory;

	@Autowired
	public ReputationBasedStakingService(ValidatorsFactory validatorsFactory) {
		this.validatorsFactory = validatorsFactory;
	}

	@Override
	public StakingSimulationResponse runStakingScenario(StakingSimulationRequest request) {
		StakingSimulationResponse response = StakingSimulationResponse.createInstance(request);
		List<Validator> validators = validatorsFactory.createValidatorList(request);

		try {
			iterateTheBlocks(response, request, validators);
		} catch (ByzantineException e) {
			response.setForkLocked(true);
			log.error("Chain fork is locked", e);
		} catch (StackOverflowError e) {
			log.warn("iteration {} response: {}", response.getFinalChainSize(), response);
			log.error("Stackoverflow", e);
		}

		response.handleMajorValidatorCoinAgeRatios();

		return response;
	}

	@Override
	public Map<Integer, String> getMajorityRatioForForkChanger(StakingSimulationRequest request) {
		List<Validator> validators = validatorsFactory.createValidatorList(request);

		Validator major = validators.stream().filter(Validator::isMajor).findFirst().orElse(null);
		Objects.requireNonNull(major);

		Map<Integer, String> coinAgeRatios = new HashMap<>();

		for (int i = 0; i < request.getDesiredBlockCount(); i++) {
			major.addNegativeContribution(i);
			major.increaseTimeOfCoins();
			validators.stream().filter(validator -> !validator.isMajor()).forEach(Validator::increaseTimeOfCoins);
			int lastCreated = i + 1;
			double coinAgeOfMajor = major.calculateAvailableCoinAge(CoinAge.REPUTATION, lastCreated);
			double totalCoinAge = validators.stream()
					.map(validator -> validator.calculateTotalCoinAge(CoinAge.REPUTATION, lastCreated)).reduce(Double::sum)
					.orElse(1.0d);
			double ratio = coinAgeOfMajor / totalCoinAge * 100;
			coinAgeRatios.put(lastCreated, String.format("%.10f", ratio));
		}

		return coinAgeRatios;
	}

	@Override
	public Map<Integer, String> getMajorityRatioForSimultaneousStaking(StakingSimulationRequest request) {
		List<Validator> validators = validatorsFactory.createValidatorList(request);

		Validator major = validators.stream().filter(Validator::isMajor).findFirst().orElse(null);
		Objects.requireNonNull(major);

		Map<Integer, String> coinAgeRatios = new HashMap<>();
		for (int i = 0; i < request.getDesiredBlockCount(); i++) {
			validators.stream().filter(validator -> !validator.isMajor()).forEach(Validator::increaseTimeOfCoins);
			int lastCreated = i + 1;
			double coinAgeOfMajor = major.calculateAvailableCoinAge(CoinAge.REPUTATION, lastCreated);
			double totalCoinAge = validators.stream()
					.map(validator -> validator.calculateTotalCoinAge(CoinAge.REPUTATION, lastCreated)).reduce(Double::sum)
					.orElse(1.0d);
			double ratio = coinAgeOfMajor / totalCoinAge * 100;
			major.slashCoins();
			major.slashCoins(false);
			coinAgeRatios.put(lastCreated, String.format("%.10f", ratio));
			if (CollectionUtils.isEmpty(major.getStakedCoins())) {
				return coinAgeRatios;
			}
		}

		return coinAgeRatios;
	}

	private void iterateTheBlocks(StakingSimulationResponse response, StakingSimulationRequest request,
			List<Validator> validators) {

		ValidatorSituationEntity entity = ValidatorSituationEntity.builder().allValidators(validators).coinAge(CoinAge.REPUTATION)
				.needToSlashing(request.isNeedToSlashing()).build();
		for (long blockNo = 0; blockNo < request.getDesiredBlockCount(); ) {
			for (long blockNoForCheckpoint = 0; blockNoForCheckpoint < 256; blockNoForCheckpoint++) {
				Validator major = entity.getMajorValidator();
				ValidatorSituationPerBlock majorSituation = major.createMajorSituation(entity);
				entity.setBlockNo(blockNo);
				entity.setBlockNoForCheckpoint(blockNoForCheckpoint);
				ValidatorSituationPerBlock producerSituation = attemptBlockCreation(entity);
				if (producerSituation == null) {
					return;
				}

				response.addBlockCreatorSituation(producerSituation);
				if (producerSituation.getValidatorId() == majorSituation.getValidatorId()) {
					response.addMajorValidatorSituation(producerSituation);
				} else {
					response.addMajorValidatorSituation(majorSituation);
				}

				blockNo++;
				response.increaseChainSize();
				entity.setLastCreatedBlock(blockNoForCheckpoint);
			}
		}
	}

	private ValidatorSituationPerBlock attemptBlockCreation(ValidatorSituationEntity entity) {
		if (entity.isNeedToForkLock()) {
			throw new ByzantineException(entity.getBlockNo());
		}

		Validator localValidator = Validator.getLocalValidator(entity);
		entity.increaseAttemptNo();

		if (localValidator == null
				|| localValidator.calculateAvailableCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock()) == 0) {
			return null;
		}

		ValidatorSituationPerBlock situation = localValidator.createBlockAttempt(entity);
		entity.getAllValidators().forEach(Validator::increaseTimeOfCoins);
		if (!situation.isBlockCreationSuccessful()) {
			entity.increaseConsecutiveUnsuccessfulAttempt();
			return attemptBlockCreation(entity);
		}

		return situation;
	}

}
