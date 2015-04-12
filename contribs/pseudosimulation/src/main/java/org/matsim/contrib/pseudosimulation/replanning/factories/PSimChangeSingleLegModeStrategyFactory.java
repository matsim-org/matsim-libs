package org.matsim.contrib.pseudosimulation.replanning.factories;

import org.matsim.contrib.pseudosimulation.controler.PSimControler;
import org.matsim.contrib.pseudosimulation.replanning.modules.PSimPlanMarkerModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeSingleLegModeStrategyFactory;

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
