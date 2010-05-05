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

public class AfterSimulationListener implements AfterMobsimListener {

	private static final Logger log = Logger.getLogger(AfterSimulationListener.class);

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

		addCostOfElectricalChargingToTheScore();
	}

	private void addCostOfElectricalChargingToTheScore() {
		for (Id personId : chargingTimes.keySet()) {
			ChargingTimes curChargingTime = chargingTimes.get(personId);

			for (ChargeLog chargeLog : curChargingTime.getChargingTimes()) {
				double chargingStartTime = chargeLog.getStartChargingTime();
				// the price is CHF per kWh (from Matthias Galus),there for
				// converting the price to CHF per Joule (1kWh=3'600'000 Joule)
				// Ask Galus again, this was something else (although adjustable through the chargingPriceScalingFactor
				double chargingPrice = ParametersPSF.getHubPriceInfo().getPrice(chargingStartTime,
						ParametersPSF.getHubLinkMapping().getHubNumber(chargeLog.getLinkId().toString())) / 3600000;

				double energyCharged = chargeLog.getEnergyCharged();
				double negativeUtilitiesForCharging = -1 * chargingPrice * energyCharged
						* ParametersPSF.getMainChargingPriceScalingFactor();


				// add price to score.
				ParametersPSF.getEvents().processEvent(
						new AgentMoneyEventImpl(chargingStartTime, personId, negativeUtilitiesForCharging));

				if (ParametersPSF.getMainChargingPriceScalingFactor() == -1.0) {
					log.fatal("charging price scaling factor used, but not initialized (e.g. in config file)");
					System.exit(0);
				}
			}
		}
	}

	public AfterSimulationListener(LogEnergyConsumption logEnergyConsumption, LogParkingTimes logParkingTimes) {
		this.logEnergyConsumption = logEnergyConsumption;
		this.logParkingTimes = logParkingTimes;
	}

}
