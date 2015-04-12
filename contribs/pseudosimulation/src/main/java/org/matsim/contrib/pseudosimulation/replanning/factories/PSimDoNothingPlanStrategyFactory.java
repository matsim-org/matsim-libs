package org.matsim.contrib.pseudosimulation.replanning.factories;

import org.matsim.contrib.pseudosimulation.controler.PSimControler;
import org.matsim.contrib.pseudosimulation.replanning.modules.PSimPlanMarkerModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class PSimDoNothingPlanStrategyFactory implements PlanStrategyFactory {

	private final PSimControler controler;

	public PSimDoNothingPlanStrategyFactory(PSimControler controler) {
		this.controler = controler;
	}

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}

}
