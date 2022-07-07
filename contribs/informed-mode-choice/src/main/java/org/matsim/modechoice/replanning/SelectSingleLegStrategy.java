package org.matsim.modechoice.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Provider;

public class SelectSingleLegStrategy extends AbstractMultithreadedModule {

	private final Provider<TopKChoicesGenerator> generator;
	private final MultinomialLogitSelector selector;

	public SelectSingleLegStrategy(GlobalConfigGroup globalConfigGroup, Provider<TopKChoicesGenerator> generator,
	                               MultinomialLogitSelector selector) {
		super(globalConfigGroup);
		this.generator = generator;
		this.selector = selector;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new Algorithm();
	}

	private final class Algorithm implements PlanAlgorithm {

		@Override
		public void run(Plan plan) {

		}
	}

}
