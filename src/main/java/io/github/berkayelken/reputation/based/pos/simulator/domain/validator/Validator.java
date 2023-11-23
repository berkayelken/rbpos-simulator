package io.github.berkayelken.reputation.based.pos.simulator.domain.validator;

import io.github.berkayelken.reputation.based.pos.simulator.domain.Coin;
import io.github.berkayelken.reputation.based.pos.simulator.domain.CoinAge;
import io.github.berkayelken.reputation.based.pos.simulator.domain.attempt.AttemptType;
import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.service.factory.StakedCoinsFactory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Validator {
	public static final long DUMMY_COIN_LIST_SIZE = 100L;
	private long id;
	private boolean major;
	private boolean attacker;
	private long successfulBlockCreationAttemptCount;
	private long unsuccessfulBlockCreationAttemptCount;
	private long initialStakedCoinSize;
	private List<Coin> stakedCoins;
	private AttemptType lastAttempt;
	private long lastAttemptedBlock;
	private int availableCoinRatioDivider;

	public static Validator createDummyAttackerInstance(long id, StakedCoinsFactory stakedCoinsFactory,
			int availableCoinRatioDivider) {
		return Validator.builder().id(id).attacker(true).major(false)
				.stakedCoins(stakedCoinsFactory.createStakedCoins(DUMMY_COIN_LIST_SIZE)).initialStakedCoinSize(DUMMY_COIN_LIST_SIZE)
				.availableCoinRatioDivider(availableCoinRatioDivider).build();
	}

	public static Validator createDummyTrustedInstance(long id, StakedCoinsFactory stakedCoinsFactory,
			int availableCoinRatioDivider) {
		return Validator.builder().id(id).attacker(false).major(false)
				.stakedCoins(stakedCoinsFactory.createStakedCoins(DUMMY_COIN_LIST_SIZE)).initialStakedCoinSize(DUMMY_COIN_LIST_SIZE)
				.availableCoinRatioDivider(availableCoinRatioDivider).build();
	}

	public static Validator createMajorAttackerInstance(long id, StakedCoinsFactory stakedCoinsFactory,
			StakingSimulationRequest request) {
		long desiredCoinMajority = request.calculateAndGetMajorValidatorCoinSize();
		return Validator.builder().id(id).attacker(true).major(true)
				.stakedCoins(stakedCoinsFactory.createStakedCoins(desiredCoinMajority)).initialStakedCoinSize(desiredCoinMajority)
				.availableCoinRatioDivider(request.getAvailableCoinRatioDivider()).build();
	}

	public static Validator createMajorTrustedInstance(long id, StakedCoinsFactory stakedCoinsFactory,
			StakingSimulationRequest request) {
		long desiredCoinMajority = request.calculateAndGetMajorValidatorCoinSize();
		return Validator.builder().id(id).attacker(false).major(true)
				.stakedCoins(stakedCoinsFactory.createStakedCoins(desiredCoinMajority)).initialStakedCoinSize(desiredCoinMajority)
				.availableCoinRatioDivider(request.getAvailableCoinRatioDivider()).build();
	}

	public static Validator getLocalValidator(ValidatorSituationEntity entity) {
		return entity.getAllValidators().stream().max((v1, v2) -> v1.compare(v2, entity)).orElse(null);
	}

	public double getReputationMultiplier() {
		if (successfulBlockCreationAttemptCount == 0 && unsuccessfulBlockCreationAttemptCount == 0) {
			return 1.0;
		} else if (successfulBlockCreationAttemptCount == 0 && unsuccessfulBlockCreationAttemptCount > 0) {
			return 1.0 / unsuccessfulBlockCreationAttemptCount;
		} else if (successfulBlockCreationAttemptCount > 0 && unsuccessfulBlockCreationAttemptCount == 0) {
			return successfulBlockCreationAttemptCount * 1.0;
		} else {
			return 1.0 * successfulBlockCreationAttemptCount / unsuccessfulBlockCreationAttemptCount;
		}
	}

	public ValidatorSituationPerBlock createMajorSituation(ValidatorSituationEntity entity) {
		double totalCoinAge = entity.getAllValidators().stream()
				.map(validator -> validator.calculateTotalCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock()))
				.reduce(Double::sum).orElse(0d);
		double availableCoinAge = calculateAvailableCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock());

		return ValidatorSituationPerBlock.builder().id(entity.getAttemptNo()).validatorId(id).coinAge(availableCoinAge)
				.coinAgeMajorityRatio(availableCoinAge / totalCoinAge).major(major).build();
	}

	public ValidatorSituationPerBlock createBlockAttempt(ValidatorSituationEntity entity) {
		lastAttempt = AttemptType.BLOCK_CREATION;
		handleLastAttemptedBlock(entity.getLastCreatedBlock());
		List<Validator> otherValidators = new ArrayList<>();
		entity.getAllValidators().stream().filter(validator -> validator.id != id).forEachOrdered(otherValidators::add);
		double availableCoinAge = calculateAvailableCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock());
		double totalCoinAge = entity.getAllValidators().stream()
				.map(validator -> validator.calculateTotalCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock()))
				.reduce(Double::sum).orElse(0d);
		boolean enoughSupport = discussionCoinAge(otherValidators, entity, availableCoinAge);

		if (entity.isNeedToSlashing()) {
			slashCoins(enoughSupport);
		}
		double availableCoinAgeAfterSlashing = calculateAvailableCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock());

		entity.getAllValidators().stream().filter(validator -> validator.lastAttemptedBlock > entity.getLastCreatedBlock())
				.forEach(validator -> validator.useCoins(entity.getCoinAge(), enoughSupport, entity.getLastCreatedBlock()));

		return ValidatorSituationPerBlock.builder().id(entity.getAttemptNo()).validatorId(id).blockCreationAttempted(true)
				.blockCreationSuccessful(enoughSupport).coinAge(availableCoinAgeAfterSlashing)
				.coinAgeMajorityRatio(availableCoinAgeAfterSlashing / totalCoinAge).major(major).slashed(!enoughSupport).build();
	}

	public void increaseTimeOfCoins() {
		stakedCoins.forEach(Coin::increaseTime);
	}

	public double calculateAvailableCoinAge(CoinAge coinAge, long lastCreatedBlock) {
		return stakedCoins.stream().map(coin -> coin.calculateAnAddCoinAge(coinAge, lastCreatedBlock, this)).sorted()
				.limit(getLimit(coinAge)).map(Coin::getLastCalculatedCoinAge).reduce(Double::sum).orElse(0.0d);
	}

	public double calculateTotalCoinAge(CoinAge coinAge, long lastCreatedBlock) {
		return stakedCoins.stream().map(coin -> coinAge.calculate(coin, lastCreatedBlock, this)).reduce(Double::sum).orElse(0.0d);
	}

	public void addNegativeContribution(long lastAttemptedBlock) {
		stakedCoins.forEach(coin -> coin.addNegativeDiscussionAttempt(lastAttemptedBlock));
	}

	public void slashCoins(boolean enoughSupport) {
		if (enoughSupport) {
			successfulBlockCreationAttemptCount++;
			return;
		}
		unsuccessfulBlockCreationAttemptCount++;
		slashCoins();
	}

	public void slashCoins() {
		if (unsuccessfulBlockCreationAttemptCount == 32) {
			stakedCoins = new ArrayList<>();
		} else {
			reduceCoinList();
		}
	}

	private void reduceCoinList() {
		long newSize = stakedCoins.size() - initialStakedCoinSize / 32;
		List<Coin> reducedCoinList = new ArrayList<>();
		if (newSize <= 0) {
			stakedCoins = reducedCoinList;
		} else {
			stakedCoins.stream().limit(newSize).forEachOrdered(reducedCoinList::add);
			stakedCoins = reducedCoinList;
		}
	}

	private void useCoins(CoinAge coinAge, boolean blockCreated, long lastCreatedBlock) {
		stakedCoins.stream().sorted().limit(getLimit(coinAge))
				.forEach(coin -> coin.useCoin(lastAttempt, blockCreated, lastCreatedBlock));
	}

	private boolean discussionCoinAge(List<Validator> otherValidators, ValidatorSituationEntity entity, double availableCoinAge) {
		if (!entity.isDiscussionCoinAge()) {
			return true;
		}
		double reportedCoinAge = otherValidators.stream()
				.map(validator -> validator.reportBlockAttempt(entity.getCoinAge(), entity.getLastCreatedBlock(), attacker))
				.reduce(Double::sum).orElse(0d);
		double supportedCoinAge = 0;
		double neededCoinAge = availableCoinAge + reportedCoinAge;
		for (Validator otherValidator : otherValidators) {
			supportedCoinAge += otherValidator.supportBlockAttempt(entity.getCoinAge(), entity.getLastCreatedBlock(), attacker);
			if (supportedCoinAge >= neededCoinAge) {
				return true;
			}
		}

		return false;
	}

	private double supportBlockAttempt(CoinAge coinAge, long lastCreatedBlock, boolean blockCreatorAttacker) {
		if (blockCreatorAttacker == attacker) {
			return 0d;
		}
		lastAttempt = AttemptType.SUPPORT;
		handleLastAttemptedBlock(lastCreatedBlock);
		return stakedCoins.stream().map(coin -> coinAge.calculate(coin, lastCreatedBlock, this)).reduce(Double::sum).orElse(0d);
	}

	private double reportBlockAttempt(CoinAge coinAge, long lastCreatedBlock, boolean blockCreatorAttacker) {
		if (blockCreatorAttacker != attacker) {
			return 0d;
		}
		lastAttempt = AttemptType.REPORT;
		handleLastAttemptedBlock(lastCreatedBlock);
		return stakedCoins.stream().map(coin -> coinAge.calculate(coin, lastCreatedBlock, this)).reduce(Double::sum).orElse(0d);
	}

	private void handleLastAttemptedBlock(long lastCreatedBlock) {
		lastAttemptedBlock = lastCreatedBlock + 1;
	}

	private int compare(Validator other, ValidatorSituationEntity entity) {
		if (other == this) {
			return 0;
		}
		double otherCoinAge = other.calculateAvailableCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock());
		double instanceCoinAge = calculateAvailableCoinAge(entity.getCoinAge(), entity.getLastCreatedBlock());

		return Double.compare(instanceCoinAge, otherCoinAge);
	}

	private long getLimit(CoinAge coinAge) {
		if (coinAge == CoinAge.PURE) {
			return stakedCoins.size();
		}
		return stakedCoins.size() / availableCoinRatioDivider;
	}

}
