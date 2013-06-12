package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.ReRoutePlanStrategyFactory;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

public class PSimReRoutePlanStrategyFactory extends ReRoutePlanStrategyFactory {

	public PSimReRoutePlanStrategyFactory(PSimControler controler) {
		super();
		this.controler = controler;
	}

	private PSimControler controler;

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager ) {
		PlanStrategyImpl strategy = (PlanStrategyImpl) super.createPlanStrategy(scenario,eventsManager);
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}


}
