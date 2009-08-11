package playground.wrashid.PSF.parking;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;

public class ParkingInfo {

	/*
	 * TODO: later read this property from a file for each parking and some
	 * default value in config file.
	 * 
	 */
	public static boolean parkingHasElectricity(Id facilityId) {
		return true;
	}

	/*
	 * TODO: get default from config file and read file for individual locations
	 * (What power can we charge at the facility)
	 */
	public static double getParkingElectricityPower(Id facilityId) {
		
		// for testing only
		String testingChargingPowerAtAllParkings = Gbl.getConfig().findParam("PSF", "testing.chargingPowerAtAllParkings");
		if (testingChargingPowerAtAllParkings!=null){
			return Double.parseDouble(testingChargingPowerAtAllParkings);
		}
		
		return 3500; // 3.5kW is default electric plug power
	}
	
}
