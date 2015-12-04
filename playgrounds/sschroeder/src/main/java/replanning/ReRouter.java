package replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.ScheduledTour;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.router.TimeAndSpaceTourRouter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.GenericPlanStrategyModule;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;

public class ReRouter implements GenericPlanStrategyModule<CarrierPlan> {

	private double rerouteProb = .1;
	
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(ReRouteVehicles.class);
	
	private LeastCostPathCalculator router;
	
	private Network network;
	
	private TravelTime travelTime;
	
	/**
	 * Constructs the module with a leastCostPathRouter, network and travelTime.
	 * 
	 * @param router
	 * @param network
	 * @param travelTime
	 * @param reRouteProb TODO
	 * @see org.matsim.core.router.util.LeastCostPathCalculator , Network, TravelTime
	 */
	public ReRouter(LeastCostPathCalculator router, Network network, TravelTime travelTime, double reRouteProb) {
		super();
		this.router = router;
		this.network = network;
		this.travelTime = travelTime;
		this.rerouteProb = reRouteProb;
	}

	/**
	 * Routes the carrierPlan in time and space.
	 *
	 * @param carrierPlan
	 * @throws IllegalStateException if carrierPlan is null.
	 * @see org.matsim.contrib.freight.router.TimeAndSpaceTourRouter
	 */
	@Override
	public void handlePlan(CarrierPlan carrierPlan) {
		if(carrierPlan == null) throw new IllegalStateException("carrierPlan is null and cannot be handled.");
		route(carrierPlan);
	}
	
	private void route(CarrierPlan carrierPlan) {
		for(ScheduledTour tour : carrierPlan.getScheduledTours()){
			if(MatsimRandom.getRandom().nextDouble() < rerouteProb){
				new TimeAndSpaceTourRouter(router, network, travelTime).route(tour);
			}
		}
	}

	@Override
	public void prepareReplanning(ReplanningContext replanningContext) {
	}

	@Override
	public void finishReplanning() {
	}

}
