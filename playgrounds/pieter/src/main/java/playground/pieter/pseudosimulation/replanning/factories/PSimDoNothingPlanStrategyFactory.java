package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

import javax.inject.Provider;

public class PSimDoNothingPlanStrategyFactory implements Provider<PlanStrategy> {

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
