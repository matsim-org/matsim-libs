

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
 * This package guides charging decisions for electric vehicles (EV & PHEV) and 
 * provides an optimized charging schedule for all agents and the associated charging costs. 
 * It also calculates the total emissions produced by the vehicles.
 * 
 * To set up using these functions
 * - first create a DecentralizedSmartCharger object within your simulation
 * - set its input parameters as shown in the example below
 * 
 * 
 * To use the function
 * - run() it after the iteration(s) 
 * 
 * 
 */
public class Main_exampleDecentralizedSmartCharger {
	
		
	public static void main(String[] args) throws IOException {
		
		
		
		/**
		 * Define the percentage of PHEVs, EVs, and combustion engine vehicles
		 * this serves as input to EnergyConsumptionInit to create a list of vehicles
		 */
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		/**
		 * INPUT PARAMETERS
		 */
		
		/*
		 * Simulation variables: 
		 * - Controler of the simulation in which the charging optimization is embedded
		 * - ParkingTimesPlugin
		 * 
		 */
		final Controler controler;
		final ParkingTimesPlugin parkingTimesPlugin;	
		
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\"; //"C:\\Users\\stellas\\Output\\V1G\\";
		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		
		controler=new Controler(configPath);
		
		
		SetUpVehicleCollector sv= new SetUpVehicleCollector();
		final VehicleTypeCollector myVehicleTypes = sv.setUp();
		

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
			
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					/*
					 * HubLinkMapping links linkIds (Id) to Hubs (Integer)
					 * this hubMapping needs to be done individually for every scenario, please write your own class/function here
					 * 
					 * 
					 */
					StellasHubMapping setHubLinkMapping= new StellasHubMapping(controler);
					final HubLinkMapping hubLinkMapping=setHubLinkMapping.mapHubs();
					
					
					/******************************************
					 * for Decentralized Smart Charging
					 * *****************************************
					 */
					
					//initialize parameters
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
					
					// initialize HubLoadReader
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
					//CHRONOLOGICAL SCHEDULES OF AGENTS where each schedule has parking and driving intervals
					LinkedListValueHashMap<Id, Schedule> agentSchedules= 
						myDecentralizedSmartCharger.getAllAgentChargingSchedules();
					
					
					//CHARGING COSTS
					LinkedListValueHashMap<Id, Double> agentChargingCosts= 
						myDecentralizedSmartCharger.getChargingCostsForAgents();
					
					
					//CHARGING SCHEDULES FOR EVERY AGENT
					for (Id id: controler.getPopulation().getPersons().keySet()){
						Schedule agentSchedule= myDecentralizedSmartCharger.getAgentChargingSchedule(id);//.printSchedule();
					}
					
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
						Id id= agentsWithEV.get(0);
						myDecentralizedSmartCharger.getAgentChargingSchedule(id).printSchedule();
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
					
					
					
					/***********************
					 * END
					 * 
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
