package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TripSubtourModeChoiceStrategyFactory;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

public class PSimTripSubtourModeChoiceStrategyFactory extends
		TripSubtourModeChoiceStrategyFactory {

	private final PSimControler controler;

	public PSimTripSubtourModeChoiceStrategyFactory(PSimControler controler) {
		super();
		this.controler = controler;
	}

	@Override
	public PlanStrategy createPlanStrategy(Scenario scenario,
			EventsManager eventsManager) {
		PlanStrategyImpl strategy = (PlanStrategyImpl) super.createPlanStrategy(scenario, eventsManager) ;
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}
	
}
