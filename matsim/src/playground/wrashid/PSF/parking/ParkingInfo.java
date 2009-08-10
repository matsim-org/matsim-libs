package playground.wrashid.PSF.parking;

public class ParkingInfo {

	/*
	 * TODO: later read this property from a file for each parking and some
	 * default value in config file.
	 * 
	 */
	public static boolean parkingHasElectricity(String facilityId) {
		return true;
	}

	/*
	 * TODO: get default from config file and read file for individual locations
	 */
	public static double getParkingElectricityPower(String facilityId) {
		return 3500; // 3.5kW is default electric plug power
	}
	
}
