

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
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.HubInfo;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.StellasHubMapping;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.scenarios.DetermisticLoadPricingCollector;

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
	
		
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {		
		
		/*************
		 * SIMULLATION VARIABLES
		 * ************
		 * The simulation needs 
		 * <li>	% of phevs, evs in the simulation * 
		 * <li> outputPath to store the output files such as graphs of the loadDistribution or behavior of the agents
		 * <li> config Path
		 * <li> buffer for battery charge
		 * <li> standard charging length
		 * <li> MappingClass object which will map linkId to Hubs for scenario
		
		 */
		//electrification 1.0=100% of cars are evs or phevs
		final double electrification= 1.0; 
		// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
		final double ev=1.0; 
		
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\Testing\\";		
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";
		
		/**
		 * define the hubs and their input, for each hub create a HubInfo Object and add it to the ArrayList<HubInfo> myHubInfo
		 * for multiple hubs, you can add multiple entries to myHubInfo
		 */
		double priceMaxPerkWh=0.40;
		double priceMinPerkWh=0.25;
		String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_1000.txt";
		ArrayList<HubInfo> myHubInfo = new ArrayList<HubInfo>(0);
		myHubInfo.add(new HubInfo(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
		
		/**
		 * define mapping class that shall be used to map the 
		 * linkdId to the hubs in the DecentralizedSmartCharger
		 * 
		 */
		int numberOfHubsInX=1;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
		
		
		/**
		 * LP Optimization parameters
		 * - battery buffer for charging (e.g. 0.2=20%, agent will have charged 20% more 
		 * than what he needs before starting the next trip ); 
		 */
		final double bufferBatteryCharge=0.0;
		
		/**
		 * Charging Distribution
		 * - standard charging length [s] = time resolution 
		 */
		final double standardChargingLength=15*60;
		
			
		DecentralizedChargingSimulation mySimulation= new DecentralizedChargingSimulation(
				configPath, 
				outputPath, 
				electrification, ev,
				bufferBatteryCharge,
				standardChargingLength,
				myMappingClass,
				myHubInfo,
				false // indicate if you want graph output for every agent to visualize the SOC over the day
				);
		mySimulation.addControlerListenerDecentralizedCharging();		
		mySimulation.controler.run();
		
		
		
		/***********************
		 * Examples how to use
		 * The decentralized smart charger runs after the iteration
		 * after the run, the results can be obtained as demonstrated in the example below
		 * **********************
		 */
		for(Id id: mySimulation.mySmartCharger.vehicles.getKeySet()){
			
			//CHRONOLOGICAL SCHEDULES OF AGENTS where each schedule has parking and driving intervals
			HashMap<Id, Schedule> agentPDSchedules= 
				mySimulation.getAllAgentParkingAndDrivingSchedules();
			
			System.out.println("parking and driving schedule agent "+id.toString());
			agentPDSchedules.get(id).printSchedule();
			
			// VISUALIZE ALL AGENT PLANS AND SAVE THEM IN FOLDER - outputPath+ "DecentralizedCharger\\agentPlans\\"+ id.toString()+"_dayPlan.png"
			mySimulation.visualizeAllAgentPlans();
			mySimulation.visualizeAgentPlans(id);
			
			
			//CHARGING COSTS
			HashMap<Id, Double> agentChargingCosts= mySimulation.getChargingCostsForAgents();
			System.out.println("charging cost of agent "+id.toString() 	+ " "+agentChargingCosts.get(id));
								
			//CHARGING SCHEDULES FOR EVERY AGENT
			HashMap<Id, Schedule> agentSchedules= 
				mySimulation.getAllAgentChargingSchedules();
			
			System.out.println("charging Schedule agent "+id.toString());
			agentSchedules.get(id).printSchedule();
			
			
			//LIST OF AGENTS WITH EV WHERE LP FAILED, i.e. where battery swap would be necessary
			LinkedList<Id> agentsWithEVFailure = 
				mySimulation.getListOfIdsOfEVAgentsWithFailedOptimization();
			
			
			// GET ALL IDs OF AGENTS WITH EV, PHEV or Combustion engine car
			LinkedList<Id> agentsWithEV = mySimulation.getListOfAllEVAgents();
			LinkedList<Id> agentsWithPHEV = mySimulation.getListOfAllPHEVAgents();			
			
			// DETAILED DATA PER AGENT
			if(agentsWithEV.isEmpty()==false){
				
				// DRIVING CONSUMPTION [Joules]
				System.out.println("Total consumption from battery [joules]" 
						+ mySimulation.getTotalDrivingConsumptionOfAgentFromBattery(id));
				
				
				// TOTAL DRIVING CONSUMPTION NOT FROM ELECTRIC ENGINE[Joules]
				// either from combustion engine or through potential battery swap for EVs
				System.out.println("Total consumption from engine [joules]" +
						mySimulation.getTotalDrivingConsumptionOfAgentFromOtherSources(id));
					
			}
			
			//TOTAL EMISSIONS
			System.out.println("Total emissions [kg]" +
					mySimulation.getTotalEmissions());
			
		}
	}
	
	
	

}
