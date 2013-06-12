package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;
import playground.pieter.pseudosimulation.replanning.strategies.PSimLocationChoicePlanStrategy;

public class PSimLocationChoicePlanStrategyFactory implements PlanStrategyFactory {

	public PSimLocationChoicePlanStrategyFactory(PSimControler controler) {
		super();
		this.controler = controler;
	}

	private PSimControler controler;

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario, EventsManager eventsManager ) {
		return new PSimLocationChoicePlanStrategy(scenario, controler);
	}


}
