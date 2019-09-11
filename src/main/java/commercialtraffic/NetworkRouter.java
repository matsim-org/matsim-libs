package commercialtraffic;

import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.router.TimeAndSpacePlanRouter;

/**
 * Router that routes {@link CarrierPlan}.
 * 
 * @author stefan schröder
 *
 */
public class NetworkRouter {
	
	/**
	 * Routes the {@link CarrierPlan} with the router defined in {@link NetworkBasedTransportCosts}.
	 * 
	 * <p>Note that this changes the plan, i.e. it adds routes to the input-plan.
	 * 
	 * @param {@link CarrierPlan}
	 * @param {@link NetworkBasedTransportCosts}
	 */
	public static void routePlan(CarrierPlan plan, NetworkBasedTransportCosts netbasedTransportCosts){
		new TimeAndSpacePlanRouter(netbasedTransportCosts.getRouter(), netbasedTransportCosts.getNetwork(), netbasedTransportCosts.getTravelTime()).run(plan);
	}

}