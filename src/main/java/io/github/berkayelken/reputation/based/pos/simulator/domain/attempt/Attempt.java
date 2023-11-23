package io.github.berkayelken.reputation.based.pos.simulator.domain.attempt;

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
public class Attempt {
	private long attemptedBlock;
	private AttemptType type;
	private boolean blockCreated;

	public static Attempt createUnsuccessfulAttempt(long attemptedBlock) {
		return Attempt.builder().attemptedBlock(attemptedBlock).type(AttemptType.SUPPORT).blockCreated(false).build();
	}

	public long calculateAndGetBlockDistance(long lastBlockNumberForFork) {
		return Math.abs(attemptedBlock - lastBlockNumberForFork);
	}

	public long getSuccessfulAttemptDivider() {
		return type.getSuccessfulThreshold();
	}

	public long getUnsuccessfulAttemptDivider() {
		return type.getSuccessfulThreshold();
	}

	public boolean isSuccessfulAttemptForDiscussion() {
		return type.isSuccessfulAttemptForDiscussion(blockCreated);
	}

	public boolean isSuccessfulAttemptForCreation() {
		return type.isSuccessfulAttemptForCreation(blockCreated);
	}

	public boolean isInboundOfThresholdForDiscussion(long distance) {
		return type.isInboundOfThresholdForDiscussion(blockCreated, distance);
	}

	public boolean isInboundOfThresholdForCreation(long distance) {
		return type.isInboundOfThresholdForCreation(blockCreated, distance);
	}

}
