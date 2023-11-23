package io.github.berkayelken.reputation.based.pos.simulator.domain;

import io.github.berkayelken.reputation.based.pos.simulator.domain.attempt.AttemptType;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.Validator;

public enum CoinAge {
	PURE,
	ADVANCED,
	MANIPULATED,
	PERIOD,
	REPUTATION;

	private static double calculateAndGetAdvancedCoinAge(Coin coin, long lastBlockNumberForFork) {
		if (lastBlockNumberForFork < 16) {
			return coin.calculateCoinAge();
		}
		long distance = coin.calculateAndGetBlockDistance(lastBlockNumberForFork);
		if (coin.getLastAttemptedBlock() == 0 || distance > 16) {
			return coin.calculateCoinAge() / lastBlockNumberForFork;
		} else {
			return coin.calculateCoinAge() * distance / 32.0;
		}
	}

	private static double calculateAndGetManipulatedCoinAge(Coin coin, long lastBlockNumberForFork) {
		if (coin.getLastAttemptedBlockForDiscussion() == 0 || lastBlockNumberForFork < AttemptType.getDiscussionThreshold()) {
			return calculateAndGetAdvancedCoinAge(coin, lastBlockNumberForFork);
		}

		long distance = coin.calculateAndGetBlockDistanceForDiscussion(lastBlockNumberForFork);

		if (coin.isOutboundOfThresholdForDiscussion(distance) && coin.isSuccessfulAttemptForDiscussion()) {
			return calculateAndGetAdvancedCoinAge(coin, lastBlockNumberForFork) * 2.0 / lastBlockNumberForFork;
		} else if (coin.isOutboundOfThresholdForDiscussion(distance) && !coin.isSuccessfulAttemptForDiscussion()) {
			return calculateAndGetAdvancedCoinAge(coin, lastBlockNumberForFork) / lastBlockNumberForFork;
		} else if (coin.isSuccessfulAttemptForDiscussion()) {
			return calculateAndGetAdvancedCoinAge(coin, lastBlockNumberForFork) * distance
					/ coin.getSuccessfulDiscussionAttemptDivider();
		} else {
			return calculateAndGetAdvancedCoinAge(coin, lastBlockNumberForFork) * distance
					/ coin.getUnsuccessfulDiscussionAttemptDivider();
		}
	}

	private static double calculateAndGetPeriodCoinAge(Coin coin, long lastBlockNumberForFork) {
		if (coin.getLastAttemptedBlockForCreation() == 0) {
			return calculateAndGetManipulatedCoinAge(coin, lastBlockNumberForFork);
		}

		long distance = coin.calculateAndGetBlockDistanceForCreation(lastBlockNumberForFork);

		if (coin.isOutboundOfThresholdForCreation(distance)) {
			return calculateAndGetManipulatedCoinAge(coin, lastBlockNumberForFork);
		} else if (coin.isSuccessfulAttemptForCreation()) {
			return calculateAndGetManipulatedCoinAge(coin, lastBlockNumberForFork) * distance
					/ coin.getSuccessfulCreationAttemptDivider();
		} else {
			return calculateAndGetManipulatedCoinAge(coin, lastBlockNumberForFork) * distance
					/ coin.getUnsuccessfulCreationAttemptDivider();
		}
	}

	private static double calculateAndGetReputationCoinAge(Coin coin, long lastBlockNumberForFork, Validator validator) {
		return calculateAndGetPeriodCoinAge(coin, lastBlockNumberForFork) * validator.getReputationMultiplier();
	}

	public double calculate(Coin coin, long lastBlockNumberForFork, Validator validator) {
		switch (this) {
			case ADVANCED:
				return calculateAndGetAdvancedCoinAge(coin, lastBlockNumberForFork);
			case MANIPULATED:
				return calculateAndGetManipulatedCoinAge(coin, lastBlockNumberForFork);
			case PERIOD:
				return calculateAndGetPeriodCoinAge(coin, lastBlockNumberForFork);
			case REPUTATION:
				return calculateAndGetReputationCoinAge(coin, lastBlockNumberForFork, validator);
			case PURE:
			default:
				return coin.calculateCoinAge();

		}
	}
}
