package ch.sbb.matsim.contrib.railsim.prototype.supply.circuits;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.misc.Time;

import java.util.LinkedList;

/**
 * A vehicle queue (FIFO)
 * <p>
 * Stores the time when vehicles, which are waiting on a stop link, a ready for the next departure.
 *
 * @author Merlin Unterfinger
 */
class VehicleQueue {

	private static final Logger log = LogManager.getLogger(VehicleQueue.class);
	private final LinkedList<VehicleReadyEvent> vehicleReadyEvents = new LinkedList<>();
	private final String name;

	/**
	 * Vehicle FIFO queue
	 *
	 * @param name the name of the vehicle queue.
	 */
	public VehicleQueue(String name) {
		this.name = name;
	}

	/**
	 * Add a vehicle to the end of the queue.
	 *
	 * @param vehicle        the vehicle to add to the queue.
	 * @param readyAgainTime the time when a departure is possible again with this vehicle.
	 */
	public void addVehicle(Vehicle vehicle, double readyAgainTime) {
		log.info(String.format("Staying on stop link, appending vehicle '%s' to queue '%s'", vehicle.id(), name));
		vehicleReadyEvents.addLast(new VehicleReadyEvent(vehicle, readyAgainTime));
	}

	/**
	 * Get a vehicle from the start of the queue, if the departure time is after the ready time.
	 *
	 * @param departureTime the departure time.
	 * @return the vehicle or null if no vehicle is available for departure.
	 */
	public Vehicle getVehicle(double departureTime) {
		// check if vehicles are waiting in the queue
		if (vehicleReadyEvents.isEmpty()) {
			return null;
		}
		VehicleReadyEvent vehicleReadyEvent = vehicleReadyEvents.getFirst();
		// check if departure is before ready time of vehicle
		if (departureTime < vehicleReadyEvent.time()) {
			log.warn(String.format("Departure time (%s) is before ready time (%s) of vehicle (%s) at stop queue '%s'! Skipping...", Time.writeTime(departureTime, Time.TIMEFORMAT_HHMMSS), Time.writeTime(vehicleReadyEvent.time(), Time.TIMEFORMAT_HHMMSS), vehicleReadyEvent.vehicle().id(), this.name));
			return null;
		}
		// there is a vehicle, where the departure is inside the time window of availability, remove it from the stop queue and return
		log.info(String.format("Leaving stop link, remove vehicle '%s' from queue '%s'", vehicleReadyEvent.vehicle().id(), name));
		return vehicleReadyEvents.removeFirst().vehicle();
	}

	/**
	 * Get the size of the queue.
	 *
	 * @return the size.
	 */
	public int size() {
		return vehicleReadyEvents.size();
	}

	/**
	 * Create a log message with the state of the queue
	 */
	public void logState() {
		StringBuilder sb = new StringBuilder("Queue state of ");
		sb.append(name);
		sb.append(": ");
		if (vehicleReadyEvents.isEmpty()) {
			sb.append("Empty");
		} else {
			for (VehicleReadyEvent vehicleReadyEvent : vehicleReadyEvents) {
				sb.append(vehicleReadyEvent.vehicle().id());
				sb.append(" (");
				sb.append(Time.writeTime(vehicleReadyEvent.time(), Time.TIMEFORMAT_HHMMSS));
				sb.append(") ");
			}
			sb.delete(sb.length() - 1, sb.length());
		}
		log.info(sb);
	}
}
