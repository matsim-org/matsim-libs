package org.matsim.modechoice.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.modechoice.CandidateGenerator;
import org.matsim.modechoice.PlanCandidate;

import javax.inject.Provider;

/**
 * Module for {@link SelectFromGeneratorStrategy}.
 */
public class SimplePlanSelectionStrategyModule extends AbstractMultithreadedModule {

	private final Provider<? extends CandidateGenerator> generator;
	private final Selector<PlanCandidate> selector;

	public SimplePlanSelectionStrategyModule(GlobalConfigGroup globalConfigGroup, Provider<? extends CandidateGenerator> generator, Selector<PlanCandidate> selector) {
		super(globalConfigGroup);
		this.generator = generator;
		this.selector = selector;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new SelectFromGeneratorStrategy(generator.get(), selector);
	}

}
