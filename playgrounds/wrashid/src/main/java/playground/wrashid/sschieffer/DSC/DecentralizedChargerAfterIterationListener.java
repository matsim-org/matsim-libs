package playground.wrashid.sschieffer.DSC;

import java.util.HashMap;
import java.util.LinkedList;


import org.matsim.api.core.v01.Id;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.SetUp.SetUpVehicleCollector;

/**
 * implements IterationEndsListener
 * to be called after an iteration, if the DecentralizedSmartCharging Optimization shall be run.
 * 
 * 
 * @author Stella
 *
 */
public class DecentralizedChargerAfterIterationListener implements IterationEndsListener{
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		
		try {
			/**
			 *  This class sets up the Vehicle types for the simulation
			 * it defines gas types, battery types and vehicle types
			 * <li>"normal gas" 
			 * (gasPricePerLiter= 0.25; gasJoulesPerLiter = 43 MJ/kg; emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l)
			 * <li>EV battery type (24kWH, minSOC=0.1, maxSOC=0.9, engine 80kW)
			 * <li>PHEV battery type  (24kWH, minSOC=0.1, maxSOC=0.9, engine 80k)
			 * 
			 * you can modify the default values in the class
			 */
			SetUpVehicleCollector sv= new SetUpVehicleCollector();
			//double kWHEV, double kWHPHEV, boolean gasHigh
			final VehicleTypeCollector myVehicleTypes = sv.setUp(
					DecentralizedChargingSimulation.kWHEV, 
					DecentralizedChargingSimulation.kWHPHEV, 
					DecentralizedChargingSimulation.gasHigh);
			
			
			/******************************************
			 * Setup for Decentralized Smart Charging
			 * *****************************************
			 */
			DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
					event.getControler(), //Controler
					DecentralizedChargingSimulation.parkingTimesPlugin, //ParkingTimesPlugIn
					DecentralizedChargingSimulation.energyInit.getEnergyConsumptionPlugin(), // EnergyConsumptionPlugIn
					DecentralizedChargingSimulation.outputPath, // where to save the data
					myVehicleTypes // the defined vehicle types(gas, battery)
					);
			
			
			
			//set battery reserve
			myDecentralizedSmartCharger.initializeLP(
					DecentralizedChargingSimulation.bufferBatteryCharge,
					DecentralizedChargingSimulation.LPoutput
					);
			
			// set standard charging slot length
			myDecentralizedSmartCharger.initializeChargingSlotDistributor(DecentralizedChargingSimulation.standardChargingLength);
			
			// set LinkedList of vehicles <agentId, vehicle>
			myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
					DecentralizedChargingSimulation.energyInit.getElectricVehicles());
			
			
			/*
			 * HubLinkMapping links linkIds (Id) to Hubs (Integer)
			 * this hubMapping needs to be done individually for every scenario, please write your own class/function here
			 *  - an example is provided in StellasHubMapping and follows the following format
			 *  which creates a HubLinkMapping hubLinkMapping=new HubLinkMapping(int numberOfHubs);
			 *  hubLinkMapping.addMapping(linkId, hubNumber);
			 */
			
			final HubLinkMapping hubLinkMapping=
				DecentralizedChargingSimulation.myMappingClass.mapHubs(DecentralizedChargingSimulation.controler);
			
					
			DecentralizedChargingSimulation.loadPricingCollector.setUp();
			
			final HashMap<Integer, Schedule> deterministicHubLoadDistribution
				= DecentralizedChargingSimulation.loadPricingCollector.getDeterministicHubLoad();
			
			final HashMap<Integer, Schedule> pricingHubDistribution
			= DecentralizedChargingSimulation.loadPricingCollector.getDeterministicPriceDistribution();
			
			myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
					hubLinkMapping, 
					deterministicHubLoadDistribution,							
					pricingHubDistribution,
					DecentralizedChargingSimulation.loadPricingCollector.getLowestPriceKWHAllHubs(),
					DecentralizedChargingSimulation.loadPricingCollector.getHighestPriceKWHAllHubs()
					);
			
			
						
			myDecentralizedSmartCharger.run();			
			
			DecentralizedChargingSimulation.setDecentralizedSmartCharger(myDecentralizedSmartCharger);
			
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
	}
}
