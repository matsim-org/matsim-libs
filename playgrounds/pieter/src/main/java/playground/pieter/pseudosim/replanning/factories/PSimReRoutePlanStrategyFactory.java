package playground.pieter.pseudosim.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.replanning.modules.PSimPlanMarkerModule;

public class PSimReRoutePlanStrategyFactory implements PlanStrategyFactory {

	public PSimReRoutePlanStrategyFactory(PseudoSimControler controler) {
		super();
		this.controler = controler;
	}

	private PseudoSimControler controler;

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager ) {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new ReRoute(scenario));
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}


}
