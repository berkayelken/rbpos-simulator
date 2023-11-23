package io.github.berkayelken.reputation.based.pos.simulator.domain.validator;

import io.github.berkayelken.reputation.based.pos.simulator.domain.CoinAge;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ValidatorSituationEntity {
	private List<Validator> allValidators;
	private CoinAge coinAge;
	private long lastCreatedBlock;
	private long attemptNo;
	private long blockNo;
	private long blockNoForCheckpoint;
	@Getter (AccessLevel.NONE)
	private boolean needToSlashing;
	private int consecutiveUnsuccessfulAttempt;

	public boolean isNeedToSlashing() {
		return coinAge != CoinAge.PURE && needToSlashing;
	}

	public void increaseAttemptNo() {
		attemptNo++;
	}

	public void increaseConsecutiveUnsuccessfulAttempt() {
		consecutiveUnsuccessfulAttempt++;
	}

	public boolean isNeedToForkLock() {
		return consecutiveUnsuccessfulAttempt > 16;
	}

	public boolean isDiscussionCoinAge() {
		return CoinAge.PURE != coinAge;
	}

	public Validator getMajorValidator() {
		Validator major = allValidators.stream().filter(Validator::isMajor).findFirst().orElse(null);
		Objects.requireNonNull(major);
		return major;
	}
}
