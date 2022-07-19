package org.matsim.modechoice.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;
import org.matsim.modechoice.PlanModel;

import java.util.Collection;

/**
 * Choose route using a generator and selector.
 */
public class SelectFromGeneratorStrategy implements PlanAlgorithm {

	private final CandidateGenerator generator;
	private final PlanSelector selector;

	public SelectFromGeneratorStrategy(CandidateGenerator generator, PlanSelector selector) {
		this.generator = generator;
		this.selector = selector;
	}

	@Override
	public void run(Plan plan) {

		Collection<PlanCandidate> candidates = generator.generate(PlanModel.newInstance(plan));
		PlanCandidate candidate = selector.select(candidates);

		if (candidate != null) {
			candidate.applyTo(plan);
		}
	}
}
