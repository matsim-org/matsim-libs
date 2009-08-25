package playground.mmoyo.precalculation;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.transitSchedule.api.TransitRoute;

/**Describes simply a trip in a pt-vechicle*/
public class PTtrip {

	private TransitRoute transitRoute;
	private NetworkRoute subRoute;
	private double travelTime;
	
	public PTtrip(final TransitRoute transitRoute, final NetworkRoute subRoute, final double travelTime) {
		this.transitRoute = transitRoute;
		this.subRoute = subRoute;
		this.travelTime= travelTime;
	}
	
	public TransitRoute getTransitRoute() {
		return transitRoute;
	}
	public NetworkRoute getRoute() {
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


