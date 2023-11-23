package io.github.berkayelken.reputation.based.pos.simulator.domain.attempt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AttemptType {
	SUPPORT(8, 16),
	REPORT(8, 16),
	BLOCK_CREATION(32, 64);

	private final long successfulThreshold;
	private final long unsuccessfulThreshold;

	public static long getDiscussionThreshold() {
		return 8;
	}

	public boolean isDiscussionAttempt() {
		return this != BLOCK_CREATION;
	}

	public boolean isSuccessfulAttemptForDiscussion(boolean blockCreated) {
		switch (this) {
			case SUPPORT:
				return blockCreated;
			case REPORT:
				return !blockCreated;
			default:
				return false;
		}
	}

	public boolean isSuccessfulAttemptForCreation(boolean blockCreated) {
		if (this == BLOCK_CREATION) {
			return blockCreated;
		}
		return false;
	}

	public boolean isInboundOfThresholdForDiscussion(boolean blockCreated, long distance) {
		if (isSuccessfulAttemptForDiscussion(blockCreated)) {
			return successfulThreshold > distance;
		}
		return unsuccessfulThreshold > distance;
	}

	public boolean isInboundOfThresholdForCreation(boolean blockCreated, long distance) {
		if (isSuccessfulAttemptForCreation(blockCreated)) {
			return successfulThreshold > distance;
		}
		return unsuccessfulThreshold > distance;
	}

}
