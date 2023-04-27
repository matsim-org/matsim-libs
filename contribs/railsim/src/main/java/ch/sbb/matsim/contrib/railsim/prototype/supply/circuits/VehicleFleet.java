package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.RouteDirection;
import ch.sbb.matsim.contrib.railsim.prototype.supply.StopInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.TransitLineInfo;
import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleTypeInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Vehicle fleet
 * <p>
 * This class manages the depots and vehicle queues at stop links. Gets or sends vehicles to the queues.
 *
 * @author Merlin Unterfinger
 */
class VehicleFleet {

	private enum TargetQueue {
		/**
		 * Get the queue at the origin of a route departure.
		 */
		ORIGIN,
		/**
		 * Get the queue at the destination of a route departure.
		 */
		DESTINATION
	}

	public record AvailableVehicle(Boolean fromDepot, Vehicle vehicle) {

	}

	private final Map<String, VehicleQueue> stops = new HashMap<>();
	private final Map<StopInfo, VehicleDepot> depots = new HashMap<>();
	private final Map<VehicleTypeInfo, Integer> vehicleCounts = new HashMap<>();

	/**
	 * Default ctor
	 */
	VehicleFleet() {
	}

	/**
	 * Request a vehicle for the next route departure from the origin of the transit line.
	 * <p>
	 * Note: The origin is switched depending on the requested departure.
	 *
	 * @param transitLineInfo the transit line information of the departure.
	 * @param routeDirection  the direction of the route departure.
	 * @param departureTime   the time of the departure.
	 * @return A pair holding the information if a vehicle came from the depot and the vehicle itself.
	 */
	public AvailableVehicle getNextVehicleForDeparture(TransitLineInfo transitLineInfo, RouteDirection routeDirection, double departureTime) {
		final VehicleQueue vehicleQueue = getQueue(transitLineInfo, routeDirection, TargetQueue.ORIGIN);
		// set initial value to a vehicle from the stop queue (STATION)
		boolean fromDepot = false;
		// log the current state of the stop queue for debugging
		vehicleQueue.logState();
		// get existing vehicle from stop queue if there is one waiting
		Vehicle vehicle = vehicleQueue.getVehicle(departureTime);
		// get a vehicle from depot if no vehicle (null) is available in the stop queue and set origin to depot queue (DEPOT)
		if (vehicle == null) {
			VehicleDepot vehicleDepot = getDepot(transitLineInfo, routeDirection, TargetQueue.ORIGIN);
			vehicleDepot.logInventory();
			fromDepot = true;
			vehicle = vehicleDepot.getVehicle(transitLineInfo.getVehicleTypeInfo(), departureTime);
		}
		// return the vehicle and a boolean indicating if the vehicle is from the depot or not
		return new AvailableVehicle(fromDepot, vehicle);
	}

	/**
	 * Send a vehicle to the depot.
	 *
	 * @param transitLineInfo    the transit line information.
	 * @param routeDirection     the direction of the route.
	 * @param vehicle            the vehicle driving on the current route.
	 * @param readyFromDepotTime the time until a vehicle is ready again for a departure from depot: (arrival + waitingTime + 2 * depot travel time + waitingTime).
	 */
	public void sendToDepot(TransitLineInfo transitLineInfo, RouteDirection routeDirection, Vehicle vehicle, double readyFromDepotTime) {
		// set target queue to destination, since we want to send the vehicle to the destination depot
		VehicleDepot vehicleDepot = getDepot(transitLineInfo, routeDirection, TargetQueue.DESTINATION);
		vehicleDepot.addVehicle(vehicle, readyFromDepotTime);
	}

	/**
	 * Let vehicle wait on the current stop link for the next departure.
	 *
	 * @param transitLineInfo the transit line information.
	 * @param routeDirection  the direction of the route.
	 * @param vehicle         the vehicle driving on the current route.
	 * @param readyAgainTime  the time until a vehicle is ready again for a departure from the stop link: arrival + max(waitingTime, turnaroundTime).
	 */
	public void stayOnStopLink(TransitLineInfo transitLineInfo, RouteDirection routeDirection, Vehicle vehicle, double readyAgainTime) {
		// set target queue to destination, since we want to queue the vehicle at the destination
		VehicleQueue vehicleQueue = getQueue(transitLineInfo, routeDirection, TargetQueue.DESTINATION);
		vehicleQueue.addVehicle(vehicle, readyAgainTime);
	}

