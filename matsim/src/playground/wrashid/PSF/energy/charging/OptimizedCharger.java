package playground.wrashid.PSF.energy.charging;

import java.util.HashMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.energy.consumption.EnergyConsumption;
import playground.wrashid.PSF.parking.ParkingTimes;

/*
 * From the consumed energy per leg, the parking times and the energy prices, 
 * we can calculate the times, when energy was effectively charged for each vehicle (this is the output)
 * 
 * - we need to put a class for reading energy prices here.
 */
public class OptimizedCharger {

	// depends both on the plug interface available at the car and the parking
	// facility
	// => use types of plugs for charging and do a match, which have different
	// charging capabilities...

	// public LinkedList<EnergyConsumption> getOptimalChargingTimes(){
	//	
	// }

	private EnergyChargingInfo chargingInfo;
	private HashMap<Id, EnergyConsumption> energyConsumption;
	private HashMap<Id, ParkingTimes> parkingTimes;

	public OptimizedCharger(EnergyChargingInfo chargingInfo, HashMap<Id, EnergyConsumption> energyConsumption,
			HashMap<Id, ParkingTimes> parkingTimes) {
		this.chargingInfo = chargingInfo;
		this.energyConsumption = energyConsumption;
		this.parkingTimes = parkingTimes;
		
		performOptimizedCharging();
	}

	// String testingMaxEnergyPriceWillingToPay =
	// Gbl.getConfig().findParam("PSF", "testing.MaxEnergyPriceWillingToPay");
	// String testingMaxBatteryCapacity = Gbl.getConfig().findParam("PSF",
	// "testing.maxBatteryCapacity");

	private void performOptimizedCharging() {

	}

	
	public void getChargings() {

	}

	// TODO: also check at parking, if we can really charge there...
	// 

}
