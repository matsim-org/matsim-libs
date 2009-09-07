package playground.wrashid.PSF;

import org.apache.log4j.Logger;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF.data.HubPriceInfo;
import playground.wrashid.PSF.data.energyConsumption.AverageEnergyConsumptionBins;
import playground.wrashid.PSF.data.energyConsumption.AverageEnergyConsumptionGalus;

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
	private static String main_numberOfHubs = "main.numberOfHubs";
	private static int numberOfHubs;
	// number of hubs the network is divided into
	private static String main_hubPricesPath = "main.hubPricesPath";
	private static HubPriceInfo hubPriceInfo;
	// path of the file, where the electricity price of each hub during the day
	// is specified
	private static String main_hubLinkMapping = "main.hubLinkMapping";
	private static HubLinkMapping hubLinkMapping;
	// path of the file, where the electricity price of each hub during the day
	// is specified
	private static AverageEnergyConsumptionBins averageEnergyConsumptionBins;

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

		tempStringValue = controler.getConfig().findParam(PSFModule, main_numberOfHubs);
		if (tempStringValue != null) {
			numberOfHubs = Integer.parseInt(tempStringValue);
		} else {
			errorReadingParameter(main_numberOfHubs);
		}

		tempStringValue = controler.getConfig().findParam(PSFModule, main_hubPricesPath);
		if (tempStringValue != null) {
			hubPriceInfo = new HubPriceInfo(tempStringValue, numberOfHubs);
		} else {
			errorReadingParameter(main_hubPricesPath);
		}
		
		tempStringValue = controler.getConfig().findParam(PSFModule, main_hubLinkMapping);
		if (tempStringValue != null) {
			hubLinkMapping = new HubLinkMapping(tempStringValue, numberOfHubs);
		} else {
			errorReadingParameter(main_hubLinkMapping);
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
			} else {
				errorReadingParameter(testing_maxEnergyPriceWillingToPay);
			}

			tempStringValue = controler.getConfig().findParam(PSFModule, testing_peakHourElectricityPrice);
			if (tempStringValue != null) {
				testingPeakHourElectricityPrice = Double.parseDouble(tempStringValue);
			} else {
				errorReadingParameter(testing_peakHourElectricityPrice);
			}

			tempStringValue = controler.getConfig().findParam(PSFModule, testing_lowTariffElectrictyPrice);
			if (tempStringValue != null) {
				testingLowTariffElectrictyPrice = Double.parseDouble(tempStringValue);
			} else {
				errorReadingParameter(testing_lowTariffElectrictyPrice);
			}
			
		}

		// TODO: adapt this later, when we have better models (e.g. consider car type also)
		averageEnergyConsumptionBins=new AverageEnergyConsumptionGalus();
		
		resetInternalParameters();
	}

	public static HubLinkMapping getHubLinkMapping() {
		return hubLinkMapping;
	}

	private static void resetInternalParameters() {
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

	// testing parameters
	
	public static AverageEnergyConsumptionBins getAverageEnergyConsumptionBins() {
		return averageEnergyConsumptionBins;
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

	public static int getNumberOfHubs() {
		return numberOfHubs;
	}

	public static HubPriceInfo getHubPriceInfo() {
		return hubPriceInfo;
	}

	
	// if any thing needs to be processed after mutation of the parameters,
	// put it here
	public static void postMutationProcessing() {
		// set hub price info
		if (testingModeOn) {
			hubPriceInfo = new HubPriceInfo(ParametersPSF.getTestingPeakPriceStartTime(), ParametersPSF.getTestingPeakPriceEndTime(), ParametersPSF.getTestingLowTariffElectrictyPrice(), ParametersPSF.getTestingPeakHourElectricityPrice());	
		}
		
	}

}
