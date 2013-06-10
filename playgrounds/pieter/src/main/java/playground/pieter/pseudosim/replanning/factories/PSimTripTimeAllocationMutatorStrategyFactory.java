package playground.pieter.pseudosim.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TripTimeAllocationMutatorStrategyFactory;

import playground.pieter.pseudosim.controler.PseudoSimControler;
import playground.pieter.pseudosim.replanning.modules.PSimPlanMarkerModule;

public class PSimTripTimeAllocationMutatorStrategyFactory extends
		TripTimeAllocationMutatorStrategyFactory {

	private PseudoSimControler controler;

	public PSimTripTimeAllocationMutatorStrategyFactory(
			PseudoSimControler controler) {
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
