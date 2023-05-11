package ch.sbb.matsim.contrib.railsim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.vehicles.Vehicle;

import java.util.Map;

/**
 * Event for currents train position.
 */
public class RailsimTrainStateEvent extends Event implements HasVehicleId {

	public static final String EVENT_TYPE = "railsimTrainStateEvent";

	private final Id<Vehicle> vehicleId;
	private final double headPosition;
	private final double tailPosition;
	private final double speed;
	private final double acceleration;
	private final double targetSpeed;

	public RailsimTrainStateEvent(double time, Id<Vehicle> vehicleId, double headPosition, double tailPosition,
								  double speed, double acceleration, double targetSpeed) {
		super(time);
		this.vehicleId = vehicleId;
		this.headPosition = headPosition;
		this.tailPosition = tailPosition;
		this.speed = speed;
		this.acceleration = acceleration;
		this.targetSpeed = targetSpeed;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return vehicleId;
	}

	public double getHeadPosition() {
		return headPosition;
	}

	public double getTailPosition() {
		return tailPosition;
	}

	public double getSpeed() {
		return speed;
	}

	public double getAcceleration() {
		return acceleration;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put("headPosition", Double.toString(headPosition));
		attr.put("tailPosition", Double.toString(tailPosition));
		attr.put("speed", Double.toString(speed));
		attr.put("acceleration", Double.toString(acceleration));
		attr.put("targetSpeed", Double.toString(targetSpeed));
		return attr;
	}
}
