package io.github.berkayelken.reputation.based.pos.simulator.domain.response;

import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.ValidatorSituationPerBlock;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor (access = AccessLevel.PRIVATE)
public class StakingSimulationResponse {
	private long populationSize;
	private long attemptCount;
	private long initialTotalCoinSize;
	private long initialMajorValidatorCoinSize;
	private long majorValidatorSlashedBlock;
	private boolean majorValidatorSlashed;
	private List<ValidatorSituationPerBlock> majorValidatorSituations = new ArrayList<>();
	private List<ValidatorSituationPerBlock> blockCreatorSituations = new ArrayList<>();
	private List<String> majorValidatorCoinAgeRatios;
	private boolean forkLocked;
	private long finalChainSize;

	public static StakingSimulationResponse createInstance(StakingSimulationRequest request) {
		StakingSimulationResponse response = new StakingSimulationResponse();

		response.populationSize = request.calculateAndGetDummyValidatorSize() + 1;
		response.initialTotalCoinSize = request.getDesiredCoinVolumeOfNetwork();
		response.initialMajorValidatorCoinSize = request.calculateAndGetMajorValidatorCoinSize();

		return response;
	}

	public void addMajorValidatorSituation(ValidatorSituationPerBlock majorValidatorSituationForBlock) {
		majorValidatorSituations.add(majorValidatorSituationForBlock);
	}

	public void addBlockCreatorSituation(ValidatorSituationPerBlock blockCreatorSituationForBlock) {
		blockCreatorSituations.add(blockCreatorSituationForBlock);
	}

	public void increaseChainSize() {
		finalChainSize++;
	}

	public void handleMajorValidatorCoinAgeRatiosForSimultaneous() {
		majorValidatorCoinAgeRatios = majorValidatorSituations.stream().map(ValidatorSituationPerBlock::getCoinAgeMajorityRatio)
				.map(ratio -> ratio * 50).map(ratio -> String.format("%.10f", ratio)).collect(Collectors.toList());
	}

	public void handleMajorValidatorCoinAgeRatios() {
		majorValidatorCoinAgeRatios = majorValidatorSituations.stream().map(ValidatorSituationPerBlock::getCoinAgeMajorityRatio)
				.map(ratio -> ratio * 100).map(ratio -> String.format("%.10f", ratio)).collect(Collectors.toList());
	}
}
