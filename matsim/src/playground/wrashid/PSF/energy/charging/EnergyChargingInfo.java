package playground.wrashid.PSF.energy.charging;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.data.HubLinkMapping;

public class EnergyChargingInfo {

	/*
	 * time in seconds - get the energy price at the specified facility and time
	 * of day
	 *  // we are taking a modulo of each day (so that plans taking many days
	 * can also be evaluated // perhaps a problem: if the cost of charging is
	 * too big, it might be better to make the plans longer, so // that a person
	 * can charge for lesser money.
	 * 
	 */
	public static double getEnergyPrice(double time, Id linkId) {

		// fit time into 24 hours
		time = Math.round(time) % 86400;

		// testing scenario
		if (ParametersPSF.isTestingModeOn()) {
			if (time < ParametersPSF.getTestingPeakPriceStartTime() || time >= ParametersPSF.getTestingPeakPriceEndTime()) {
				// if low tariff
				return ParametersPSF.getTestingLowTariffElectrictyPrice();
			} else {
				// if peak hour
				return ParametersPSF.getTestingPeakHourElectricityPrice();
			}
		} else {
			// read price from file
			HubLinkMapping hubLinkMapping=ParametersPSF.getHubLinkMapping();
			return ParametersPSF.getHubPriceInfo().getPrice(time, hubLinkMapping.getHubNumber(linkId.toString()));
		}	
	}

}
