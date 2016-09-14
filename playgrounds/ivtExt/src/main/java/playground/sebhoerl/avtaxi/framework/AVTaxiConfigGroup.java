package playground.sebhoerl.avtaxi.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AVTaxiConfigGroup extends ReflectiveConfigGroup {
	private int numberOfVehicles;
	
	public AVTaxiConfigGroup() {
		super("avtaxi");
	}
	
	@StringSetter("numberOfVehicles")
	public void setNumberOfVehicles(int numberOfVehicles) {
		this.numberOfVehicles = numberOfVehicles;
	}
	
	@StringGetter("numberOfVehicles")
	public int getNumberOfVehicles() {
		return numberOfVehicles;
	}
}
