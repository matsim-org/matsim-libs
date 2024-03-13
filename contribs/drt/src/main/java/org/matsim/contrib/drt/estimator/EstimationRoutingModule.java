package org.matsim.contrib.drt.estimator;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

import java.util.List;

/**
 * Delegates routing to the original dvrp router and adds estimations to the leg.
 */
public class EstimationRoutingModule implements RoutingModule {
	private final DvrpRoutingModule delegate;
	private final DrtEstimator estimator;

	public EstimationRoutingModule(DvrpRoutingModule delegate, DrtEstimator estimator) {
		this.delegate = delegate;
		this.estimator = estimator;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {

		List<? extends PlanElement> route = delegate.calcRoute(request);

		if (route == null) {
			// no suitable DRT connection found (e.g., can't find DRT stops nearby), will fall back to walk mode.
			return null;
		}

		for (PlanElement el : route) {
			if (el instanceof Leg leg) {
				if (leg.getRoute() instanceof DrtRoute drtRoute) {
					DrtEstimator.Estimate estimate = estimator.estimate(drtRoute, leg.getDepartureTime());
					DrtEstimator.setEstimateAttributes(leg, estimate);
				}
			}
		}
		return route;
	}
}
