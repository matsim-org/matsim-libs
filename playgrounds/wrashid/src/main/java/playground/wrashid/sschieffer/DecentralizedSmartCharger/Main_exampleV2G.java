
/* *********************************************************************** *
 * project: org.matsim.*
 * Main_exampleV2G.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

//package playground.wrashid.sschieffer;
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.io.IOException;


import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import java.util.*;


/**
 * This class highlights how to use V2G functions of this package:
 * 
 * V2G can only follow after the Decentralized Smart Charger has been run
 * since its based on the charging schedules planned by the Decentralized Smart Charger
 * 
 * For all stochastic loads the V2G procedure will then check if rescheduling is possible 
 * and if the utility of the agent can be increased by rescheduling.
 * 
 * 
 *PLease provide the following folders in the output path to store results locally
 * please provide a folder Output with the following sub-folders
 * <ul>
 * 		<li>DecentralizedCharger
 * 			<ul>
 * 				<li>agentPlans
 * 				<li>LP	
 * 					<ul><li>EV <li> PHEV</ul>
 * 			</ul>
 * 		<li>Hub
 * 		<li>V2G
 * 			<ul>
 * 				<li>agentPlans
 * 				<li>LP
 * 					<ul><li>EV <li> PHEV</ul>
 *  		</ul>
 * </ul>
 * @author Stella
 *
 */
public class Main_exampleV2G {
	
	
	