	/**
	 * Get and increase vehicle type specific count
	 * <p>
	 * This method is used to generate globally unique vehicle ids.
	 *
	 * @param vehicleTypeInfo the vehicle type of the vehicle.
	 * @return the not yet increased count.
	 */
	public int increaseVehicleCount(VehicleTypeInfo vehicleTypeInfo) {
		int vehicleTypeCount = vehicleCounts.getOrDefault(vehicleTypeInfo, 0);
		int tmp = vehicleTypeCount;
		vehicleCounts.put(vehicleTypeInfo, ++vehicleTypeCount);
		return tmp;
	}

	/**
	 * Get the number of vehicles in all stop link queues.
	 *
	 * @return the summed size of all vehicle queues.
	 */
	public int getTotalQueueSize() {
		if (stops.isEmpty()) {
			return 0;
		}
		return stops.values().stream().mapToInt(VehicleQueue::size).sum();
	}

	/**
	 * A counter of all created vehicles
	 *
	 * @return a hashmap containing the counts for all created vehicles.
	 */
	public Map<VehicleTypeInfo, Integer> getTotalVehicleCounts() {
		return vehicleCounts;
	}

	/**
	 * A counter of all vehicles currently located in a depot
	 *
	 * @return a hashmap containing the counts for all vehicles in depots.
	 */
	public HashMap<VehicleTypeInfo, Integer> getTotalVehicleInDepotCounts() {
		HashMap<VehicleTypeInfo, Integer> globalVehicleCounter = new HashMap<>();
		for (VehicleDepot depot : depots.values()) {
			for (Map.Entry<VehicleTypeInfo, Integer> entry : depot.getVehicleCounts().entrySet()) {
				int count = globalVehicleCounter.getOrDefault(entry.getKey(), 0);
				count += entry.getValue();
				globalVehicleCounter.put(entry.getKey(), count);
			}
		}
		return globalVehicleCounter;
	}

	/**
	 * Reset the vehicle fleet
	 */
	public void clear() {
		this.stops.clear();
		this.depots.clear();
		this.vehicleCounts.clear();
	}

	private VehicleDepot getDepot(TransitLineInfo transitLineInfo, RouteDirection routeDirection, TargetQueue targetQueue) {
		final StopInfo stopInfo = getTargetStopInfo(transitLineInfo, routeDirection, targetQueue);
		final VehicleDepot vehicleDepot = depots.getOrDefault(stopInfo, new VehicleDepot(stopInfo.getId(), this));
		depots.put(stopInfo, vehicleDepot);
		return vehicleDepot;
	}

	private VehicleQueue getQueue(TransitLineInfo transitLineInfo, RouteDirection routeDirection, TargetQueue targetQueue) {
		final String key = constructQueueKey(transitLineInfo, routeDirection, targetQueue);
		final VehicleQueue vehicleQueue = stops.getOrDefault(key, new VehicleQueue(key));
		stops.put(key, vehicleQueue);
		return vehicleQueue;
	}

	private static String constructQueueKey(TransitLineInfo transitLineInfo, RouteDirection routeDirection, TargetQueue targetQueue) {
		// key: transitLineName_vehicleTypeName_stopInfoName
		return String.format("%s_%s_%s", transitLineInfo.getId(), transitLineInfo.getVehicleTypeInfo().getId(), getTargetStopInfo(transitLineInfo, routeDirection, targetQueue).getId());
	}

	private static StopInfo getTargetStopInfo(TransitLineInfo transitLineInfo, RouteDirection routeDirection, TargetQueue targetQueue) {
		return switch (targetQueue) {
			case ORIGIN -> transitLineInfo.getOrigin(routeDirection).getStopInfo();
			case DESTINATION -> transitLineInfo.getDestination(routeDirection).getStopInfo();
		};
	}

}
