package playground.wrashid.PSF.energy.charging.optimizedCharging;

import java.util.HashMap;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;

import playground.wrashid.PSF.ParametersPSF;
import playground.wrashid.PSF.V2G.BatteryStatistics;
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

	private HashMap<Id<Person>, EnergyConsumption> energyConsumption;
	private HashMap<Id<Person>, ParkingTimes> parkingTimes;
	private HashMap<Id<Person>, ChargingTimes> chargingTimes = new HashMap<>();

	public OptimizedCharger(HashMap<Id<Person>, EnergyConsumption> energyConsumption, HashMap<Id<Person>, ParkingTimes> parkingTimes, double defaultMaxBatteryCapacity) {
		this.energyConsumption = energyConsumption;
		this.parkingTimes = parkingTimes;
		performOptimizedCharging(defaultMaxBatteryCapacity);
	}

	// String testingMaxEnergyPriceWillingToPay =
	// Gbl.getConfig().findParam("PSF", "testing.MaxEnergyPriceWillingToPay");
	// String testingMaxBatteryCapacity = Gbl.getConfig().findParam("PSF",
	// "testing.maxBatteryCapacity");

	// TODO: this operation could be parallelized later...
	private void performOptimizedCharging(final double defaultMaxBatteryCapacity) {

		Iterator<Id<Person>> iter = energyConsumption.keySet().iterator();

		// iterate through all vehicles and find their optimal charging time
		while (iter.hasNext()) {
			Id<Person> personId = iter.next();

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
			//chargingTimes.print();
			//System.out.println("===================");

		}



	}

	public void outputOptimizationData(AfterMobsimEvent event){
		// write out charging events to file, if specified
		// TODO: 'remove parameter main.chargingTimesOutputFilePath from config'
		if (ParametersPSF.getMainChargingTimesOutputFilePath()!=null){
			// write it to the iteration folder
			ChargingTimes.writeChargingTimes(chargingTimes, event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "chargingLog.txt"));

			// also write it directly into the output folder, so that it can be used by the PSS
			ChargingTimes.writeChargingTimes(chargingTimes, event.getServices().getControlerIO().getOutputFilename("chargingLog.txt"));
		}


		// write out energy usage statistics (data and graph)
		// the hub link mapping is not on during testing mode.
		if ((ParametersPSF.getMainEnergyUsageStatistics()!=null) && !ParametersPSF.isTestingModeOn()){
			double[][] energyUsageStatistics = ChargingTimes.getEnergyUsageStatistics(chargingTimes,ParametersPSF.getHubLinkMapping());

			//ChargingTimes.writeEnergyUsageStatisticsData(ParametersPSF.getMainEnergyUsageStatistics() + ".txt", energyUsageStatistics, ParametersPSF.getHubLinkMapping().getNumberOfHubs());
			//ChargingTimes.writeVehicleEnergyConsumptionStatisticsGraphic(ParametersPSF.getMainEnergyUsageStatistics() + ".png",energyUsageStatistics);
			// TODO: remove the parameter ParametersPSF.getMainEnergyUsageStatistics() from the config file.

			// write data into iterations folder
			ChargingTimes.writeEnergyUsageStatisticsData(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "vehicleEnergyConsumption.txt"), energyUsageStatistics);

			ChargingTimes.writeVehicleEnergyConsumptionStatisticsGraphic(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "vehicleEnergyConsumption.png"),energyUsageStatistics);



			// TODO: the v2g power could be changed later.
			double[][] gridConnectedPower=BatteryStatistics.getGridConnectedPower(parkingTimes,ParametersPSF.getDefaultChargingPowerAtParking());
			BatteryStatistics.writeGridConnectedPower(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "gridConnectedVehiclePower.png"), gridConnectedPower);
			BatteryStatistics.writeGridConnectedPowerData(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "gridConnectedVehiclePower.txt"), gridConnectedPower);

			double[][] gridConnectedEnergy=BatteryStatistics.getGridConnectedEnergy(chargingTimes, parkingTimes);
			BatteryStatistics.writeGridConnectedEnergy(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "gridConnectedVehicleEnergy.png"), gridConnectedEnergy);
			BatteryStatistics.writeGridConnectedEnergyData(event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "gridConnectedVehicleEnergy.txt"), gridConnectedEnergy);
		}

		// output the price graphics
		ParametersPSF.getHubPriceInfo().writePriceGraph(ParametersPSF.getMainHubPriceGraphFileName());

		// output base load data
		if (ParametersPSF.getMainBaseLoad()!=null){
			ChargingTimes.writeEnergyConsumptionGraphic(ParametersPSF.getMainBaseLoadOutputGraphFileName(), ParametersPSF.getMainBaseLoad(), "Base Load");
		}
	}

	public HashMap<Id<Person>, ChargingTimes> getChargingTimes() {
		return chargingTimes;
	}

	// TODO: also check at parking, if charging plug is available at charging at all...
	//

}
