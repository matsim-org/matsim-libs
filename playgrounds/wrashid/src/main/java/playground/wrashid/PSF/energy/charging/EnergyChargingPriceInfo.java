package playground.wrashid.PSF.energy.charging;

import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF.data.HubPriceInfo;

public class EnergyChargingPriceInfo {
	
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
		time = Math.floor(time) % 86400;

		// testing scenario
		if (ParametersPSF.isTestingModeOn()) {
			// in the testing scenario, there is only one hub
			return ParametersPSF.getHubPriceInfo().getPrice(time);
		} else {
			// get hub and time specific electricity price
			HubLinkMapping hubLinkMapping=ParametersPSF.getHubLinkMapping();
			return ParametersPSF.getHubPriceInfo().getPrice(time, hubLinkMapping.getHubNumber(linkId.toString()));
		}	
	}
}
