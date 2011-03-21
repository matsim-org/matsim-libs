package playground.sergioo.GTFS;

import java.util.SortedMap;
import java.util.TreeMap;

public class Route {
	
	//Constants
	public static final String[] ROUTE_TYPES = {"Tram","Subway","Rail","Bus","Ferry","Cable car"};
	                           
	//Attributes
	private String shortName;
	private int routeType;
	private SortedMap<String, Trip> trips;
	
	//Methods
	/**
	 * @param shortName
	 * @param routeType
	 */
	public Route(String shortName, int routeType) {
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
	public int getRouteType() {
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
