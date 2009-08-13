package playground.wrashid.PSF.energy;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;

import playground.wrashid.PSF.energy.charging.optimizedCharging.OptimizedCharger;
import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.parking.ParkingTimes;

public class AfterSimulationListener implements AfterMobsimListener{

	public void notifyAfterMobsim(AfterMobsimEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	// TODO: we need scoring later here...
	public AfterSimulationListener(HashMap<Id, EnergyConsumption> energyConsumption, HashMap<Id, ParkingTimes> parkingTimes){
		//OptimizedCharger optimizedCharger= new OptimizedCharger(energyConsumption,parkingTimes);
	}
}
