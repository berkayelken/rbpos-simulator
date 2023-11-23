package io.github.berkayelken.reputation.based.pos.simulator.domain;

import io.github.berkayelken.reputation.based.pos.simulator.domain.attempt.Attempt;
import io.github.berkayelken.reputation.based.pos.simulator.domain.attempt.AttemptType;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.Validator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coin implements Comparable<Coin> {
	private static long BLOCK_DELAY_AS_MILLIS = 16000;
	private static long COIN_VALUE = 1;
	private static long GENESIS_TIME = 0;
	private long value;
	private long time;
	private Attempt discussionAttempt;
	private Attempt creationAttempt;
	private double lastCalculatedCoinAge;

	public static Coin createInstance() {
		return Coin.builder().value(COIN_VALUE).time(BLOCK_DELAY_AS_MILLIS).build();
	}

	public double calculateCoinAge() {
		return value * time * 1.0;
	}

	public void useCoin(AttemptType attemptType, boolean blockCreated, long attemptedBlock) {
		setAttempt(attemptType, blockCreated, attemptedBlock);
		time = GENESIS_TIME;
	}

	public void addNegativeDiscussionAttempt(long attemptedBlock) {
		time = GENESIS_TIME;
		discussionAttempt = Attempt.createUnsuccessfulAttempt(attemptedBlock);
	}

	private void setAttempt(AttemptType attemptType, boolean blockCreated, long attemptedBlock) {
		Attempt attempt = Attempt.builder().attemptedBlock(attemptedBlock).type(attemptType).blockCreated(blockCreated).build();
		if (attemptType.isDiscussionAttempt()) {
			discussionAttempt = attempt;
		} else {
			creationAttempt = attempt;
		}
	}

	public void increaseTime() {
		time += BLOCK_DELAY_AS_MILLIS;
		lastCalculatedCoinAge = 0.0d;
	}

	public long getSuccessfulDiscussionAttemptDivider() {
		return discussionAttempt.getSuccessfulAttemptDivider();
	}

	public long getUnsuccessfulDiscussionAttemptDivider() {
		return discussionAttempt.getUnsuccessfulAttemptDivider();
	}

	public long getSuccessfulCreationAttemptDivider() {
		return creationAttempt.getSuccessfulAttemptDivider();
	}

	public long getUnsuccessfulCreationAttemptDivider() {
		return creationAttempt.getUnsuccessfulAttemptDivider();
	}

	public long calculateAndGetBlockDistance(long lastBlockNumberForFork) {
		return Math.max(calculateAndGetBlockDistanceForDiscussion(lastBlockNumberForFork),
				calculateAndGetBlockDistanceForCreation(lastBlockNumberForFork));
	}

	public long calculateAndGetBlockDistanceForDiscussion(long lastBlockNumberForFork) {
		if (discussionAttempt == null) {
			return 0;
		}
		return discussionAttempt.calculateAndGetBlockDistance(lastBlockNumberForFork);
	}

	public long calculateAndGetBlockDistanceForCreation(long lastBlockNumberForFork) {
		if (creationAttempt == null) {
			return 0;
		}
		return creationAttempt.calculateAndGetBlockDistance(lastBlockNumberForFork);
	}

	public long getLastAttemptedBlock() {
		return Math.max(getLastAttemptedBlockForDiscussion(), getLastAttemptedBlockForCreation());
	}

	public long getLastAttemptedBlockForDiscussion() {
		if (discussionAttempt == null) {
			return 0;
		}
		return discussionAttempt.getAttemptedBlock();
	}

	public long getLastAttemptedBlockForCreation() {
		if (creationAttempt == null) {
			return 0;
		}
		return creationAttempt.getAttemptedBlock();
	}

	public boolean isSuccessfulAttemptForDiscussion() {
		return discussionAttempt.isSuccessfulAttemptForDiscussion();
	}

	public boolean isSuccessfulAttemptForCreation() {
		return creationAttempt.isSuccessfulAttemptForCreation();
	}

	public boolean isOutboundOfThresholdForDiscussion(long distance) {
		return !discussionAttempt.isInboundOfThresholdForDiscussion(distance);
	}

	public boolean isOutboundOfThresholdForCreation(long distance) {
		return !creationAttempt.isInboundOfThresholdForCreation(distance);
	}

	public Coin calculateAnAddCoinAge(CoinAge coinAge, long lastCreatedBlock, Validator validator) {
		lastCalculatedCoinAge = coinAge.calculate(this, lastCreatedBlock, validator);
		return this;
	}

	@Override
	public int compareTo(Coin o) {
		return Double.compare(o.lastCalculatedCoinAge, lastCalculatedCoinAge);
	}
}
