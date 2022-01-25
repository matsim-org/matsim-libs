package org.matsim.contribs.discrete_mode_choice.model.utilities;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * The RandomSelector does what the name says: It collects a set of candidates
 * and then selects one randomly.
 * 
 * @author sebhoerl
 */
public class RandomSelector implements UtilitySelector {
	private final List<UtilityCandidate> candidates = new LinkedList<>();

	@Override
	public void addCandidate(UtilityCandidate candidate) {
		candidates.add(candidate);
	}

	@Override
	public Optional<UtilityCandidate> select(Random random) {
		if (candidates.size() == 0) {
			return Optional.empty();
		}

		return Optional.of(candidates.get(random.nextInt(candidates.size())));
	}

	static public class Factory implements UtilitySelectorFactory {
		@Override
		public UtilitySelector createUtilitySelector() {
			return new RandomSelector();
		}
	}
}
