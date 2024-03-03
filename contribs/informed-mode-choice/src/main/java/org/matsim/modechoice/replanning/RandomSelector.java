package org.matsim.modechoice.replanning;

import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

/**
 * Select randomly from candidates.
 */
public class RandomSelector implements Selector<PlanCandidate> {

	private final Random rnd;

	public RandomSelector(Random rnd) {
		this.rnd = rnd;
	}

	@Nullable
	@Override
	public PlanCandidate select(Collection<PlanCandidate> candidates) {
		int skip = rnd.nextInt(candidates.size());
		return candidates.stream().skip(skip).findFirst().orElse(null);
	}
}
