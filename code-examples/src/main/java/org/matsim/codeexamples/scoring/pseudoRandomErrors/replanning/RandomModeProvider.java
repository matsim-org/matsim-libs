package org.matsim.codeexamples.scoring.pseudoRandomErrors.replanning;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

public class RandomModeProvider implements Provider<PlanStrategy> {
	private Provider<TripRouter> tripRouterProvider;
	private GlobalConfigGroup globalConfigGroup;
	private ActivityFacilities facilities;
	private TimeInterpretation timeInterpretation;

	@Inject
	public RandomModeProvider(Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup,
			ActivityFacilities facilities, TimeInterpretation timeInterpretation) {
		this.tripRouterProvider = tripRouterProvider;
		this.facilities = facilities;
		this.globalConfigGroup = globalConfigGroup;
		this.timeInterpretation = timeInterpretation;
	}

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());

		builder.addStrategyModule(new RandomModeStrategyModule(globalConfigGroup));
		builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup, timeInterpretation));

		return builder.build();
	}
}
