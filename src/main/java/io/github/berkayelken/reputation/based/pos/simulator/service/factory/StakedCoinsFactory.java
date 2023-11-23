package io.github.berkayelken.reputation.based.pos.simulator.service.factory;

import io.github.berkayelken.reputation.based.pos.simulator.domain.Coin;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class StakedCoinsFactory {

	public List<Coin> createStakedCoins(long size) {
		List<Coin> coinList = new ArrayList<>();
		Stream.generate(Coin::createInstance).limit(size).forEachOrdered(coinList::add);
		return coinList;
	}

}
