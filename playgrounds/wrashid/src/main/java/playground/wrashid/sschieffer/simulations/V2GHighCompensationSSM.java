
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
 * This V2G simulation tests the V2G behavior for extremely high 
 * V2G regulation compensaion of 1CHF/kWh
 * for simulation SSA
 * 
 * @author Stella
 *
 */
public class V2GHighCompensationSSM {
	
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		
		/*************
		 * SET UP STANDARD Decentralized Smart Charging simulation 
		 * *************/
		
		final double electrification= 1.0; 
		// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
		final double ev=0.1; 
		
		
		//final String outputPath="D:/ETH/MasterThesis/TestOutput/";
		final String outputPath="/cluster/home/baug/stellas/Runs/HighCompensationSSA/";
		//String configPath="test/scenarios/berlin/config.xml";
		String configPath="/cluster/home/baug/stellas/Runs/berlinInput/config.xml";
		//String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_berlin16000.txt";
		String freeLoadTxt="/cluster/home/baug/stellas/Runs/berlinInput/freeLoad15minBinSec_berlin16000.txt";
		//String stochasticGeneral= "test/input/playground/wrashid/sschieffer/stochasticRandom+-5000.txt";
		String stochasticGeneral= "/cluster/home/baug/stellas/Runs/berlinInput/stochasticRandom+-5000.txt";
		
		double kWHEV =16;
		double kWHPHEV =16;
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
		
		ArrayList<HubInfoDeterministic> myHubInfo = new ArrayList<HubInfoDeterministic>(0);
		myHubInfo.add(new HubInfoDeterministic(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
		
							
		ArrayList<HubInfoStochastic> myStochasticHubInfo = new ArrayList<HubInfoStochastic>(0);
		
		HubInfoStochastic hubInfo1= new HubInfoStochastic(1, stochasticGeneral);
		
		
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
		
		final double xPercentDownUp=1.0;
		final double xPercentDown=1.0-xPercentDownUp;
		
		
		/*
		 * if you also want to include information about vehicle and other hub sources 
		 * <li> create the objects (ArrayList<GeneralSource>  stochasticHubLoadTxt=null; HashMap <Id, String> stochasticVehicleLoad= new HashMap <Id, String> ();)
		 * <li>use the following constructor: 
		 * myStochasticHubInfo.add(new HubInfo(1, stochasticGeneral, stochasticHubLoadTxt, stochasticVehicleLoad));
		 */
		
				
		double compensationPerKWHRegulationUp=1.0;
		double compensationPerKWHRegulationDown=1.0;
		double compensationPERKWHFeedInVehicle=1.0;
		
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
