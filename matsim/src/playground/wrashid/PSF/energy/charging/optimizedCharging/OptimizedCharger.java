package playground.wrashid.PSF.energy.charging.optimizedCharging;

import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.gbl.Gbl;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.energy.charging.ChargingTimes;
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

	// TODO: perhaps add EnergyChargeInfo later here as an object...

	private HashMap<Id, EnergyConsumption> energyConsumption;
	private HashMap<Id, ParkingTimes> parkingTimes;
	private HashMap<Id, ChargingTimes> chargingTimes = new HashMap<Id, ChargingTimes>();

	public OptimizedCharger(HashMap<Id, EnergyConsumption> energyConsumption, HashMap<Id, ParkingTimes> parkingTimes) {
		this.energyConsumption = energyConsumption;
		this.parkingTimes = parkingTimes;
		performOptimizedCharging();
	}

	// String testingMaxEnergyPriceWillingToPay =
	// Gbl.getConfig().findParam("PSF", "testing.MaxEnergyPriceWillingToPay");
	// String testingMaxBatteryCapacity = Gbl.getConfig().findParam("PSF",
	// "testing.maxBatteryCapacity");

	// TODO: this operation could be parallelized later...
	private void performOptimizedCharging() {

		double defaultMaxBatteryCapacity = Double.parseDouble(Gbl.getConfig().findParam("PSF", "default.maxBatteryCapacity"));

		Iterator<Id> iter = energyConsumption.keySet().iterator();

		// iterate through all vehicles and find their optimal charging time
		while (iter.hasNext()) {
			Id personId = iter.next();

			// initialize the Charging times
			// TODO: later for each individual car we should be able to read the
			// max Battery capacities
			
			
			//System.out.println("personId: " + personId);
			
			
			EnergyConsumption agentEnergyConsumption=energyConsumption.get(personId);
			double maxBatteryCapacity = defaultMaxBatteryCapacity;
			ChargingTimes chargingTimes = new ChargingTimes();
			this.chargingTimes.put(personId, chargingTimes);

			EnergyBalance eb = new EnergyBalance(parkingTimes.get(personId), agentEnergyConsumption, maxBatteryCapacity,
					chargingTimes);

			
			if (personId.toString().equalsIgnoreCase("107909")){
			//	System.out.println();
			}
			
			chargingTimes = eb.getChargingTimes(agentEnergyConsumption);

			// write out charging events to the console and also to a file (if specified)
			chargingTimes.print();
			//System.out.println("===================");	

		}
		
		outputOptimizationData();

	}
	
	private void outputOptimizationData(){
		// write out charging events to file, if specified
		if (ParametersPSF.getMainChargingTimesOutputFilePath()!=null){
			ChargingTimes.writeChargingTimes(chargingTimes, ParametersPSF.getMainChargingTimesOutputFilePath());
		}
		
		
		
		if (ParametersPSF.getMainEnergyUsageStatistics()!=null){
			double[][] energyUsageStatistics = ChargingTimes.getEnergyUsageStatistics(chargingTimes,ParametersPSF.getHubLinkMapping()); 
			
			// write out grafics
			ChargingTimes.writeEnergyUsageStatisticsGrafic(ParametersPSF.getMainEnergyUsageStatistics() + ".png",energyUsageStatistics, ParametersPSF.getHubLinkMapping().getNumberOfHubs());
		
			// TODO: write out text file
			
		}
		
		//TODO: output grafics for (input) price at each hub and output for each hub
	}
	
	public HashMap<Id, ChargingTimes> getChargingTimes() {
		return chargingTimes;
	}

	// TODO: also check at parking, if charging plug is available at charging at all...
	// 

}
