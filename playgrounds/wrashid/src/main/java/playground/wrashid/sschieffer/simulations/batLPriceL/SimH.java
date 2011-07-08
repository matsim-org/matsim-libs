
/* *********************************************************************** *
 * project: org.matsim.*
 * Simulation_1.java
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
package playground.wrashid.sschieffer.simulations.batLPriceL;

import java.io.IOException;


import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;

import playground.wrashid.sschieffer.DSC.DecentralizedChargingSimulation;
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoDeterministic;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoStochastic;
import playground.wrashid.sschieffer.SetUp.NetworkTopology.StellasHubMapping;
import playground.wrashid.sschieffer.V2G.StochasticLoadCollector;

import java.util.*;


/**
 *h	0	0	0.9	0.33

 *price of gas US prices : S
 *battery size : S
 *
 * @author Stella
 *
 */
public class SimH{
	
	public static void main(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		
		final double electrification= 1.0; 
		
		final String outputPath="/cluster/home/baug/stellas/Runs/LL/SimLLH/";
		//final String outputPath="D:\\ETH\\MasterThesis\\Output\\Runs\\Simulation1\\";		
		String configPath="/cluster/home/baug/stellas/Runs/berlinInput/config.xml";
		String freeLoadTxt="/cluster/home/baug/stellas/Runs/berlinInput/freeLoad15minBinSec_berlin16000.txt";
		String stochasticGeneral= "/cluster/home/baug/stellas/Runs/berlinInput/stochasticRandom+-5000.txt";
		
		double priceMaxPerkWh=0.11;// http://www.ekz.ch/internet/ekz/de/privatkunden/Tarife_neu/Tarife_Mixstrom.html
		double priceMinPerkWh=0.07;
		
		ArrayList<HubInfoDeterministic> myHubInfo = new ArrayList<HubInfoDeterministic>(0);
		myHubInfo.add(new HubInfoDeterministic(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
		
		final double standardChargingLength=15.0*DecentralizedSmartCharger.SECONDSPERMIN;
		final double bufferBatteryCharge=0.0;
		final double ev=0.9; 
		double kWHEV =24;
		double kWHPHEV =24;
		boolean gasHigh = true;
		
		final double xPercentDownUp=0.33;
		final double xPercentDown=1.0-xPercentDownUp;
		
		int numberOfHubsInX=1;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
	
double standardConnectionWatt=3500;
		
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
		
		
		ArrayList<HubInfoStochastic> myStochasticHubInfo = new ArrayList<HubInfoStochastic>(0);
		myStochasticHubInfo.add(new HubInfoStochastic(1, stochasticGeneral));
						
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
		System.out.println("average extra costs for extra vehicle charging all agents: "+mySimulation.getAverageExtraChargingAllEVs());
		System.out.println("average extra costs for extra vehicle charging all agents: "+mySimulation.getAverageExtraChargingAllPHEVs());
		
		
		/*
		 * HUBSOURCES
		 */
		
		System.out.println("average revenue from feed in for hub sources: "+mySimulation.getAverageFeedInRevenueHubSources());
		System.out.println("average extra charging cost for hub sources: "+mySimulation.getAverageExtraChargingHubSources());
		System.out.println("total energy for hub sources: "+mySimulation.getTotalJoulesFeedInHubSources());
		
		}
		
		

}
