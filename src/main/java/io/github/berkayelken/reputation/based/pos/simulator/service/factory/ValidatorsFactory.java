package io.github.berkayelken.reputation.based.pos.simulator.service.factory;

import io.github.berkayelken.reputation.based.pos.simulator.domain.request.StakingSimulationRequest;
import io.github.berkayelken.reputation.based.pos.simulator.domain.validator.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ValidatorsFactory {

	private final StakedCoinsFactory stakedCoinsFactory;

	@Autowired
	public ValidatorsFactory(StakedCoinsFactory stakedCoinsFactory) {
		this.stakedCoinsFactory = stakedCoinsFactory;
	}

	public List<Validator> createValidatorList(StakingSimulationRequest request) {
		List<Validator> validators = new ArrayList<>();
		AtomicLong idCounter = new AtomicLong();

		validators.add(createMajorValidator(request, idCounter));
		if (request.calculateAndGetDummyAttackerSize() > 0) {
			validators.addAll(createDummyAttackerValidatorList(request, idCounter));
		}
		if (request.calculateAndGetDummyTrustedSize() > 0) {
			validators.addAll(createDummyTrustedValidatorList(request, idCounter));
		}

		return validators;
	}

	private Validator createMajorValidator(StakingSimulationRequest request, AtomicLong idCounter) {
		if (request.isMajorValidatorAsAnAttacker()) {
			return Validator.createMajorAttackerInstance(idCounter.getAndIncrement(), stakedCoinsFactory,
					request);
		} else {
			return Validator.createMajorTrustedInstance(idCounter.getAndIncrement(), stakedCoinsFactory,
					request);
		}
	}

	private List<Validator> createDummyAttackerValidatorList(StakingSimulationRequest request, AtomicLong idCounter) {
		return Stream.generate(() -> Validator.createDummyAttackerInstance(idCounter.getAndIncrement(), stakedCoinsFactory, request.getAvailableCoinRatioDivider()))
				.limit(request.calculateAndGetDummyAttackerSize()).collect(Collectors.toList());
	}

	private List<Validator> createDummyTrustedValidatorList(StakingSimulationRequest request, AtomicLong idCounter) {
		return Stream.generate(() -> Validator.createDummyTrustedInstance(idCounter.getAndIncrement(), stakedCoinsFactory,
						request.getAvailableCoinRatioDivider()))
				.limit(request.calculateAndGetDummyAttackerSize()).collect(Collectors.toList());
	}
}
