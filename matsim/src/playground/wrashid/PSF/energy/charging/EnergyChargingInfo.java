package playground.wrashid.PSF.energy.charging;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;

public class EnergyChargingInfo {

	/*
	 * time in seconds
	 * - get the energy price at the specified facility and time of day
	 * 
	 * // we are taking a modulo of each day (so that plans taking many days can also be evaluated
				// perhaps a problem: if the cost of charging is too big, it might be better to make the plans longer, so
				// that a person can charge for lesser money.
	 * 
	 */
	public static double getEnergyPrice(double time, Id facility){
		
		
		// fit time into 24 hours
		time = Math.round(time) % 86400;
		
		String testingPeakHourElectricityPrice = Gbl.getConfig().findParam("PSF", "testing.peakHourElectricityPrice");
		String testingLowTariffElectrictyPrice = Gbl.getConfig().findParam("PSF", "testing.lowTariffElectrictyPrice");
		if (testingPeakHourElectricityPrice!=null || testingLowTariffElectrictyPrice!=null){
			if (time<25200 || time>72000){
				// if low tariff
				return Double.parseDouble(testingLowTariffElectrictyPrice) ;
			} else{
				// if peak hour
				return  Double.parseDouble(testingPeakHourElectricityPrice);
			} 
		}
		
		
		// TODO: make the real implementation here
		return 1.0;
	}
	
}
