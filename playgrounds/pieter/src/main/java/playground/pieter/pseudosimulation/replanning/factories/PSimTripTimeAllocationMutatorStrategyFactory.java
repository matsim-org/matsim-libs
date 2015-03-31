package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.TimeAllocationMutatorPlanStrategyFactory;
import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

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
