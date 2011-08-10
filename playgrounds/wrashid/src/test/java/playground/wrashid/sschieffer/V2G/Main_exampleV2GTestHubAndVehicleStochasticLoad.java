
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
package playground.wrashid.sschieffer.V2G;

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
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.DetermisticLoadPricingCollector;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.GeneralSource;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoDeterministic;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoStochastic;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.NetworkTopology.StellasHubMapping;
import playground.wrashid.sschieffer.V2G.StochasticLoadCollector;

import java.util.*;

import junit.framework.TestCase;


/**
 * checks on highest level the values for 
 * <li> VEHICLE STOCHATIC LOAD: feed in, revenue, joules saved by local production,...
 * <li> HUB STOCHASTIC SOURCE: feed in, revenue
 * 
 * </br>
 * 
 * In the system we have 
 * <li> one hub source with 5000W between 3500-7000s 
 * <li> one vehicle load with 3500W between 3500-7000s 
 * 
 * @author Stella
 *
 */
public class Main_exampleV2GTestHubAndVehicleStochasticLoad extends TestCase{
	
	private static DecentralizedChargingSimulation mySimulation;
	
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		
	}
	
	public void testRun() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		/*************
		 * SET UP STANDARD Decentralized Smart Charging simulation 
		 * *************/
		
		final double electrification= 1.0;
		// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
		final double ev=1.0; 
		
		final String outputPath="test/output/";
		//String configPath="test/input/playground/wrashid/sschieffer/config.xml";// 100 agents
		String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
		
		final double bufferBatteryCharge=0.0;
		final double standardChargingLength=15.0*DecentralizedSmartCharger.SECONDSPERMIN;
		
		int numberOfHubsInX=1;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
		
		double priceMaxPerkWh=0.11;// http://www.ekz.ch/internet/ekz/de/privatkunden/Tarife_neu/Tarife_Mixstrom.html
		double priceMinPerkWh=0.07;
		
		String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_berlin16000.txt";
		ArrayList<HubInfoDeterministic> myHubInfo = new ArrayList<HubInfoDeterministic>(0);
		myHubInfo.add(new HubInfoDeterministic(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
		
		double kWHEV =24;
		double kWHPHEV =24;
		boolean gasHigh = false;
		mySimulation= new DecentralizedChargingSimulation(
				configPath, 
				outputPath, 
				electrification, ev,
				bufferBatteryCharge,
				standardChargingLength,
				myMappingClass,
				myHubInfo,
				false, // indicate if you want graph output for every agent to visualize the SOC over the day
				kWHEV,kWHPHEV, gasHigh,
				3500
				);
		
		ArrayList<HubInfoStochastic> myStochasticHubInfo = new ArrayList<HubInfoStochastic>(0);
		String stochasticGeneral= "test/input/playground/wrashid/sschieffer/stochasticRandom+-5000.txt";
		HubInfoStochastic hubInfo1= new HubInfoStochastic(1, stochasticGeneral);
		
		/*
		 * ADDING A HUB LOAD
		 * - by generating a general hub source 
		 */
		ArrayList<GeneralSource> generalHubSource= new ArrayList<GeneralSource>(0);
		ArrayList<LoadDistributionInterval> generalHubLoad= new ArrayList<LoadDistributionInterval>(0);
		generalHubLoad.add(new LoadDistributionInterval(3500, 7000, 5000));//17.5 million Joules
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
		vehicleLoad.add(new LoadDistributionInterval(3500, 4400, 1000));//1000W for 900 seconds - expect 900.000 Joules saved energy
		vehicleLoadHashMap.put(new IdImpl(1), vehicleLoad);
		hubInfo1.setStochasticVehicleSourcesIntervals(vehicleLoadHashMap);
		
		// add hubInfo to stochastic load
		myStochasticHubInfo.add(hubInfo1);
		
		
		double xPercentDown=0.0;
		double xPercentDownUp=0.0;
		
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
		
		resultsCheckNoReg();
	
	}
	
	
	
	
	public static void resultsCheckNoReg(){
		/*********************
		 * Check V2G
		 *********************/
		/*
		 * VEHICLES AS SOURCES
		 */
		//FEED IN from vehicle local production 
		System.out.println("average revenue from feed in for all agents: "+mySimulation.getAverageRevenueFeedInAllAgents());
		System.out.println("average revenue from feed in for EV agents: "+mySimulation.getAverageRevenueFeedInForEVs());
		System.out.println("average revenue from feed in for PHEV agents: "+mySimulation.getAverageRevenueFeedInForPHEVs());
		assertEquals(mySimulation.getAverageRevenueFeedInAllAgents(), 0.0);
		assertEquals(mySimulation.getAverageRevenueFeedInForEVs(), 0.0);
		assertEquals(mySimulation.getAverageRevenueFeedInForPHEVs(), 0.0);
		
		//ENERGY FEEDIN VEHICLES
		System.out.println("total energy feed in for all agents: "+mySimulation.getTotalJoulesFromFeedIn());
		System.out.println("total energy feed in for EV agents: "+mySimulation.getTotalJoulesFromFeedInfromEVs());
		System.out.println("total energy feed in for PHEV agents: "+mySimulation.getTotalJoulesFromFeedInFromPHEVs());
		assertEquals(mySimulation.getTotalJoulesFromFeedIn(), 0.0);
		assertEquals(mySimulation.getTotalJoulesFromFeedInfromEVs(), 0.0);
		assertEquals(mySimulation.getTotalJoulesFromFeedInFromPHEVs(), 0.0);
		
		//ENERGY EXTRA COSTS From extra charging
		System.out.println("average extra costs for extra vehicle charging all agents: "+mySimulation.getAverageExtraChargingAllVehicles());
		System.out.println("average extra costs for extra vehicle charging EV: "+mySimulation.getAverageExtraChargingAllEVs());
		System.out.println("average extra costs for extra vehicle charging PHEV: "+mySimulation.getAverageExtraChargingAllPHEVs());
		assertEquals(mySimulation.getAverageExtraChargingAllVehicles(), 0.0);
		assertEquals(mySimulation.getAverageExtraChargingAllEVs(), 0.0);
		assertEquals(mySimulation.getAverageExtraChargingAllPHEVs(), 0.0);
		
		//Extra joules charged from battery for additional stochastic consumption 
		System.out.println("average joules taken from battery for extra stochastic consumption: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionAllAgents());
		System.out.println("average joules taken from battery for extra stochastic consumption EV: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionEV());
		System.out.println("average joules taken from battery for extra stochastic consumption PHEV: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionPHEV());
		assertEquals(mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionAllAgents(), 0.0);
		assertEquals(mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionEV(), 0.0);
		assertEquals(mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionPHEV(), 0.0);
		
		
		//joules saved by local production
		System.out.println("average joules saved by local stochastic production all agents: "+mySimulation.getAverageJouleSavedByLocalV2GProductionAllAgents());
		System.out.println("average joules saved by local stochastic production EV: "+mySimulation.getAverageJouleSavedByLocalV2GProductionEV());
		System.out.println("average joules saved by local stochastic production PHEV: "+mySimulation.getAverageJouleSavedByLocalV2GProductionPHEV());
		assertEquals(mySimulation.getAverageJouleSavedByLocalV2GProductionAllAgents(), 900000.0);
		assertEquals(mySimulation.getAverageJouleSavedByLocalV2GProductionEV(), 900000.0);
		assertEquals(mySimulation.getAverageJouleSavedByLocalV2GProductionPHEV(), 0.0);
		
		/*
		 * HUBSOURCES
		 */
		System.out.println("average revenue from feed in for hub sources: "+mySimulation.getAverageFeedInRevenueHubSources());
		System.out.println("average extra charging cost for hub sources: "+mySimulation.getAverageExtraChargingHubSources());
		System.out.println("total energy for hub sources: "+mySimulation.getTotalJoulesFeedInHubSources());
		assertEquals(mySimulation.getAverageFeedInRevenueHubSources(), 0.024305555555555552);
		assertEquals(mySimulation.getAverageExtraChargingHubSources(), 0.0);
		assertEquals(mySimulation.getTotalJoulesFeedInHubSources(),1.75E7);
	}

}
