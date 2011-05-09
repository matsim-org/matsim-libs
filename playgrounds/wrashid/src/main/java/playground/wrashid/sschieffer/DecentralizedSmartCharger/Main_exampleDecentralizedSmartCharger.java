

/* *********************************************************************** *
 * project: org.matsim.*
 * Main_exampleDecentralizedSmartCharger.java
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


import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
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
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import java.util.*;

/**
 * The package DecentralizedSmartCharger guides charging decisions for electric vehicles (EV & PHEV) and 
 * provides an optimized charging schedule for all agents according to their daily plans.
 * The associated charging costs and total emissions are also calculated. 
 *
 * 
 * To use these functions
 * <li>  first create a DecentralizedSmartCharger object within your simulation
 * <li> set its input parameters as shown in the example below
 * 
 * To use the function
 * <li> run() it after the iteration(s) 
 * <li> by calling the different available get methods, you can extract relevant information
 *  
 * Please provide the following folders in the output path to store results locally
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
 * 
 */
public class Main_exampleDecentralizedSmartCharger {
	
		
	public static void main(String[] args) throws IOException {
		
		
		
		/*************
		 * VEHICLES
		 * ************
		 * Provide a list of vehicles with the agent Id and the corresponding vehicle object
		 * LinkedListValueHashMap<Id, Vehicle>
		 * 
		 * You can use the EnergyConsumptionInit class of the package to do this or make your own
		 * To use the EnergyConsumptionInit class, define the percentage of PHEVs, EVs, and combustion engine vehicles
		 * 
		 */
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		
		
		/*************
		 * SIMULLATION VARIABLES
		 * ************
		 * The simulation needs a 
		 * <li>controler
		 * <li>parkingTimesPlugIn in order to retrieve the population
		 * <li>an outputPath to store the output files such as graphs of the loadDistribution or behavior of the agents
		 * <li>config Path
		 * 
		 */
		final Controler controler;
		final ParkingTimesPlugin parkingTimesPlugin;	
		
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\"; //"C:\\Users\\stellas\\Output\\V1G\\";
		
		String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
		
		controler=new Controler(configPath);
		
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
		final VehicleTypeCollector myVehicleTypes = sv.setUp();
		

		
		/**
		 * LP Optimization parameters
		 * - battery buffer for charging (e.g. 0.2=20%, agent will have charged 20% more 
		 * than what he needs before starting the next trip ); 
		 */
		final double bufferBatteryCharge=0.0;
		
		/**
		 * Charging Distribution
		 * - minimum charging length [s]
		 * - time resolution of optimization
		 */
		final double minChargingLength=5*60;//5 minutes
		
		
				
		/*
		 * ****************************
		 * SETUP SIMULATION
		 * ****************************
		 */
		EventHandlerAtStartupAdder eventHandlerAtStartupAdder = new EventHandlerAtStartupAdder();
		
		parkingTimesPlugin = new ParkingTimesPlugin(controler);
		
		eventHandlerAtStartupAdder.addEventHandler(parkingTimesPlugin);
		
		final EnergyConsumptionInit e= new EnergyConsumptionInit(
				phev, ev, combustion);
		
		controler.addControlerListener(e);
				
		controler.addControlerListener(eventHandlerAtStartupAdder);
		
		controler.setOverwriteFiles(true);
		
		controler.addControlerListener(new IterationEndsListener() {
			
			/*
			 * ****************************
			 * at Iteration End
			 * ****************************
			 */	
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					/******************************************
					 * SEtup for Decentralized Smart Charging
					 * *****************************************
					 */
					
					
										
					//initialize DecentralizedSmartCharger
					DecentralizedSmartCharger myDecentralizedSmartCharger = new DecentralizedSmartCharger(
							event.getControler(), //Controler
							parkingTimesPlugin, //ParkingTimesPlugIn
							e.getEnergyConsumptionPlugin(), // EnergyConsumptionPlugIn
							outputPath, // where to save the data
							myVehicleTypes // the defined vehicle types(gas, battery)
							);
					
					//set battery reserve
					myDecentralizedSmartCharger.initializeLP(bufferBatteryCharge);
					
					// set standard charging slot length
					myDecentralizedSmartCharger.initializeChargingSlotDistributor(minChargingLength);
					
					// set LinkedList of vehicles <agentId, vehicle>
					myDecentralizedSmartCharger.setLinkedListValueHashMapVehicles(
							e.getVehicles());
					
					
					/*
					 * HubLinkMapping links linkIds (Id) to Hubs (Integer)
					 * this hubMapping needs to be done individually for every scenario, please write your own class/function here
					 *  - an example is provided in StellasHubMapping and follows the following format
					 *  which creates a HubLinkMapping hubLinkMapping=new HubLinkMapping(int numberOfHubs);
					 *  hubLinkMapping.addMapping(linkId, hubNumber);
					 */
					StellasHubMapping setHubLinkMapping= new StellasHubMapping(controler);
					final HubLinkMapping hubLinkMapping=setHubLinkMapping.mapHubs();
					
					/*
					 * Network  - Electric Grid Information
					 * 
					 * - distribution of free load [W] available for charging over the day (deterministicHubLoadDistribution)
					 * this is given in form of a LinkedListValueHashMap, where Integer corresponds to a hub and 
					 * the Schedule includes LoadDistributionIntervals which represent the free load 
					 * THe LoadDistributionIntervals indicate 
					 * <li> a time interval: start second, end second 
					 * <li>PolynomialFunction indicating the free Watts over the time interval
					 * <li> an optimality boolean(true, if free load is positive=electricity for charging available and false if not)
					 * </br>
					 * </br>
					 * - pricing (pricingHubDistribution)
					 * is also given as LinkedListValueHashMap analogous to the determisticiHubLoadDIstribution
					 *  where Integer corresponds to a hub and
					 * the Schedule includes LoadDistributionIntervals which represent the price per second 
					 * of charging at a 3500W connection over the day
					 * </br>
					 * !!!!!!!!!!!!!!!!!!!!!!
					 * IMPORTANT 
					 * !!!!!!!!!!!!!!!!!!!!!!
					 * 1) the day can be split into as many time intervals as you wish, however 
					 * positive and negative determisticLoad Intervals should be different intervals (either optimal or suboptimal intervals)
					 * 2) Also the pricingHubDistribution and the deterministicHubLoadDistribution 
					 * Should have the SAME time intervals (start and end)
					 * 
					 * 
					 * DetermisticLoadAndPricingCollector is an example how you can define 
					 * these determisticLoad curves for your scenario
					 * 
					 */
					DetermisticLoadAndPricingCollector dlpc= new DetermisticLoadAndPricingCollector();
					
					final LinkedListValueHashMap<Integer, Schedule> deterministicHubLoadDistribution
						= dlpc.getDeterminisitcHubLoad();
					
					final LinkedListValueHashMap<Integer, Schedule> pricingHubDistribution
						= dlpc.getDeterminisitcPriceDistribution();
					
					myDecentralizedSmartCharger.initializeHubLoadDistributionReader(
							hubLinkMapping, 
							deterministicHubLoadDistribution,							
							pricingHubDistribution							
							);
					
					/***********************
					 * RUN
					 * **********************
					 */
					
					myDecentralizedSmartCharger.run();
					
					
					/***********************
					 * Examples how to use
					 * **********************
					 */
					for(Id id: controler.getPopulation().getPersons().keySet()){
						//CHRONOLOGICAL SCHEDULES OF AGENTS where each schedule has parking and driving intervals
						LinkedListValueHashMap<Id, Schedule> agentPDSchedules= 
							myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules();
						
						System.out.println("parking and driving schedule agent "+id.toString());
						agentPDSchedules.getValue(id).printSchedule();
						
						//CHARGING COSTS
						LinkedListValueHashMap<Id, Double> agentChargingCosts= 
							myDecentralizedSmartCharger.getChargingCostsForAgents();
						System.out.println("charging cost of agent "+id.toString() 
								+ " "+agentChargingCosts.getValue(id));
												
						
						//CHARGING SCHEDULES FOR EVERY AGENT
						LinkedListValueHashMap<Id, Schedule> agentSchedules= 
							myDecentralizedSmartCharger.getAllAgentChargingSchedules();
						
						System.out.println("charging Schedule agent "+id.toString());
						agentSchedules.getValue(id).printSchedule();
						
						
						//LIST OF AGENTS WITH EV WHERE LP FAILED, i.e. where battery swap would be necessary
						LinkedList<Id> agentsWithEVFailure = 
							myDecentralizedSmartCharger.getIdsOfEVAgentsWithFailedOptimization();
						
						
						// GET ALL IDs OF AGENTS WITH EV, PHEV or Combustion engine car
						LinkedList<Id> agentsWithEV = myDecentralizedSmartCharger.getAllAgentsWithEV();
						LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
						LinkedList<Id> agentsWithConventionalCar = myDecentralizedSmartCharger.getAllAgentsWithCombustionVehicle();
						
						
						// DETAILED DATA PER AGENT
						if(agentsWithEV.isEmpty()==false){
							
							// DRIVING CONSUMPTION [Joules]
							System.out.println("Total consumption from battery [joules]" 
									+ myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromBattery(id));
							
							
							// TOTAL DRIVING CONSUMPTION NOT FROM ELECTRIC ENGINE[Joules]
							// either from combustion engine or through potential battery swap for EVs
							System.out.println("Total consumption from engine [joules]" +
									myDecentralizedSmartCharger.getTotalDrivingConsumptionOfAgentFromOtherSources(id));
								
						}
						
						//TOTAL EMISSIONS
						System.out.println("Total emissions [kg]" +
								myDecentralizedSmartCharger.getTotalEmissions());
						
					}
					
					
					/***********************
					 * END
					 * **********************
					 */
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
