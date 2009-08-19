package playground.mmoyo.precalculation;

import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.api.basic.v01.Id;

/**Describes simply a trip in a pt-vechicle*/
public class PTtrip {

	private Id transitRouteId;
	private NetworkRoute route;
	private double travelTime;
	
	public PTtrip(final Id transitRouteId, final NetworkRoute route, final double travelTime) {
		this.transitRouteId = transitRouteId;
		this.route = route;
		this.travelTime= travelTime;
	}
	
	public Id getTransitRouteId() {
		return transitRouteId;
	}
	public NetworkRoute getRoute() {
		return route;
	}
	
	public double getTravelTime(){
		return travelTime;
	}
	
}


