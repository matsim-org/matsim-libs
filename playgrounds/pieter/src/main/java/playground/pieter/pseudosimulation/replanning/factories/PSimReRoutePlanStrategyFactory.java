package playground.pieter.pseudosimulation.replanning.factories;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoutePlanStrategyFactory;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import playground.pieter.pseudosimulation.controler.PSimControler;
import playground.pieter.pseudosimulation.replanning.modules.PSimReRoute;

public class PSimReRoutePlanStrategyFactory extends ReRoutePlanStrategyFactory {

    private Scenario scenario;

    public PSimReRoutePlanStrategyFactory(PSimControler controler, Scenario scenario) {
		super(scenario);
        this.scenario = scenario;
    }

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new PSimReRoute(scenario));
//		we don't need to execute the plan, cos it never gets handled
//		strategy.addStrategyModule(new PSimPlanMarkerModule(controler));
		return strategy;
	}


}
