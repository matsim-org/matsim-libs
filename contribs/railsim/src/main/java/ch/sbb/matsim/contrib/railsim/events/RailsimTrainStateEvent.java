package ch.sbb.matsim.contrib.railsim.events;

import ch.sbb.matsim.contrib.railsim.RailsimUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.HasVehicleId;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Event for currents train position.
 */
public class RailsimTrainStateEvent extends Event implements HasVehicleId {

	public static final String EVENT_TYPE = "railsimTrainStateEvent";

	public static final String ATTRIBUTE_EXACT_TIME = "exactTime";
	public static final String ATTRIBUTE_HEADLINK = "headLink";
	public static final String ATTRIBUTE_HEADPOSITION = "headPosition";
	public static final String ATTRIBUTE_TAILLINK = "tailLink";
	public static final String ATTRIBUTE_TAILPOSITION = "tailPosition";
	public static final String ATTRIBUTE_SPEED = "speed";
	public static final String ATTRIBUTE_ACCELERATION = "acceleration";
	public static final String ATTRIBUTE_TARGETSPEED = "targetSpeed";

	/**
	 * Exact time with resolution of 0.001s.
	 */
	private final double exactTime;
	private final Id<Vehicle> vehicleId;
	private final Id<Link> headLink;
	private final double headPosition;
	private final Id<Link> tailLink;
	private final double tailPosition;
	private final double speed;
	private final double acceleration;
	private final double targetSpeed;

	public RailsimTrainStateEvent(double time, double exactTime, Id<Vehicle> vehicleId,
								  Id<Link> headLink, double headPosition,
								  Id<Link> tailLink, double tailPosition,
								  double speed, double acceleration, double targetSpeed) {
		super(time);
		this.exactTime = RailsimUtils.round(exactTime);
		this.vehicleId = vehicleId;
		this.headLink = headLink;
		this.headPosition = headPosition;
		this.tailLink = tailLink;
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

	public double getExactTime() {
		return exactTime;
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

	public double getTargetSpeed() {
		return this.targetSpeed;
	}

	public double getAcceleration() {
		return this.acceleration;
	}

	public Id<Link> getHeadLink() {
		return this.headLink;
	}

	public Id<Link> getTailLink() {
		return this.tailLink;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_EXACT_TIME, String.valueOf(exactTime));
		attr.put(ATTRIBUTE_VEHICLE, this.vehicleId.toString());
		attr.put(ATTRIBUTE_HEADLINK, String.valueOf(headLink));
		attr.put(ATTRIBUTE_HEADPOSITION, Double.toString(headPosition));
		attr.put(ATTRIBUTE_TAILLINK, String.valueOf(tailLink));
		attr.put(ATTRIBUTE_TAILPOSITION, Double.toString(tailPosition));
		attr.put(ATTRIBUTE_SPEED, Double.toString(speed));
		attr.put(ATTRIBUTE_ACCELERATION, Double.toString(acceleration));
		attr.put(ATTRIBUTE_TARGETSPEED, Double.toString(targetSpeed));
		return attr;
	}
}
