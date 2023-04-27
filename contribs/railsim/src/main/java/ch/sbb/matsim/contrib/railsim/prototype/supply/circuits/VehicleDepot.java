package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import ch.sbb.matsim.contrib.railsim.prototype.supply.VehicleTypeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Time;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A vehicle depot
 * <p>
 * Contains a queue for each vehicle type that enters the depot. Can create new vehicles if empty.
 *
 * @author Merlin Unterfinger
 */
class VehicleDepot {

	private static final Logger log = LogManager.getLogger(VehicleDepot.class);
	private final HashMap<VehicleTypeInfo, LinkedList<VehicleReadyEvent>> vehicles = new HashMap<>();
	private final VehicleFleet fleet;
	private final String name;

	/**
	 * Ctor
	 *
	 * @param name  the name of the depot.
	 * @param fleet the fleet, the depot corresponds to.
	 */
	VehicleDepot(String name, VehicleFleet fleet) {
		this.name = name;
		this.fleet = fleet;
	}

	/**
	 * Gets or creates a new vehicle
	 * <p>
	 * If there is a vehicle of the corresponding type is ready in the queue, it is taken. Otherwise, a new vehicle of this type is generated and returned.
	 *
	 * @param vehicleTypeInfo the vehicle type information, used to determine the requested vehicle type.
	 * @return a new or existing vehicle.
	 */
	public Vehicle getVehicle(VehicleTypeInfo vehicleTypeInfo, double departureTime) {
		// get vehicle queue of corresponding type, add back to hashmap (only relevant the first time)
		LinkedList<VehicleReadyEvent> vehiclesOfType = vehicles.getOrDefault(vehicleTypeInfo, new LinkedList<>());
		vehicles.put(vehicleTypeInfo, vehiclesOfType);
		// check if there are vehicles in the depot, if there are any, check if the first one is ready again before departure
		if (!vehiclesOfType.isEmpty() && departureTime >= vehiclesOfType.getFirst().time()) {
			VehicleReadyEvent vehicleReadyEvent = vehiclesOfType.removeFirst();
			Vehicle vehicle = vehicleReadyEvent.vehicle();
			log.info(String.format("Leaving depot link, remove available vehicle '%s' (departure: %s, ready: %s) from depot queue '%s'", vehicleReadyEvent.vehicle().id(), Time.writeTime(departureTime, Time.TIMEFORMAT_HHMMSS), Time.writeTime(vehicleReadyEvent.time(), Time.TIMEFORMAT_HHMMSS), name));
			return vehicle;
		}
		// No vehicle was ready in the depot queue, create a new one
		final String vehicleId = String.format("%s_%d", vehicleTypeInfo.getId(), fleet.increaseVehicleCount(vehicleTypeInfo));
		log.info(String.format("No vehicle ready (departure: %s) in depot, creating new vehicle '%s' one", Time.writeTime(departureTime, Time.TIMEFORMAT_HHMMSS), vehicleId));
		return new Vehicle(vehicleId, vehicleTypeInfo);
	}

	/**
	 * Add a vehicle to the depot.
	 *
	 * @param vehicle            the vehicle to add to the depot.
	 * @param readyFromDepotTime the time until a vehicle is ready again for a departure from depot: (arrival + waitingTime + 2 * depot travel time + waitingTime)
	 */
	public void addVehicle(Vehicle vehicle, double readyFromDepotTime) {
		log.info(String.format("Entering depot link, add vehicle '%s' to depot queue '%s'", vehicle.id(), name));
		LinkedList<VehicleReadyEvent> vehiclesOfType = vehicles.getOrDefault(vehicle.type(), new LinkedList<>());
		vehiclesOfType.addLast(new VehicleReadyEvent(vehicle, readyFromDepotTime));
		vehicles.put(vehicle.type(), vehiclesOfType);
	}

	/**
	 * Get the vehicle type specific count of all vehicles in the depot
	 *
	 * @return a hashmap containing the counts per vehicle type.
	 */
	public HashMap<VehicleTypeInfo, Integer> getVehicleCounts() {
		HashMap<VehicleTypeInfo, Integer> vehicleCounter = new HashMap<>();
		for (Map.Entry<VehicleTypeInfo, LinkedList<VehicleReadyEvent>> entry : vehicles.entrySet()) {
			vehicleCounter.put(entry.getKey(), entry.getValue().size());
		}
		return vehicleCounter;
	}

	/**
	 * Create a log message with the inventory of the depot.
	 * <p>
	 * Useful for debugging.
	 */
	public void logInventory() {
		StringBuilder sb = new StringBuilder("Depot inventory of ");
		sb.append(name);
		sb.append(":\n");
		for (Map.Entry<VehicleTypeInfo, LinkedList<VehicleReadyEvent>> entry : vehicles.entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			sb.append(" - ");
			sb.append(entry.getKey().getId());
			sb.append(": ");
			for (VehicleReadyEvent vehicleReadyEvent : entry.getValue()) {
				sb.append(vehicleReadyEvent.vehicle().id());
				sb.append(" (");
				sb.append(Time.writeTime(vehicleReadyEvent.time(), Time.TIMEFORMAT_HHMMSS));
				sb.append("), ");
			}
			sb.delete(sb.length() - 2, sb.length());
			sb.append("\n");
		}
		sb.delete(sb.length() - 1, sb.length());
		log.info(sb);
	}
}
