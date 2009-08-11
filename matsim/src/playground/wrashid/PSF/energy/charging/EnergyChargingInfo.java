package playground.wrashid.PSF.energy.charging;

import org.matsim.core.gbl.Gbl;

public class EnergyChargingInfo {

	/*
	 * time in seconds
	 */
	public double getEnergyPrice(double time, double facility){
		
		
		String testingPeakHourElectricityPrice = Gbl.getConfig().findParam("PSF", "testing.peakHourElectricityPrice");
		String testingLowTariffElectrictyPrice = Gbl.getConfig().findParam("PSF", "testing.lowTariffElectrictyPrice");
		if (testingPeakHourElectricityPrice!=null || testingLowTariffElectrictyPrice!=null){
			if (time<25200 || time>72000){
				// if low tariff
				return Double.parseDouble(testingLowTariffElectrictyPrice);
			} else{
				// if peak hour
				return Double.parseDouble(testingPeakHourElectricityPrice);
			} 
		}
		
		
		// TODO: make the real implementation here
		return 1.0;
	}
	
}
