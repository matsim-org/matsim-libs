package org.matsim.contrib.drt.estimator;

import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;

import java.util.List;

/**
 * Delegates routing to the original dvrp router and adds estimations to the leg.
 * */
public class EstimationRoutingModule implements RoutingModule {
	private final DvrpRoutingModule delegate;

	public EstimationRoutingModule(DvrpRoutingModule delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {

		List<? extends PlanElement> route = delegate.calcRoute(request);

		return route;
	}
}
