package io.github.berkayelken.reputation.based.pos.simulator.rest;

import io.github.berkayelken.reputation.based.pos.simulator.domain.CoinAge;
import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.response.StakingSimulationResponse;
import io.github.berkayelken.reputation.based.pos.simulator.excel.ExcelWriter;
import io.github.berkayelken.reputation.based.pos.simulator.service.PureStakingService;
import io.github.berkayelken.reputation.based.pos.simulator.service.ReputationBasedStakingService;
import io.github.berkayelken.reputation.based.pos.simulator.service.StakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping ("/stake")
public class StakingController {
	private final StakingService pureStaking;
	private final StakingService reputationBasedStaking;

	@Autowired
	public StakingController(PureStakingService pureStaking, ReputationBasedStakingService reputationBasedStaking) {
		this.pureStaking = pureStaking;
		this.reputationBasedStaking = reputationBasedStaking;
	}

	@PostMapping
	public Map<CoinAge, StakingSimulationResponse> doStaking(@RequestBody StakingSimulationRequest request) {
		ConcurrentMap<CoinAge, StakingSimulationResponse> scenarios = new ConcurrentHashMap<>();

		scenarios.put(CoinAge.PURE, pureStaking.runStakingScenario(request));
		scenarios.put(CoinAge.REPUTATION, reputationBasedStaking.runStakingScenario(request));
		ExcelWriter.writeToExcelFile(scenarios, request);

		return scenarios;
	}

	@GetMapping("/forkChangerScenario")
	public Map<CoinAge, Map<Integer, String>> changeForkForEachBlock() {
		StakingSimulationRequest request = StakingSimulationRequest.createRequestForForkChanger();

		ConcurrentMap<CoinAge, Map<Integer, String>> scenarios = new ConcurrentHashMap<>();

		scenarios.put(CoinAge.PURE, pureStaking.getMajorityRatioForForkChanger(request));
		scenarios.put(CoinAge.REPUTATION, reputationBasedStaking.getMajorityRatioForForkChanger(request));

		return scenarios;
	}

	@GetMapping("simultaneousStaking")
	public Map<CoinAge, Map<Integer, String>> doSimultaneousStaking() {
		StakingSimulationRequest request = StakingSimulationRequest.createRequestForForkChanger();
		ConcurrentMap<CoinAge, Map<Integer, String>> scenarios = new ConcurrentHashMap<>();

		scenarios.put(CoinAge.PURE, pureStaking.getMajorityRatioForSimultaneousStaking(request));
		scenarios.put(CoinAge.REPUTATION, reputationBasedStaking.getMajorityRatioForSimultaneousStaking(request));

		return scenarios;
	}
}
