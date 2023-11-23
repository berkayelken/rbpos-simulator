package io.github.berkayelken.reputation.based.pos.simulator.service;

import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.response.StakingSimulationResponse;

import java.util.Map;

public interface StakingService {
	StakingSimulationResponse runStakingScenario(StakingSimulationRequest request);

	Map<Integer, String> getMajorityRatioForForkChanger(StakingSimulationRequest request);

	Map<Integer, String> getMajorityRatioForSimultaneousStaking(StakingSimulationRequest request);
}
