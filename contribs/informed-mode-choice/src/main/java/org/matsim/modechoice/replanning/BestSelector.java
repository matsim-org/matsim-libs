package org.matsim.modechoice.replanning;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.modechoice.PlanCandidate;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Select best, assuming the candidates are ordered.
 */
public class BestSelector implements Selector<PlanCandidate> {

	@Nullable
	@Override
	public PlanCandidate select(Collection<PlanCandidate> candidates) {
		return candidates.stream().findFirst().orElse(null);
	}
}
