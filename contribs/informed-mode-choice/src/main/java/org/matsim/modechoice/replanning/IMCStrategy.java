package org.matsim.modechoice.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;

import java.util.Collection;

/**
 * Choose route using a generator and selector.
 */
public class IMCStrategy implements PlanAlgorithm {

	private final CandidateGenerator generator;
	private final Selector<PlanCandidate> selector;

	public IMCStrategy(CandidateGenerator generator, Selector<PlanCandidate> selector) {
		this.generator = generator;
		this.selector = selector;
	}

	@Override
	public void run(Plan plan) {

		Collection<PlanCandidate> candidates = generator.generate(plan);
		PlanCandidate candidate = selector.select(candidates);

		if (candidate != null) {
			candidate.applyTo(plan);
		}
	}
}
