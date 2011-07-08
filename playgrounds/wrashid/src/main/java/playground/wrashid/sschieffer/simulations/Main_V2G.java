
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
package playground.wrashid.sschieffer.simulations;

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
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DSC.DecentralizedChargingSimulation;
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.GeneralSource;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoDeterministic;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoStochastic;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.NetworkTopology.StellasHubMapping;
import playground.wrashid.sschieffer.V2G.StochasticLoadCollector;

import java.util.*;


/**
 * This class highlights how to use V2G functions of this package:
 * 
 * V2G can only follow after the Decentralized Smart Charger has been run
 * since its based on the charging schedules planned by the Decentralized Smart Charger
 * 
 * For all specified stochastic loads the V2G procedure will then check if rescheduling is possible 
 * and if the utility of the agent can be increased by rescheduling.
 * 
 * 
 * PLease provide the following folders and sub-folders in the output path to store results locally
 * 
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
public class Main_V2G {
	
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		
		/*************
		 * SET UP STANDARD Decentralized Smart Charging simulation 
		 * *************/
		
		//percent of population with electric vehicles
		final double electrification= 1.0; 
		// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
		final double ev=0.0; 
		
		//final String outputPath="/cluster/home/baug/stellas/Runs/24hrV2G20000Plans15Min/Results";
		final String outputPath="D:/ETH/MasterThesis/TestOutput/";
		//String configPath="test/scenarios/berlin/config.xml";
		String configPath="test/input/playground/wrashid/sschieffer/config.xml";// 100 agents
		
		double kWHEV =24;
		double kWHPHEV =24;
		boolean gasHigh = false;
		
		final double standardChargingLength=15.0*DecentralizedSmartCharger.SECONDSPERMIN;
		final double bufferBatteryCharge=0.0;
		
		int numberOfHubsInX=1;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
		
		double standardConnectionWatt=3500;
		
		/*
		 * CHARGING PRICES
		 */
		double priceMaxPerkWh=0.11;
		double priceMinPerkWh=0.07;
		
		String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_berlin16000.txt";
		
		ArrayList<HubInfoDeterministic> myHubInfo = new ArrayList<HubInfoDeterministic>(0);
		myHubInfo.add(new HubInfoDeterministic(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
		
		/*
		 * ************************
		 * Stochastic Sources
		 * <p>
		 * the information about the stochastic load distribution over the day is given in Watt. 
		 * It can be given as a txt file with 96 bins (15 min intervals) 
		 * or with discrete load intervals which reflects the intermittent character of some loads better (ArrayList<LoadDistributionIntervals>)</p>
		 * 
		 *  * the sources are given in [Watt]
		 * <li> negative values mean, that the source needs energy --> regulation up
		 * <li> positive values mean, that the source has too much energy--> regulation down
		 * 
		 * StochasticLoadSources can be defined for
		 * <li> general hub stochastic Load 
		 * <li> for vehicles (i.e. solar roof) given as HashMap with AgentId and String for (HashMap <Id, String> stochasticVehicleLoad)
		 * OR HashMap <Id, ArrayList<LoadDIstrubitionIntervals>>
		 * <li> local sources for hubs (wind turbine, etc) given in 96 bin txt file with 97 data points (String stochasticHubLoadTxt)
			INPUT AS
			<li>TEXT FILE: 
			The example below only adds the generalStochastic load in form of a txt file to Hub1
			<li> Discrete LOAD INTERVALS:
			ArrayList<LoadDistributionInterval> discreteLoadIntervals= new ArrayList<LoadDistributionInterval>(0);
			// the following line adds a loadInterval between seconds 0- 30000 of 100000W to the load Distribution
			discreteLoadIntervals.add(new LoadDistributionInterval(0, 30000, 100000));
			hubInfo1.setStochasticVehicleSourcesIntervals(discreteLoadIntervals);
		 */			
					
		ArrayList<HubInfoStochastic> myStochasticHubInfo = new ArrayList<HubInfoStochastic>(0);
		String stochasticGeneral= "test/input/playground/wrashid/sschieffer/stochasticRandom+-5000.txt";
		HubInfoStochastic hubInfo1= new HubInfoStochastic(1, stochasticGeneral);
		
		/*
		 * ADDING A HUB LOAD
		 * - by generating a general hub source 
		 */
		ArrayList<GeneralSource> generalHubSource= new ArrayList<GeneralSource>(0);
		ArrayList<LoadDistributionInterval> generalHubLoad= new ArrayList<LoadDistributionInterval>(0);
		generalHubLoad.add(new LoadDistributionInterval(3500, 7000, 5000));
		generalHubSource.add(new GeneralSource(
				generalHubLoad,
				new IdImpl(1),				
				"random load", 
				0.005) );
		hubInfo1.setStochasticGeneralSources(generalHubSource);
		
		/*
		 * ADDING A VEHICLE LOAD
		 */
		HashMap <Id, ArrayList<LoadDistributionInterval>> vehicleLoadHashMap = new HashMap<Id, ArrayList<LoadDistributionInterval>>();
		ArrayList<LoadDistributionInterval> vehicleLoad= new ArrayList<LoadDistributionInterval>(0);
		vehicleLoad.add(new LoadDistributionInterval(3500, 7000, 3500));
		vehicleLoadHashMap.put(new IdImpl(1), vehicleLoad);
		hubInfo1.setStochasticVehicleSourcesIntervals(vehicleLoadHashMap);
		
		// add hubInfo to stochastic load
		myStochasticHubInfo.add(hubInfo1);
		
				
		//SETUP UP VARIABLES
		DecentralizedChargingSimulation mySimulation= new DecentralizedChargingSimulation(
				configPath, 
				outputPath, 
				electrification, ev,
				bufferBatteryCharge,
				standardChargingLength,
				myMappingClass,
				myHubInfo,
				false, // indicate if you want graph output for every agent to visualize the SOC over the day
				kWHEV,kWHPHEV, gasHigh,
				standardConnectionWatt
				);
		
		/******************************************
		 * SETUP V2G
		 * *****************************************
		
		/**
		 * SPECIFY WHAT PERCENTAGE OF THE POPULATION PROVIDES V2G
		 * <li> no V2G
		 * <li> only down (only charging)
		 * <li> up and down (charging and discharging)
		 * 
		 * i.e. 0/0.5/0.5
		 * 50% do only down, 50% do up and down
		 */
		
		final double xPercentDown=0.0;
		final double xPercentDownUp=1.0;
		
			
		// compensation for V2Gs for regulation up, down or feed in
		double compensationPerKWHRegulationUp=0.1;
		double compensationPerKWHRegulationDown=0.005;
		double compensationPERKWHFeedInVehicle=0.005;
		
		mySimulation.setUpV2G(				
				xPercentDown,
				xPercentDownUp,
				new StochasticLoadCollector(mySimulation, myStochasticHubInfo ),
				compensationPerKWHRegulationUp,
				compensationPerKWHRegulationDown,
				compensationPERKWHFeedInVehicle);
	
		mySimulation.controler.run();
		
		/*********************
		 * Example how to Use V2G
		 *********************/
		/*
		 * V2G Of VEHICLES
		 */
		// REVENUES FROM V2G
		System.out.println("average revenue from V2G for agents: "+mySimulation.getAverageRevenueV2GPerAgent());
		System.out.println("average revenue from V2G for EV agents: "+mySimulation.getAverageRevenueV2GPerEV());
		System.out.println("average revenue from V2G for PHEV agents: "+mySimulation.getAverageRevenueV2GPerPHEV());
		
		
		//JOULES PROVIDED IN REGULATION UP OR DOWN
		System.out.println("total joules from V2G for regulation up: "+mySimulation.getTotalJoulesV2GRegulationUp());
		System.out.println("total joules from V2G for regulation up from EVs: "+mySimulation.getTotalJoulesV2GRegulationUpEV());
		System.out.println("total joules from V2G for regulation up from PHEVs: "+mySimulation.getTotalJoulesV2GRegulationUpPHEV());
		
		
		//JOULES PROVIDED IN REGULATION UP OR DOWN
		System.out.println("total joules from V2G for regulation down: "+mySimulation.getTotalJoulesV2GRegulationDown());
		System.out.println("total joules from V2G for regulation down from EVs: "+mySimulation.getTotalJoulesV2GRegulationDownEV());
		System.out.println("total joules from V2G for regulation down from PHEVs: "+mySimulation.getTotalJoulesV2GRegulationDownPHEV());
		
		
		/*
		 * VEHICLES AS SOURCES
		 */
		//FEED IN from vehicle local production 
		System.out.println("average revenue from feed in for all agents: "+mySimulation.getAverageRevenueFeedInAllAgents());
		System.out.println("average revenue from feed in for EV agents: "+mySimulation.getAverageRevenueFeedInForEVs());
		System.out.println("average revenue from feed in for PHEV agents: "+mySimulation.getAverageRevenueFeedInForPHEVs());
		
		//ENERGY FEEDIN VEHICLES
		System.out.println("total energy feed in for all agents: "+mySimulation.getTotalJoulesFromFeedIn());
		System.out.println("total energy feed in for EV agents: "+mySimulation.getTotalJoulesFromFeedInfromEVs());
		System.out.println("total energy feed in for PHEV agents: "+mySimulation.getTotalJoulesFromFeedInFromPHEVs());
		
		
		//ENERGY EXTRA COSTS From extra charging
		System.out.println("average extra costs for extra vehicle charging all agents: "+mySimulation.getAverageExtraChargingAllVehicles());
		System.out.println("average extra costs for extra vehicle charging EV: "+mySimulation.getAverageExtraChargingAllEVs());
		System.out.println("average extra costs for extra vehicle charging PHEV: "+mySimulation.getAverageExtraChargingAllPHEVs());
		
		//Extra joules charged from battery for additional stochastic consumption 
		
		System.out.println("average joules taken from battery for extra stochastic consumption: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionAllAgents());
		System.out.println("average joules taken from battery for extra stochastic consumption EV: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionEV());
		System.out.println("average joules taken from battery for extra stochastic consumption PHEV: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionPHEV());
		
		//joules saved by local production
		System.out.println("average joules saved by local stochastic production all agents: "+mySimulation.getAverageJouleSavedByLocalV2GProductionAllAgents());
		System.out.println("average joules saved by local stochastic production EV: "+mySimulation.getAverageJouleSavedByLocalV2GProductionEV());
		System.out.println("average joules saved by local stochastic production PHEV: "+mySimulation.getAverageJouleSavedByLocalV2GProductionPHEV());
		
		/*
		 * HUBSOURCES
		 */
		System.out.println("average revenue from feed in for hub sources: "+mySimulation.getAverageFeedInRevenueHubSources());
		System.out.println("average extra charging cost for hub sources: "+mySimulation.getAverageExtraChargingHubSources());
		System.out.println("total energy for hub sources: "+mySimulation.getTotalJoulesFeedInHubSources());
		
		}
		
		

}
