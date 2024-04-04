package org.matsim.modechoice.replanning.scheduled;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.replanning.GeneratorContext;

/**
 * Provider for {@link AllBestPlansStrategy}.
 */
public class AllBestPlansStrategyProvider implements Provider<PlanStrategy> {
	@Inject
	private Provider<TripRouter> tripRouterProvider;
	@Inject
	private Config config;

	@Inject
	private Scenario scenario;

	@Inject
	private ActivityFacilities facilities;
	@Inject
	private TimeInterpretation timeInterpretation;
	@Inject
	private Provider<GeneratorContext> generator;

	@Inject
	private Provider<StrategyManager> strategyManager;

	@Override
	public PlanStrategy get() {

		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new KeepSelected<>());

		builder.addStrategyModule(new AllBestPlansStrategy(config, scenario, strategyManager, generator));
		builder.addStrategyModule(new PartialReRoute(facilities, tripRouterProvider, config.global(), timeInterpretation));

		return builder.build();
	}

}
