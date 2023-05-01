package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteType;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleAllocationInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Time;

import java.util.EnumMap;
import java.util.LinkedList;

/**
 * Transit line vehicle allocation
 * <p>
 * Container class to store the route departures with allocated vehicles per line.
 *
 * @author Merlin Unterfinger
 */
class TransitLineVehicleAllocation implements VehicleAllocationInfo {

	private static final Logger log = LogManager.getLogger(TransitLineVehicleAllocation.class);

	private final EnumMap<RouteDirection, EnumMap<RouteType, LinkedList<Double>>> directedDepartures = new EnumMap<>(RouteDirection.class);
	private final EnumMap<RouteDirection, EnumMap<RouteType, LinkedList<String>>> directedVehicleIds = new EnumMap<>(RouteDirection.class);

	/**
	 * Ctor
	 */
	public TransitLineVehicleAllocation() {
		directedDepartures.put(RouteDirection.FORWARD, new EnumMap<>(RouteType.class));
		directedDepartures.put(RouteDirection.REVERSE, new EnumMap<>(RouteType.class));
		directedVehicleIds.put(RouteDirection.FORWARD, new EnumMap<>(RouteType.class));
		directedVehicleIds.put(RouteDirection.REVERSE, new EnumMap<>(RouteType.class));
	}

	/**
	 * Adds a vehicle allocation for a route departure to the transit line.
	 *
	 * @param routeDepartureEvent the departure event.
	 * @param routeType           the route type of the route.
	 * @param vehicle             the vehicle to allocate.
	 */
	public void addVehicleAllocation(RouteDepartureEvent routeDepartureEvent, RouteType routeType, Vehicle vehicle) {
		final RouteDirection routeDirection = routeDepartureEvent.getRouteDirection();
		final double departureTime = routeDepartureEvent.getDepartureTime();
		final String vehicleId = vehicle.id();
		log.info(String.format("Allocating vehicle %s to departure %s of line %s (%s)", vehicleId, Time.writeTime(departureTime, Time.TIMEFORMAT_HHMMSS), routeDepartureEvent.getTransitLineInfo().getId(), routeType.name()));
		// add departure time
		EnumMap<RouteType, LinkedList<Double>> departureMap = directedDepartures.get(routeDirection);
		LinkedList<Double> departuresList = departureMap.getOrDefault(routeType, new LinkedList<>());
		departuresList.add(departureTime);
		departureMap.put(routeType, departuresList);
		// add vehicle id
		EnumMap<RouteType, LinkedList<String>> vehicleIdMap = directedVehicleIds.get(routeDirection);
		LinkedList<String> vehicleIds = vehicleIdMap.getOrDefault(routeType, new LinkedList<>());
		vehicleIds.add(vehicleId);
		vehicleIdMap.put(routeType, vehicleIds);
	}

	/**
	 * Retrieve the departure times for a route type and direction.
	 *
	 * @param routeType      the type of the route.
	 * @param routeDirection the route direction.
	 * @return a list containing the departure times.
	 */
	@Override
	public LinkedList<Double> getDepartures(RouteType routeType, RouteDirection routeDirection) {
		return directedDepartures.get(routeDirection).get(routeType);
	}

	/**
	 * Retrieve the vehicle ids for a route type and direction.
	 *
	 * @param routeType      the type of the route.
	 * @param routeDirection the route direction.
	 * @return a list containing the vehicle ids.
	 */
	@Override
	public LinkedList<String> getVehicleIds(RouteType routeType, RouteDirection routeDirection) {
		return directedVehicleIds.get(routeDirection).get(routeType);
	}
}
