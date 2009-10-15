package playground.wrashid.PSF.energy;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.energy.consumption.LogEnergyConsumption;
import playground.wrashid.PSF.parking.LogParkingTimes;
import playground.wrashid.PSF.parking.ParkingTimes;

public class AfterSimulationListener implements AfterMobsimListener{

	private LogEnergyConsumption logEnergyConsumption;
	private LogParkingTimes logParkingTimes;

	
	// after each execution, add the score of the 
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		
		
		OptimizedCharger optimizedCharger = new OptimizedCharger(logEnergyConsumption.getEnergyConsumption(), logParkingTimes
				.getParkingTimes());
		HashMap<Id, ChargingTimes> chargingTimes = optimizedCharger.getChargingTimes();

		ChargingTimes.printEnergyUsageStatistics(chargingTimes, ParametersPSF.getHubLinkMapping());
		
	}
	
	
	public AfterSimulationListener(LogEnergyConsumption logEnergyConsumption, LogParkingTimes logParkingTimes){
		this.logEnergyConsumption = logEnergyConsumption;
		this.logParkingTimes = logParkingTimes;
	}
}
