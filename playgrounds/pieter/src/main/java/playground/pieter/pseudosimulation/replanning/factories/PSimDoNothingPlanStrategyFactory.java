package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

public class PSimDoNothingPlanStrategyFactory implements PlanStrategyFactory {

	private final PSimControler controler;

	public PSimDoNothingPlanStrategyFactory(PSimControler controler) {
		this.controler = controler;
	}

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario,
			EventsManager eventsManager) {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}

}
