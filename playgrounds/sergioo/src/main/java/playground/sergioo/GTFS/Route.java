package playground.sergioo.GTFS;

import java.util.SortedMap;
import java.util.TreeMap;

public class Route {
	
	//Constants
	public enum WayTypes {
		RAIL,
		ROAD,
		WATER,
		CABLE;
	}
	public enum RouteTypes {
		//Values
		TRAM("Tram",WayTypes.RAIL),
		SUBWAY("Subway",WayTypes.RAIL),
		RAIL("Rail",WayTypes.RAIL),
		BUS("Bus",WayTypes.ROAD),
		FERRY("Ferry",WayTypes.WATER),
		CABLE_CAR("Cable car",WayTypes.CABLE);
		//Attributes
		public String name;
		public WayTypes wayType;
		//Methods
		private RouteTypes(String name,WayTypes wayType) {
			this.name = name;
			this.wayType = wayType;
		}
	}
	                           
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
