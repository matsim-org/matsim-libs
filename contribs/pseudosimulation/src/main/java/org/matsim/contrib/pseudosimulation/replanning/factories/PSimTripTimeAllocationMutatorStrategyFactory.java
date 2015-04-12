package org.matsim.contrib.pseudosimulation.replanning.factories;

import org.matsim.contrib.pseudosimulation.replanning.modules.PSimPlanMarkerModule;
import org.matsim.contrib.pseudosimulation.controler.PSimControler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TimeAllocationMutatorPlanStrategyFactory;

class PSimTripTimeAllocationMutatorStrategyFactory extends
		TimeAllocationMutatorPlanStrategyFactory {

	private final PSimControler controler;

	public PSimTripTimeAllocationMutatorStrategyFactory(
			PSimControler controler) {
		super(controler.getScenario());
		this.controler = controler;
	}

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = (PlanStrategyImpl) super.get() ;
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}
	
	

}
