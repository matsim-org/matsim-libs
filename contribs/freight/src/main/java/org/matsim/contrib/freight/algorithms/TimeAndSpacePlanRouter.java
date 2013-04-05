package org.matsim.contrib.freight.algorithms;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

/**
 * Router routing carrierPlans in time and space.
 * 
 * @author sschroeder
 *
 */
public class TimeAndSpacePlanRouter {

	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;
	
	public TimeAndSpacePlanRouter(LeastCostPathCalculator router, Network network, TravelTime travelTime) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
	}

	/**
	 * Routes all scheduled tours within the plan in time and space.
	 * 
	 * @param plan
	 * @see TimeAndSpaceTourRouter
	 */
	public void run(CarrierPlan plan) {
		for(ScheduledTour tour : plan.getScheduledTours()){
			new TimeAndSpaceTourRouter(router, network, travelTime).route(tour);
		}
		
	}

}
