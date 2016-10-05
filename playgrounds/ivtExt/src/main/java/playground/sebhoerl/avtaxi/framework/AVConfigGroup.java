package playground.sebhoerl.avtaxi.framework;

import org.matsim.core.config.ReflectiveConfigGroup;

public class AVConfigGroup extends ReflectiveConfigGroup {
	private int numberOfVehicles;
	
	public AVConfigGroup() {
		super("av");
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
