package org.matsim.modechoice.replanning;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.InformedModeChoiceConfigGroup;
import org.matsim.modechoice.pruning.CandidatePruner;
import org.matsim.modechoice.search.SingleTripChoicesGenerator;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

import java.util.HashSet;

/**
 * Provider for {@link SelectSingleTripModeStrategy}.
 */
public class SelectSingleTripModeStrategyProvider implements Provider<PlanStrategy> {

	@Inject
	private Provider<TripRouter> tripRouterProvider;
	@Inject
	private GlobalConfigGroup globalConfigGroup;

	@Inject
	private ActivityFacilities facilities;
	@Inject
	private TimeInterpretation timeInterpretation;

	@Inject
	private Provider<GeneratorContext> ctx;

	@Inject
	private InformedModeChoiceConfigGroup config;

	@Inject
	private Provider<PlanSelector> selector;

	@Inject
	private Provider<CandidatePruner> pruner;

	@Override
	public PlanStrategy get() {

		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());

		builder.addStrategyModule(new SelectSingleTripModeStrategy(globalConfigGroup,  new HashSet<>(config.getModes()), ctx, selector, pruner, config.isRequireDifferentModes()));

		builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup, timeInterpretation));

		return builder.build();
	}

}
