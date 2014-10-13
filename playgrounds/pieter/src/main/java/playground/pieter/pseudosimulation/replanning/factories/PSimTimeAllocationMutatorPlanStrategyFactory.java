package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TimeAllocationMutatorPlanStrategyFactory;

import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

public class PSimTimeAllocationMutatorPlanStrategyFactory extends
TimeAllocationMutatorPlanStrategyFactory {

	private final PSimControler controler;

	public PSimTimeAllocationMutatorPlanStrategyFactory(
			PSimControler controler) {
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
