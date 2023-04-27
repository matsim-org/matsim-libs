package ch.sbb.matsim.contrib.railsim.prototype.supply;

import java.util.HashMap;
import java.util.Map;

/**
 * Vehicle type information
 * <p>
 * Use this class in combination with TransitLineInfo to provide a vehicle type for a transit line.
 *
 * @author Merlin Unterfinger
 */
public class VehicleTypeInfo {

	private final String id;
	private final int capacity;
	private final double length;
	private final double maxVelocity;
	private final double turnaroundTime;

	private final Map<String, Object> attributes = new HashMap<>();

	/**
	 * @param id             the name of the vehicle type.
	 * @param capacity       the passenger capacity of the vehicle.
	 * @param length         the vehicle length.
	 * @param maxVelocity    the maximum velocity of the vehicle.
	 * @param turnaroundTime the time needed to change direction of travel in a station (change drivers cab).
	 */
	public VehicleTypeInfo(String id, int capacity, double length, double maxVelocity, double turnaroundTime) {
		this.id = id;
		this.capacity = capacity;
		this.length = length;
		this.maxVelocity = maxVelocity;
		this.turnaroundTime = turnaroundTime;
	}

	public String getId() {
		return id;
	}

	public int getCapacity() {
		return capacity;
	}

	public double getLength() {
		return length;
	}

	public double getMaxVelocity() {
		return maxVelocity;
	}

	public double getTurnaroundTime() {
		return turnaroundTime;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}
}
