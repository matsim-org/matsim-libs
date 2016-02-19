package playground.wrashid.parkingChoice;

import org.matsim.core.config.ReflectiveConfigGroup;

public class ParkingConfigModule extends ReflectiveConfigGroup {

	public ParkingConfigModule() {
		super("parkingChoice", true);
	}

	public static double getStartParkingSearchDistanceInMeters() {
		return 200;
	}

	
	
}
