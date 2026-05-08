package org.matsim.codeexamples.scoring.pseudoRandomErrors.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

public class RandomModeStrategyModule extends AbstractMultithreadedModule {
	public RandomModeStrategyModule(GlobalConfigGroup globalConfigGroup) {
		super(globalConfigGroup);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new RandomModeAlgorithm();
	}
}
