package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeSingleLegModeStrategyFactory;
import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimPlanMarkerModule;

public class PSimChangeSingleLegModeStrategyFactory extends
		ChangeSingleLegModeStrategyFactory {

	public PSimChangeSingleLegModeStrategyFactory(PSimControler controler) {
        super(controler.getMATSimControler().getScenario());
        this.controler = controler;
	}

	private final PSimControler controler;

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = (PlanStrategyImpl) super.get();
		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}
}
