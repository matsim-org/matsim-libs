package playground.wrashid.tryouts.zain;


import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.lib.obj.DoubleValueHashMap;
import org.matsim.contrib.transEnergySim.charging.ChargingUponArrival;
import org.matsim.contrib.transEnergySim.chargingInfrastructure.road.InductiveStreetCharger;
import org.matsim.contrib.transEnergySim.controllers.InductiveChargingController;
import org.matsim.contrib.transEnergySim.vehicles.api.Vehicle;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionModel;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.EnergyConsumptionTracker;
import org.matsim.contrib.transEnergySim.vehicles.energyConsumption.galus.EnergyConsumptionModelGalus;
import org.matsim.contrib.transEnergySim.vehicles.impl.InductivelyChargableBatteryElectricVehicle;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class InductiveCharging {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		
		Config config= ConfigUtils.loadConfig("C:/tmp/Inductive charging data/input files/config_Mini_Playground.xml");
	
		EnergyConsumptionModel ecm=new EnergyConsumptionModelGalus();
		
		HashMap<Id<Vehicle>, Vehicle> vehicles=new HashMap<>();
		
		
		
		int batteryCapacityInJoules = 10*1000*3600;
		//int batteryCapacityInJoules = 26516889;   
		//int batteryCapacityInJoules = 18000000;
		//int batteryCapacityInJoules = 0;
		/*for (int i=0;i<100;i++){
			IdImpl agentId = new IdImpl("pid" + i);
			vehicles.put(agentId, new IC_BEV(ecm,batteryCapacityInJoules));
		}
		*/
		Id<Vehicle> agentId = Id.create("pid" + 0, Vehicle.class);
		vehicles.put(agentId, new InductivelyChargableBatteryElectricVehicle(ecm,batteryCapacityInJoules));
		
		InductiveChargingController controller = new InductiveChargingController(config,vehicles);

		EnergyConsumptionTracker energyConsumptionTracker = controller.getEnergyConsumptionTracker();
		InductiveStreetCharger inductiveCharger = controller.getInductiveCharger();
		ChargingUponArrival chargingUponArrival= controller.getChargingUponArrival();
		
		DoubleValueHashMap<Id<Link>> chargableStreets=new DoubleValueHashMap<Id<Link>>();
		chargableStreets.put(Id.createLinkId("2223"), 3500.0);
		chargableStreets.put(Id.createLinkId("2322"), 3500.0);
		chargableStreets.put(Id.createLinkId("1213"), 3500.0);
		chargableStreets.put(Id.createLinkId("1312"), 3500.0);
		
		inductiveCharger.setChargableStreets(chargableStreets);
		//inductiveCharger.setSamePowerAtAllStreets(3000);
		
		
		//chargingUponArrival.setPowerForNonInitializedActivityTypes(controller.getFacilities(), 3500);
		chargingUponArrival.getChargablePowerAtActivityTypes().put("h", 3312.0);
		chargingUponArrival.getChargablePowerAtActivityTypes().put("w", 3312.0);


		controller.getConfig().controler().setOverwriteFileSetting(
				true ?
						OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles :
						OutputDirectoryHierarchy.OverwriteFileSetting.failIfDirectoryExists );
		controller.run();
		controller.printStatisticsToConsole();
		
	}

}
