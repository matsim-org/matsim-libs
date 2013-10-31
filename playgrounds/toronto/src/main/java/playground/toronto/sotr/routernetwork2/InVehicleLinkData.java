package playground.toronto.sotr.routernetwork2;

import java.util.concurrent.ConcurrentSkipListMap;

import org.matsim.pt.transitSchedule.api.TransitRoute;

/**
 * A (hopefully) thread-safe container of in-vehicle link data (departure times, travel times) used in
 * routing. This way, parallel copies of this link can just point to this one holder for the travel
 * time data to save memory.
 * 
 * @author pkucirek
 *
 */
public final class InVehicleLinkData {
	
	private final TransitRoute route;
	private final ConcurrentSkipListMap<Double, Double> travelTimes;
	private final double defaultTravelTime; //Scheduled travel time. Should never change for the life of this object.
	
	public InVehicleLinkData(final TransitRoute route, final double defaultTravelTime){
		this.route = route;
		
		this.travelTimes = new ConcurrentSkipListMap<Double, Double>();
		this.defaultTravelTime = defaultTravelTime;
	}
	
	//Package internal
	TransitRoute getRoute() { return this.route; }
	ConcurrentSkipListMap<Double, Double> getTravelTimes() {return this.travelTimes; }
	double getDefaultTravelTime() { return this.defaultTravelTime; }
	
	/**
	 * Adds just a departure from this link's tail node.
	 * @param departureTime The absolute time of the departure.
	 */
	public void addDeparture(double departureTime){
		this.travelTimes.put(departureTime, Double.NaN);
	}
	
	/**
	 * Adds a
	 * @param departureTime
	 * @param arrivalTime
	 */
	public void addDeparture(double departureTime, double arrivalTime){
		//this.departures.add(departureTime);
		double delta = arrivalTime - departureTime;
		this.travelTimes.put(departureTime, delta);
	}
}
 