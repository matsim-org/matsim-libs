package org.matsim.modechoice.replanning.scheduled;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.modechoice.replanning.GeneratorContext;

/**
 * Same as re-route but keeps the selected plan.
 */
public class ReRouteSelectedStrategyProvider implements Provider<PlanStrategy> {
	@Inject
	private Provider<TripRouter> tripRouterProvider;
	@Inject
	private Config config;
	@Inject
	private ActivityFacilities facilities;
	@Inject
	private TimeInterpretation timeInterpretation;

	@Override
	public PlanStrategy get() {


		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new KeepSelected<>());

		builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, config.global(), timeInterpretation));

		return builder.build();
	}

}
