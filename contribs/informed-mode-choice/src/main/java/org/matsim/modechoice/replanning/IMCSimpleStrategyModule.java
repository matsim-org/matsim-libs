package org.matsim.modechoice.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;

/**
 * Module for {@link IMCSelectFromGenerator}.
 */
public class IMCSimpleStrategyModule extends AbstractMultithreadedModule {

	private final CandidateGenerator generator;
	private final Selector<PlanCandidate> selector;

	public IMCSimpleStrategyModule(GlobalConfigGroup globalConfigGroup, CandidateGenerator generator, Selector<PlanCandidate> selector) {
		super(globalConfigGroup);
		this.generator = generator;
		this.selector = selector;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new IMCSelectFromGenerator(generator, selector);
	}

}
