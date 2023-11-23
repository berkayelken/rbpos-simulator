package io.github.berkayelken.reputation.based.pos.simulator.service;

import io.github.berkayelken.reputation.based.pos.simulator.domain.CoinAge;
import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.response.StakingSimulationResponse;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.Validator;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.ValidatorSituationEntity;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.ValidatorSituationPerBlock;
import io.github.berkayelken.reputation.based.pos.simulator.service.factory.ValidatorsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PureStakingService implements StakingService {
	private final ValidatorsFactory validatorsFactory;

	@Autowired
	public PureStakingService(ValidatorsFactory validatorsFactory) {
		this.validatorsFactory = validatorsFactory;
	}

	@Override
	public StakingSimulationResponse runStakingScenario(StakingSimulationRequest request) {
		StakingSimulationResponse response = StakingSimulationResponse.createInstance(request);
		List<Validator> validators = validatorsFactory.createValidatorList(request);
		iterateTheBlocks(response, request, validators);
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

			validators.forEach(Validator::increaseTimeOfCoins);
			int lastCreated = i + 1;
			double coinAgeOfMajor = major.calculateAvailableCoinAge(CoinAge.PURE, lastCreated);
			double totalCoinAge = validators.stream().map(validator -> validator.calculateTotalCoinAge(CoinAge.PURE, lastCreated))
					.reduce(Double::sum).orElse(1.0d);
			double ratio = coinAgeOfMajor / totalCoinAge * 100;
			coinAgeRatios.put(lastCreated, String.format("%.10f", ratio));
		}

		return coinAgeRatios;
	}

	@Override
	public Map<Integer, String> getMajorityRatioForSimultaneousStaking(StakingSimulationRequest request) {
		StakingSimulationResponse response = runStakingScenario(request);
		AtomicInteger indexer = new AtomicInteger();
		response.handleMajorValidatorCoinAgeRatiosForSimultaneous();
		return response.getMajorValidatorCoinAgeRatios().stream()
				.collect(Collectors.toMap(ratio -> indexer.incrementAndGet(), ratio -> ratio));
	}

	private void iterateTheBlocks(StakingSimulationResponse response, StakingSimulationRequest request,
			List<Validator> validators) {
		ValidatorSituationEntity entity = ValidatorSituationEntity.builder().allValidators(validators).coinAge(CoinAge.PURE)
				.needToSlashing(request.isNeedToSlashing()).build();
		Validator major = entity.getMajorValidator();
		AtomicLong blockIndexer = new AtomicLong();
		Stream.generate(blockIndexer::getAndIncrement).limit(request.getDesiredBlockCount())
				.forEachOrdered(blockNo -> createBlock(response, entity, major, blockNo));
	}

	private void createBlock(StakingSimulationResponse response, ValidatorSituationEntity entity, Validator major, long blockNo) {
		Validator localValidator = Validator.getLocalValidator(entity);
		entity.setBlockNo(blockNo);
		entity.getAllValidators().forEach(Validator::increaseTimeOfCoins);

		ValidatorSituationPerBlock majorSituation = major.createMajorSituation(entity);
		ValidatorSituationPerBlock situation = localValidator.createBlockAttempt(entity);

		response.addBlockCreatorSituation(situation);
		if (situation.getValidatorId() == majorSituation.getValidatorId()) {
			response.addMajorValidatorSituation(situation);
		} else {
			response.addMajorValidatorSituation(majorSituation);
		}
		response.increaseChainSize();
	}

}
