package playground.singapore.springcalibration.run.replanning;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

public class SingaporeSubtourModeChoicePlanStrategyProvider implements Provider<PlanStrategy> {
	@Inject private Provider<TripRouter> tripRouterProvider;
	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	@Inject private ActivityFacilities facilities;
	@Inject private Population population;

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new BestPlanSelector());		
		strategy.addStrategyModule(new SingaporeSubtourModeChoice(tripRouterProvider, globalConfigGroup, subtourModeChoiceConfigGroup, population));
		strategy.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup));
		return strategy;
	}
}
