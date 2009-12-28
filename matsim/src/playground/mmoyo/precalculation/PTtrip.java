package playground.mmoyo.precalculation;

import org.matsim.api.core.v01.Id;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.transitSchedule.api.TransitRoute;

/**Describes simply a trip in a pt-vechicle*/
public class PTtrip {
	//-> use leg with infinite time values instead?
	private TransitRoute transitRoute;
	private NetworkRouteWRefs subRoute;
	private double travelTime;
	
	public PTtrip(final TransitRoute transitRoute, final NetworkRouteWRefs subRoute, final double travelTime) {
		this.transitRoute = transitRoute;
		this.subRoute = subRoute;
		this.travelTime= travelTime;
	}
	
	public TransitRoute getTransitRoute() {
		return transitRoute;
	}
	public NetworkRouteWRefs getRoute() {
		return subRoute;
	}
	
	public double getTravelTime(){
		return travelTime;
	}
	
	/**Returns the first Facility Id */
	public Id getBoardFacilityId (){
		return subRoute.getNodes().get(0).getId();
	} 
	

}


