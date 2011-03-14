package playground.sergioo.GTFS;

import java.util.HashMap;

public class Route {
	
	//Attributes
	private String shortName;
	private String routeType;
	private HashMap<String, Trip> trips;
	
	//Methods
	/**
	 * @param shortName
	 * @param routeType
	 */
	public Route(String shortName, String routeType) {
		super();
		this.shortName = shortName;
		this.routeType = routeType;
		trips = new HashMap<String, Trip>();
	}
	/**
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}
	/**
	 * @return the routeType
	 */
	public String getRouteType() {
		return routeType;
	}
	/**
	 * @return the trips
	 */
	public HashMap<String, Trip> getTrips() {
		return trips;
	}
	/**
	 * Puts a new trip
	 * @param key
	 * @param trip
	 */
	public void putTrip(String key, Trip trip) {
		trips.put(key, trip);
	}
	
}
