package ch.sbb.matsim.contrib.railsim.prototype.supply;

/**
 * Route type
 * <p>
 * A transit line can have those route types in two directions (F: forward, R: Reverse).
 *
 * @author Merlin Unterfinger
 */
public enum RouteType {
	/**
	 * New vehicle is created of vehicle type and route start is in the depot.
	 * <p>
	 * The departure time is decreased by the travel time from the depot to the station plus the waiting time of the origin station.
	 */
	DEPOT_TO_STATION,
	/**
	 * Existing vehicle at the origin station is used.
	 * <p>
	 * The departure time and the route profile is not changed.
	 */
	STATION_TO_STATION,
	/**
	 * Existing vehicle at the origin station is used, but the destination transit stop facility is the depot of the destination station.
	 * <p>
	 * The departure time is not changed, but the route profile is extended into the depot.
	 */
	STATION_TO_DEPOT,
	/**
	 * New vehicle is created of vehicle type and route start and end is in the depot.
	 * <p>
	 * The departure time is decreased as in DEPOT_TO_STATION, and the route is extended from the depot at the origin to the depot at the destination.
	 */
	DEPOT_TO_DEPOT
}
