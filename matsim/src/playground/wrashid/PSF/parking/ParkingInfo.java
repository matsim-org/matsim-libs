package playground.wrashid.PSF.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.ParametersPSF;

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
		
		// TODO: incorporate also possibility of individual charging power plugs at each parking facility
		
		return ParametersPSF.getDefaultChargingPowerAtParking();
	}
	
}
