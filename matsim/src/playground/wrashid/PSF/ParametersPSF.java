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

	private static String testing_ModeOn = "testingModeOn";
	private static boolean testingModeOn = false;

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
	
	private static double testingPeakPriceStartTime = 25200;
	private static double testingPeakPriceEndTime = 72000;

	public static void readConfigParamters(Controler controler) {
		String tempStringValue;

		tempStringValue = controler.getConfig().findParam(PSFModule, default_maxBatteryCapacity);
		if (tempStringValue != null) {
			defaultMaxBatteryCapacity = Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(default_maxBatteryCapacity);
		}

		tempStringValue = controler.getConfig().findParam(PSFModule, default_chargingPowerAtParking);
		if (tempStringValue != null) {
			defaultChargingPowerAtParking = Double.parseDouble(tempStringValue);
		} else {
			errorReadingParameter(default_chargingPowerAtParking);
		}

		tempStringValue = controler.getConfig().findParam(PSFModule, testing_ModeOn);
		if (tempStringValue != null) {
			testingModeOn = Boolean.parseBoolean(tempStringValue);
		} else {
			errorReadingParameter(default_chargingPowerAtParking);
		}

		if (testingModeOn) {
			tempStringValue = controler.getConfig().findParam(PSFModule, testing_energyConsumptionPerLink);
			if (tempStringValue != null) {
				testingEnergyConsumptionPerLink = Double.parseDouble(tempStringValue);
			} else {
				errorReadingParameter(testing_energyConsumptionPerLink);
			}

			tempStringValue = controler.getConfig().findParam(PSFModule, testing_maxEnergyPriceWillingToPay);
			if (tempStringValue != null) {
				testingMaxEnergyPriceWillingToPay = Double.parseDouble(tempStringValue);
			}else {
				errorReadingParameter(testing_maxEnergyPriceWillingToPay);
			}

			tempStringValue = controler.getConfig().findParam(PSFModule, testing_peakHourElectricityPrice);
			if (tempStringValue != null) {
				testingPeakHourElectricityPrice = Double.parseDouble(tempStringValue);
			}else {
				errorReadingParameter(testing_peakHourElectricityPrice);
			}

			tempStringValue = controler.getConfig().findParam(PSFModule, testing_lowTariffElectrictyPrice);
			if (tempStringValue != null) {
				testingLowTariffElectrictyPrice = Double.parseDouble(tempStringValue);
			}else {
				errorReadingParameter(testing_lowTariffElectrictyPrice);
			}
		}
		
		resetInternalParameters();
	}
	
	private static void resetInternalParameters(){
		testingPeakPriceStartTime = 25200;
		testingPeakPriceEndTime = 72000;
	}

	private static void errorReadingParameter(String parameterName) {
		log.error("parameter '" + parameterName + "' could not be read");
	}

	public static double getDefaultMaxBatteryCapacity() {
		return defaultMaxBatteryCapacity;
	}

	public static void setDefaultMaxBatteryCapacity(double defaultMaxBatteryCapacity) {
		ParametersPSF.defaultMaxBatteryCapacity = defaultMaxBatteryCapacity;
	}

	public static double getDefaultChargingPowerAtParking() {
		return defaultChargingPowerAtParking;
	}

	public static void setDefaultChargingPowerAtParking(double defaultChargingPowerAtParking) {
		ParametersPSF.defaultChargingPowerAtParking = defaultChargingPowerAtParking;
	}

	public static boolean isTestingModeOn() {
		return testingModeOn;
	}

	public static void setTestingModeOn(boolean testingModeOn) {
		ParametersPSF.testingModeOn = testingModeOn;
	}

	public static double getTestingEnergyConsumptionPerLink() {
		return testingEnergyConsumptionPerLink;
	}

	public static void setTestingEnergyConsumptionPerLink(double testingEnergyConsumptionPerLink) {
		ParametersPSF.testingEnergyConsumptionPerLink = testingEnergyConsumptionPerLink;
	}

	public static double getTestingMaxEnergyPriceWillingToPay() {
		return testingMaxEnergyPriceWillingToPay;
	}

	public static void setTestingMaxEnergyPriceWillingToPay(double testingMaxEnergyPriceWillingToPay) {
		ParametersPSF.testingMaxEnergyPriceWillingToPay = testingMaxEnergyPriceWillingToPay;
	}

	public static double getTestingPeakHourElectricityPrice() {
		return testingPeakHourElectricityPrice;
	}

	public static void setTestingPeakHourElectricityPrice(double testingPeakHourElectricityPrice) {
		ParametersPSF.testingPeakHourElectricityPrice = testingPeakHourElectricityPrice;
	}

	public static double getTestingLowTariffElectrictyPrice() {
		return testingLowTariffElectrictyPrice;
	}

	public static void setTestingLowTariffElectrictyPrice(double testingLowTariffElectrictyPrice) {
		ParametersPSF.testingLowTariffElectrictyPrice = testingLowTariffElectrictyPrice;
	}

	public static double getTestingPeakPriceStartTime() {
		return testingPeakPriceStartTime;
	}

	public static void setTestingPeakPriceStartTime(double testingPeakPriceStartTime) {
		ParametersPSF.testingPeakPriceStartTime = testingPeakPriceStartTime;
	}

	public static double getTestingPeakPriceEndTime() {
		return testingPeakPriceEndTime;
	}

	public static void setTestingPeakPriceEndTime(double testingPeakPriceEndTime) {
		ParametersPSF.testingPeakPriceEndTime = testingPeakPriceEndTime;
	}

}
