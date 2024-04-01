package org.matsim.modechoice.replanning.scheduled;

import com.google.inject.Provider;
import jakarta.inject.Inject;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.replanning.strategies.TimeAllocationMutatorModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

/**
 * Same as re-route but keeps the selected plan.
 */
public class TimeMutateSelectedStrategyProvider implements Provider<PlanStrategy> {
	@Inject private jakarta.inject.Provider<TripRouter> tripRouterProvider;
	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup;
	@Inject private ActivityFacilities activityFacilities;
	@Inject private TimeInterpretation timeInterpretation;

	@Override
	public PlanStrategy get() {

		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new KeepSelected<>());

		builder.addStrategyModule(new TimeAllocationMutatorModule(this.timeAllocationMutatorConfigGroup, this.globalConfigGroup) );
		builder.addStrategyModule(new ReRoute(this.activityFacilities, this.tripRouterProvider, this.globalConfigGroup, this.timeInterpretation));

		return builder.build();
	}

}
