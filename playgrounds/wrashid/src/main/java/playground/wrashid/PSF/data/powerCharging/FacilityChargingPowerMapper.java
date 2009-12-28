package playground.wrashid.PSF.data.powerCharging;

import org.matsim.api.core.v01.Id;

public interface FacilityChargingPowerMapper {

	/**
	 * "At which rate" can energy be charged at the given facility (result in W).
	 *
	 * @param facilityId
	 * @return
	 */
	public double getChargingPower(Id facilityId);
	
}