	public static void main(String[] args) throws IOException {
		
		double energyPricePerkWh=0.25;
		double standardConnectionElectricityJPerSecond= 3500; 
		final double optimalPrice=energyPricePerkWh*1/1000*1/3600*standardConnectionElectricityJPerSecond;//0.25 CHF per kWh		
		final double suboptimalPrice=optimalPrice*3; // cost/second  
		
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final double xPercentNone=0.0;
		final double xPercentDown=0.0;
		final double xPercentDownUp=1.0;
		
		
		/**
		 * Decentralized Smart Charger
		 */
		
		/*
		 * Simulation variables: 
		 * - Controler of the simulation in which the charging optimization is embedded
		 * - ParkingTimesPlugin
		 * 
		 */
		final Controler controler;
		final ParkingTimesPlugin parkingTimesPlugin;					
		
		/*
		 * GAS TYPES
		 * 
		 * 
		 * - Gas Price [currency]
		 * - Joules per liter in gas [J]
		 * - emissions of Co2 per liter gas [kg]
		 
		 */
		double gasPricePerLiter= 0.25; 
		double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
		double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l
		
		GasType normalGas=new GasType("normal gas", 
				gasJoulesPerLiter, 
				gasPricePerLiter, 
				emissionPerLiter);
		
		
		/*
		 * Battery characteristics:
		 * - full capacity [J]
		 * e.g. common size is 24kWh = 24kWh*3600s/h*1000W/kW = 24*3600*1000Ws= 24*3600*1000J
		 * - minimum level of state of charge, avoid going below this SOC= batteryMin
		 * (0.1=10%)
		 * - maximum level of state of charge, avoid going above = batteryMax
		 * (0.9=90%)
		 * 
		 * Create desired Battery Types
		 */
		double batterySizeEV= 24*3600*1000; 
		double batterySizePHEV= 24*3600*1000; 
		double batteryMinEV= 0.1; 
		double batteryMinPHEV= 0.1; 
		double batteryMaxEV= 0.9; 
		double batteryMaxPHEV= 0.9; 		
		
		Battery EVBattery = new Battery(batterySizeEV, batteryMinEV, batteryMaxEV);
		Battery PHEVBattery = new Battery(batterySizePHEV, batteryMinPHEV, batteryMaxPHEV);
		
		
		VehicleType EVTypeStandard= new VehicleType("standard EV", 
				EVBattery, 
				null, 
				new ElectricVehicle(null, new IdImpl(1)),
				80000);// Nissan leaf 80kW Engine
		
		VehicleType PHEVTypeStandard= new VehicleType("standard PHEV", 
				PHEVBattery, 
				normalGas, 
				new PlugInHybridElectricVehicle(new IdImpl(1)),
				80000);
		
		final VehicleTypeCollector myVehicleTypes= new VehicleTypeCollector();
		myVehicleTypes.addVehicleType(EVTypeStandard);
		myVehicleTypes.addVehicleType(PHEVTypeStandard);
		
		
		

		/*
		 * LP optimization parameters
		 * - battery buffer for charging (e.g. 0.2=20%, agent will have charged 20% more 
		 * than what he needs before starting the next trip ); 
		 * 
		 */
		final double bufferBatteryCharge=0.0;
		
		/*
		 * Charging Distribution
		 * - minimum charging length [s]
		 * - time resolution of optimization
		 */
		final double minChargingLength=5*60;//5 minutes
		
		
		/*
		 * Network  - Electric Grid Information
		 * 
		 * - distribution of free load [W] available for charging over the day (deterministicHubLoadDistribution)
		 * this is given in form of a LinkedListValueHashMap, where Integer corresponds to a hub and 
		 * the Schedule includes LoadDistributionIntervals which represent the free load 
		 * (LoadDistributionIntervals indicate a time interval: start second, end second and also have a PolynomialFunction)
		 * 
		 * - pricing (pricingHubDistribution)
		 * is also given as LinkedListValueHashMap, where Integer corresponds to a hub and
		 * the Schedule includes LoadDistributionIntervals which represent the price per second over the day
		 */
		DetermisticLoadAndPricingCollector dlpc= new DetermisticLoadAndPricingCollector();
		
		final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution
			= dlpc.getDeterminisitcHubLoad();
		
		final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution
			= dlpc.getDeterminisitcPriceDistribution();
		
		// HubLinkMapping links linkIds (Id) to Hubs (Integer)
		final HubLinkMapping hubLinkMapping=new HubLinkMapping(deterministicHubLoadDistribution.size());//= new HubLinkMapping(0);
		
		
				
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\"; //"C:\\Users\\stellas\\Output\\V1G\\";
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		controler=new Controler(configPath);
		
		
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		
		final EnergyConsumptionInit e= new EnergyConsumptionInit(
				phev, ev, combustion);
		
		controler.addControlerListener(e);
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					// map linkIds to hubs once the scenario is read in from the config file
					StellasHubMapping setHubLinkMapping= new StellasHubMapping(controler);
					final HubLinkMapping hubLinkMapping=setHubLinkMapping.mapHubs();
					
					
					/******************************************
					 * for Decentralized Smart Charging
					 * *****************************************
					 */
					
					//initialize parameters
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), //controler
							parkingTimesPlugin, //ParkingTimesPlugIn
							e.getEnergyConsumptionPlugin(),
							outputPath, 
							myVehicleTypes
							);
					
					
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(minChargingLength);
					
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,							
							pricingHubDistribution
							);
					
					//RUN
					myDecentralizedSmartCharger.run();
					
					
					/******************************************
					 * V2G
					 * *****************************************
					 * V2G can only follow after Decentralized Smart Charger has been run
					 * since it relies on the planned charging schedules
					 * 
					 * For all stochastic loads the V2G procedure will then check if rescheduling is possible 
					 * and if the utility of the agent can be increased by rescheduling.
					 */
					
				
					/*
					 * ************************
					 * Stochastic Sources
					 * they need to be defined per scenario. dummy values have been entered in 
					 * StochasticLoadCollector
					 */
					
					StochasticLoadCollector slc= new StochasticLoadCollector(controler);
					
					LinkedListValueHashMap<Integer, Schedule> stochasticHubLoadDistribution=
						slc.getStochasticHubLoad();
						
					LinkedListValueHashMap<Integer, Schedule> locationSourceMapping= 
						slc.getStochasticHubSources();
					
					LinkedListValueHashMap<Id, Schedule> agentVehicleSourceMapping= 
						slc.getStochasticAgentVehicleSources();
						
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticHubLoadDistribution,
							locationSourceMapping,
							agentVehicleSourceMapping);
					
					
					/*
					 * ************************
					 * Agent contracts
					 */
					double compensationPerKWHRegulationUp=0.15;
					double compensationPerKWHRegulationDown=0.15;
					 
					AgentContractCollector myAgentContractsCollector= new AgentContractCollector (
							myDecentralizedSmartCharger,
							 compensationPerKWHRegulationUp,
							 compensationPerKWHRegulationDown);
					
					
					LinkedListValueHashMap<Id, ContractTypeAgent> agentContracts= 
						myAgentContractsCollector.makeAgentContracts(
								controler,
								xPercentNone,
								xPercentDown,
								xPercentDownUp);
						
					
					myDecentralizedSmartCharger.setAgentContracts(agentContracts);
					
					
					/*
					 * ************************
					 * RUn
					 */
					
					myDecentralizedSmartCharger.initializeAndRunV2G();
					
					/*
					 * Example how to Use V2G
					 */
					LinkedListValueHashMap<Id, Double> agentRevenuesFromV2G=
						myDecentralizedSmartCharger.getAgentV2GRevenues();
					
					
					//*****************************************
					//END
					//*****************************************
					myDecentralizedSmartCharger.clearResults();
					
				
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
			}
		});
		
				
		controler.run();		
				
	}
	
	


}
