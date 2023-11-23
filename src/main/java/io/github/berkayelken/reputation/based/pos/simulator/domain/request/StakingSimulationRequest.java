package io.github.berkayelken.reputation.based.pos.simulator.domain.request;

import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.Validator;
import lombok.AccessLevel;
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
public class StakingSimulationRequest {
	private static final long DEFAULT_COIN_VOLUME = (long) Math.pow(10, 5);
	private static final int DEFAULT_COIN_RATIO = 16;
	@Getter (AccessLevel.NONE)
	private long desiredCoinVolumeOfNetwork;
	private double majorityRatio;
	private double attackerRatioWithoutMajor;
	private boolean majorValidatorAsAnAttacker;
	private long desiredBlockCount;
	private boolean needToSlashing;
	@Getter (AccessLevel.NONE)
	private int availableCoinRatioDivider;
	@Getter (AccessLevel.NONE)
	private String excelFileName;

	public static StakingSimulationRequest createRequestForForkChanger() {
		return StakingSimulationRequest.builder().majorityRatio(0.05).attackerRatioWithoutMajor(0.15)
				.majorValidatorAsAnAttacker(false).desiredBlockCount(256).availableCoinRatioDivider(64).build();
	}

	public long getDesiredCoinVolumeOfNetwork() {
		if (desiredCoinVolumeOfNetwork == 0) {
			return DEFAULT_COIN_VOLUME;
		}
		return desiredCoinVolumeOfNetwork;
	}

	public int getAvailableCoinRatioDivider() {
		if (availableCoinRatioDivider == 0) {
			return DEFAULT_COIN_RATIO;
		}
		return availableCoinRatioDivider;
	}

	public long calculateAndGetDummyValidatorSize() {
		return (getDesiredCoinVolumeOfNetwork() - calculateAndGetMajorValidatorCoinSize()) / Validator.DUMMY_COIN_LIST_SIZE;
	}

	public long calculateAndGetMajorValidatorCoinSize() {
		return (long) (getDesiredCoinVolumeOfNetwork() * majorityRatio);
	}

	public long calculateAndGetDummyAttackerSize() {
		return (long) Math.floor(calculateAndGetDummyValidatorSize() * attackerRatioWithoutMajor);
	}

	public long calculateAndGetDummyTrustedSize() {
		return calculateAndGetDummyValidatorSize() - calculateAndGetDummyAttackerSize();
	}

	public String getExcelFileName() {
		String attacker = majorValidatorAsAnAttacker ? "attacker" : "trusted";
		return "/Users/berkayyelken/Desktop/thesis/" + excelFileName + "_major_" + attacker + "_" + majorityRatio
				+ "_other_attackers_" + attackerRatioWithoutMajor + ".xlsx";
	}
}
