package org.matsim.modechoice.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.search.TopKChoicesGenerator;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Provider for {@link SelectFromGeneratorStrategy}.
 */
public class BestKPlanSelectionStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	private Provider<TripRouter> tripRouterProvider;
	@Inject
	private GlobalConfigGroup globalConfigGroup;

	@Inject
	private InformedModeChoiceConfigGroup configGroup;

	@Inject
	private ActivityFacilities facilities;
	@Inject
	private TimeInterpretation timeInterpretation;
	@Inject
	private Provider<TopKChoicesGenerator> generator;

	@Override
	public PlanStrategy get() {

		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());

		builder.addStrategyModule(new SimplePlanSelectionStrategyModule(globalConfigGroup, generator,
				new MultinomialLogitSelector(configGroup.getInvBeta(), MatsimRandom.getLocalInstance())));

		builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup, timeInterpretation));

		return builder.build();
	}
}