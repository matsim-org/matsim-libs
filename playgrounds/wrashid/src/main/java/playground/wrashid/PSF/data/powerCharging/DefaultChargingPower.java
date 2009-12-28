package playground.wrashid.PSF.data.powerCharging;

import org.matsim.api.core.v01.Id;

/**
 * This charger gives back always the default charging power (not considering
 * the facilityId)
 * 
 * @author rashid_waraich
 * 
 */
public class DefaultChargingPower implements FacilityChargingPowerMapper {

	private double chargingPower;

	public DefaultChargingPower(double chargingPower) {
		this.chargingPower = chargingPower;
	}

	public double getChargingPower(Id facilityId) {
		return chargingPower;
	}
}
