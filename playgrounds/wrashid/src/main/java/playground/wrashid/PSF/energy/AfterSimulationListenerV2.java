package playground.wrashid.PSF.energy;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.events.AgentMoneyEventImpl;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.ChargeLog;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;

public class AfterSimulationListenerV2 implements AfterMobsimListener {

	private static final Logger log = Logger.getLogger(AfterSimulationListenerV2.class);

	private static LogEnergyConsumption logEnergyConsumption;
	private static HashMap<Id, ChargingTimes> chargingTimes;

	public static HashMap<Id, ChargingTimes> getChargingTimes() {
		return chargingTimes;
	}

	public static LogEnergyConsumption getLogEnergyConsumption() {
		return logEnergyConsumption;
	}

	public static LogParkingTimes getLogParkingTimes() {
		return logParkingTimes;
	}

	public static OptimizedCharger getOptimizedCharger() {
		return optimizedCharger;
	}

	private static LogParkingTimes logParkingTimes;
	private static OptimizedCharger optimizedCharger;

	// after each execution, do charging of the cars and add the score
	public void notifyAfterMobsim(AfterMobsimEvent event) {

		optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(), logParkingTimes.getParkingTimes(), Double.parseDouble(event.getControler().getConfig().findParam("PSF", "default.maxBatteryCapacity")));
		optimizedCharger.outputOptimizationData(event);

		chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes.printEnergyUsageStatistics(chargingTimes, ParametersPSF.getHubLinkMapping());

		
	}

	

	public AfterSimulationListenerV2() {

	}

}
