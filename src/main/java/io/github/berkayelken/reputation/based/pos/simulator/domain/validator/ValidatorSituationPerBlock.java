package io.github.berkayelken.reputation.based.pos.simulator.domain.validator;

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
public class ValidatorSituationPerBlock {
	private long id;
	private long validatorId;
	private double coinAge;
	private double coinAgeMajorityRatio;
	private boolean blockCreationAttempted;
	private boolean blockCreationSuccessful;
	private boolean slashed;
	private boolean major;
}
