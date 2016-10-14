package playground.sebhoerl.avtaxi.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AVConfigGroup extends ReflectiveConfigGroup {
	final static String NUMBER_OF_VEHICLES = "numberOfVehicles";
	final static String PICKUP_DURATION = "pickupDuration";
	final static String DROPOFF_DURATION = "dropoffDuration";

	private int numberOfVehicles = 10;
	private double pickupDuration = 120.0;
	private double dropoffDuration = 60.0;
	
	public AVConfigGroup() {
		super("av");
	}
	
	@StringSetter(NUMBER_OF_VEHICLES)
	public void setNumberOfVehicles(int numberOfVehicles) {
		this.numberOfVehicles = numberOfVehicles;
	}
	
	@StringGetter(NUMBER_OF_VEHICLES)
	public int getNumberOfVehicles() {
		return numberOfVehicles;
	}

	@StringSetter(PICKUP_DURATION)
	public void setPickupDuration(double pickupDuration) {
		this.pickupDuration = pickupDuration;
	}

	@StringGetter(PICKUP_DURATION)
	public double getPickupDuration() {
		return pickupDuration;
	}

	@StringSetter(DROPOFF_DURATION)
	public void setDropoffDuration(double dropoffDuration) {
		this.dropoffDuration = dropoffDuration;
	}

	@StringGetter(DROPOFF_DURATION)
	public double getDropoffDuration() {
		return pickupDuration;
	}
}
