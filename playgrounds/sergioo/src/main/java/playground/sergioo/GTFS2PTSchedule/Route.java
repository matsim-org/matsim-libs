package playground.sergioo.GTFS2PTSchedule;

import java.util.SortedMap;
import java.util.TreeMap;

import playground.sergioo.GTFS2PTSchedule.GTFSDefinitions.RouteTypes;

public class Route {
	                           
	//Attributes
	private String shortName;
	private RouteTypes routeType;
	private SortedMap<String, Trip> trips;
	
	//Methods
	/**
	 * @param shortName
	 * @param routeType
	 */
	public Route(String shortName, RouteTypes routeType) {
		super();
		this.shortName = shortName;
		this.routeType = routeType;
		trips = new TreeMap<String, Trip>();
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
	public RouteTypes getRouteType() {
		return routeType;
	}
	/**
	 * @return the trips
	 */
	public SortedMap<String, Trip> getTrips() {
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
