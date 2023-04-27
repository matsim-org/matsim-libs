package ch.sbb.matsim.contrib.railsim.prototype.supply;

import java.util.LinkedList;

/**
 * Vehicle allocation information
 * <p>
 * Denotes which vehicles are used by a departure of a transit line.
 *
 * @author Merlin Unterfinger
 */
public interface VehicleAllocationInfo {

	LinkedList<Double> getDepartures(RouteType routeType, RouteDirection routeDirection);

	LinkedList<String> getVehicleIds(RouteType routeType, RouteDirection routeDirection);

}
