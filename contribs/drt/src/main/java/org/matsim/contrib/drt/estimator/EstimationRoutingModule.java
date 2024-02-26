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
 * */
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

		Leg mainDrtLeg = (Leg) route.get(2);
		DrtRoute drtRoute = (DrtRoute) mainDrtLeg.getRoute();
		DrtEstimator.Estimate estimate = estimator.estimate(drtRoute, mainDrtLeg.getDepartureTime());

		mainDrtLeg.getAttributes().putAttribute("travel_time", estimate.travelTime());
		mainDrtLeg.getAttributes().putAttribute("travel_distance", estimate.distance());
		mainDrtLeg.getAttributes().putAttribute("wait_time", estimate.waitingTime());

		return route;
	}
}
