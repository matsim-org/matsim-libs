package playground.wrashid.PSF;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

public class ParametersPSF {

	private static final Logger log = Logger.getLogger(ParametersPSF.class);
	
	private static String PSFModule = "PSF";

	// default parameters
	private static String default_maxBatteryCapacity = "default.maxBatteryCapacity"; 
	private static double defaultMaxBatteryCapacity;
	// in [J]
	private static String default_chargingPowerAtParking = "default.chargingPowerAtParking";
	private static double defaultChargingPowerAtParking;
	// in [W]
	
	// testing parameters
	private static String testing_energyConsumptionPerLink = "testing.energyConsumptionPerLink";
	private static double testingEnergyConsumptionPerLink;
	// in [J]
	private static String testing_maxEnergyPriceWillingToPay = "testing.maxEnergyPriceWillingToPay";
	private static double testingMaxEnergyPriceWillingToPay;
	// in utils per [J]
	private static String testing_peakHourElectricityPrice = "testing.peakHourElectricityPrice";
	private static double testingPeakHourElectricityPrice;
	// in utils/J peakHour: from 07:00 to 20:00
	private static String testing_lowTariffElectrictyPrice = "testing.lowTariffElectrictyPrice";
	private static double testingLowTariffElectrictyPrice;
	// in utils/J lowTariff: from 20:00 to 07:00
	
	public static void readConfigParamters() {
		String tempStringValue;
		
		tempStringValue = Gbl.getConfig().findParam(PSFModule, default_maxBatteryCapacity);
		if (tempStringValue!=null){
			defaultMaxBatteryCapacity =  Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(default_maxBatteryCapacity);
		}
		
		tempStringValue = Gbl.getConfig().findParam(PSFModule, default_chargingPowerAtParking);
		if (tempStringValue!=null){
			defaultChargingPowerAtParking =  Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(default_chargingPowerAtParking);
		}
		
		tempStringValue = Gbl.getConfig().findParam(PSFModule, testing_energyConsumptionPerLink);
		if (tempStringValue!=null){
			testingEnergyConsumptionPerLink =  Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(testing_energyConsumptionPerLink);
		}
		
		tempStringValue = Gbl.getConfig().findParam(PSFModule, testing_maxEnergyPriceWillingToPay);
		if (tempStringValue!=null){
			testingMaxEnergyPriceWillingToPay =  Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(testing_maxEnergyPriceWillingToPay);
		}
		
		tempStringValue = Gbl.getConfig().findParam(PSFModule, testing_peakHourElectricityPrice);
		if (tempStringValue!=null){
			testingPeakHourElectricityPrice =  Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(testing_peakHourElectricityPrice);
		}
		
		tempStringValue = Gbl.getConfig().findParam(PSFModule, testing_lowTariffElectrictyPrice);
		if (tempStringValue!=null){
			testingLowTariffElectrictyPrice =  Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(testing_lowTariffElectrictyPrice);
		}
	}
	
	private static void errorReadingParameter(String parameterName){
		log.error("parameter '"+ parameterName +"' could not be read");
	}



	public String getPSFModule() {
		return PSFModule;
	}
	


	public double getDefaultMaxBatteryCapacity() {
		return defaultMaxBatteryCapacity;
	}



	public double getDefaultChargingPowerAtParking() {
		return defaultChargingPowerAtParking;
	}



	public double getTestingEnergyConsumptionPerLink() {
		return testingEnergyConsumptionPerLink;
	}



	public double getTestingMaxEnergyPriceWillingToPay() {
		return testingMaxEnergyPriceWillingToPay;
	}



	public double getTestingPeakHourElectricityPrice() {
		return testingPeakHourElectricityPrice;
	}



	public double getTestingLowTariffElectrictyPrice() {
		return testingLowTariffElectrictyPrice;
	}

}
