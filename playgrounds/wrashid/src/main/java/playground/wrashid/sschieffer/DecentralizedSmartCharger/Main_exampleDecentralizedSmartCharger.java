

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
	
		
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {		
		
		/*************
		 * SIMULLATION VARIABLES
		 * ************
		 * The simulation needs 
		 * <li>	% of phevs, evs and combustion engine cars in the simulation * 
		 * <li> outputPath to store the output files such as graphs of the loadDistribution or behavior of the agents
		 * <li> config Path
		 * <li> buffer for battery charge
		 * <li> minimum charging length
		 * <li> MappingClass object which will map linkId to Hubs for scenario
		 */
		final double phev=1.0;
		final double ev=0.0;
		final double combustion=0.0;
		
		final String outputPath="D:\\ETH\\MasterThesis\\Output\\";		
		String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
				
		/**
		 * LP Optimization parameters
		 * - battery buffer for charging (e.g. 0.2=20%, agent will have charged 20% more 
		 * than what he needs before starting the next trip ); 
		 */
		final double bufferBatteryCharge=0.0;
		
		/**
		 * Charging Distribution
		 * - minimum charging length [s] = time resolution of optimization
		 */
		final double minChargingLength=15*60;
		
		/**
		 * define mapping class that shall be used to map the 
		 * linkdId to the hubs in the DecentralizedSmartCharger
		 */
		StellasHubMapping myMappingClass= new StellasHubMapping();
		
		StellasResidentialDetermisticLoadPricingCollector loadPricingCollector= 
			new StellasResidentialDetermisticLoadPricingCollector();
		
		DecentralizedChargingSimulation mySimulation= new DecentralizedChargingSimulation(configPath, 
				outputPath, 
				phev, ev, combustion,
				bufferBatteryCharge,
				minChargingLength,
				myMappingClass,
				loadPricingCollector
				);
		mySimulation.addControlerListenerDecentralizedCharging();		
		mySimulation.controler.run();
		
		/***********************
		 * Examples how to use
		 * The decentralized smart charger runs after the iteration
		 * after the run, the results can be obtained as demonstrated in the example below
		 * **********************
		 */
		
		
		for(Id id: DecentralizedChargingSimulation.controler.getPopulation().getPersons().keySet()){
			//CHRONOLOGICAL SCHEDULES OF AGENTS where each schedule has parking and driving intervals
			LinkedListValueHashMap<Id, Schedule> agentPDSchedules= 
				mySimulation.getAllAgentParkingAndDrivingSchedules();
			
			System.out.println("parking and driving schedule agent "+id.toString());
			agentPDSchedules.getValue(id).printSchedule();
			
			//CHARGING COSTS
			LinkedListValueHashMap<Id, Double> agentChargingCosts= 
				mySimulation.getChargingCostsForAgents();
			System.out.println("charging cost of agent "+id.toString() 
					+ " "+agentChargingCosts.getValue(id));
									
			
			//CHARGING SCHEDULES FOR EVERY AGENT
			LinkedListValueHashMap<Id, Schedule> agentSchedules= 
				mySimulation.getAllAgentChargingSchedules();
			
			System.out.println("charging Schedule agent "+id.toString());
			agentSchedules.getValue(id).printSchedule();
			
			
			//LIST OF AGENTS WITH EV WHERE LP FAILED, i.e. where battery swap would be necessary
			LinkedList<Id> agentsWithEVFailure = 
				mySimulation.getListOfIdsOfEVAgentsWithFailedOptimization();
			
			
			// GET ALL IDs OF AGENTS WITH EV, PHEV or Combustion engine car
			LinkedList<Id> agentsWithEV = mySimulation.getListOfAllEVAgents();
			LinkedList<Id> agentsWithPHEV = mySimulation.getListOfAllPHEVAgents();
			LinkedList<Id> agentsWithConventionalCar = mySimulation.getListOfAllCombustionAgents();
			
			
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
