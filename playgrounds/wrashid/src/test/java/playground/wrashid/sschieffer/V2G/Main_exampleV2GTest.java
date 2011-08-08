
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
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoDeterministic;
import playground.wrashid.sschieffer.SetUp.NetworkTopology.StellasHubMapping;
import playground.wrashid.sschieffer.V2G.StochasticLoadCollector;

import java.util.*;

import junit.framework.TestCase;


/**
 * checks on highest level if results of simulation make sense
 * unfortunately it is not possible to check exact values -> because of the distribution of charging slots 
 * with random numbers and probability density functions the exact charging times and thus V2G are different every time
 * 
 * @author Stella
 *
 */
public class Main_exampleV2GTest extends TestCase{
	
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
		
		final String outputPath="test/output";
		//String configPath="test/input/playground/wrashid/sschieffer/config.xml";// 100 agents
		String configPath="test/input/playground/wrashid/sschieffer/config_plans2.xml";
		
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
		
		double xPercentDown=0.0;
		double xPercentDownUp=1.0;
		
		double compensationPerKWHRegulationUp=0.1;
		double compensationPerKWHRegulationDown=0.005;
		double compensationPERKWHFeedInVehicle=0.005;
		
		mySimulation.setUpV2G(				
				xPercentDown,
				xPercentDownUp,
				new StochasticLoadCollector(mySimulation),//test scenario with 4 times 1 hour stochastic load
				compensationPerKWHRegulationUp,
				compensationPerKWHRegulationDown,
				compensationPERKWHFeedInVehicle);
	
		mySimulation.controler.run();
		
		resultsCheckUpDown();
	}
	
	
	public static void resultsCheckUpDown(){
		/*********************
		 * Check V2G
		 *********************/
		//AVERAGE REVENUE - only EVs, thus average revenue per agent= AR of EV
		System.out.println("average revenue from V2G for agents: "+mySimulation.getAverageRevenueV2GPerAgent());		
		System.out.println("average revenue from V2G for EV agents: "+mySimulation.getAverageRevenueV2GPerEV());				
		System.out.println("average revenue from V2G for PHEV agents: "+mySimulation.getAverageRevenueV2GPerPHEV());
		//assertEquals(mySimulation.getAverageRevenueV2GPerAgent(),0.10062500000000006); CANT assert - always stochastic influence
		assertEquals(mySimulation.getAverageRevenueV2GPerAgent(), mySimulation.getAverageRevenueV2GPerEV());
		assertEquals(0.0, mySimulation.getAverageRevenueV2GPerPHEV());
		
		assertEquals(0.235, mySimulation.getAverageRevenueV2GPerEV(), 0.002);
		
		//JOULES PROVIDED IN REGULATION UP OR DOWN
		System.out.println("total joules from V2G for regulation up: "+mySimulation.getTotalJoulesV2GRegulationUp());		
		System.out.println("total joules from V2G for regulation up from EVs: "+mySimulation.getTotalJoulesV2GRegulationUpEV());		
		System.out.println("total joules from V2G for regulation up from PHEVs: "+mySimulation.getTotalJoulesV2GRegulationUpPHEV());
		assertEquals(mySimulation.getTotalJoulesV2GRegulationUpEV(), mySimulation.getTotalJoulesV2GRegulationUp());
		assertEquals(mySimulation.getTotalJoulesV2GRegulationUpPHEV(), 0.0);
		assertEquals(mySimulation.getTotalJoulesV2GRegulationUpEV()!=0.0, true);
		assertEquals(mySimulation.getTotalJoulesV2GRegulationUpEV(), -1.5378304378040342E7, -0.5E7);
		
		
		System.out.println("total joules from V2G for regulation down: "+mySimulation.getTotalJoulesV2GRegulationDown());		
		System.out.println("total joules from V2G for regulation down from EVs: "+mySimulation.getTotalJoulesV2GRegulationDownEV());		
		System.out.println("total joules from V2G for regulation down from PHEVs: "+mySimulation.getTotalJoulesV2GRegulationDownPHEV());
		assertEquals(mySimulation.getTotalJoulesV2GRegulationDownEV(), mySimulation.getTotalJoulesV2GRegulationDown());
		assertEquals(mySimulation.getTotalJoulesV2GRegulationDownPHEV(), 0.0);
		assertEquals(mySimulation.getTotalJoulesV2GRegulationDown()!=0.0, true);
		
		/*
		 * VEHICLES AS SOURCES
		 */
		//FEED IN from vehicle local production 
		System.out.println("average revenue from feed in for all agents: "+mySimulation.getAverageRevenueFeedInAllAgents());
		System.out.println("average revenue from feed in for EV agents: "+mySimulation.getAverageRevenueFeedInForEVs());
		System.out.println("average revenue from feed in for PHEV agents: "+mySimulation.getAverageRevenueFeedInForPHEVs());
		assertEquals(mySimulation.getAverageRevenueFeedInAllAgents(), 0.0);
		
		
		//ENERGY FEEDIN VEHICLES
		System.out.println("total energy feed in for all agents: "+mySimulation.getTotalJoulesFromFeedIn());
		System.out.println("total energy feed in for EV agents: "+mySimulation.getTotalJoulesFromFeedInfromEVs());
		System.out.println("total energy feed in for PHEV agents: "+mySimulation.getTotalJoulesFromFeedInFromPHEVs());
		assertEquals(mySimulation.getTotalJoulesFromFeedIn(), 0.0);
		
		//ENERGY EXTRA COSTS From extra charging
		System.out.println("average extra costs for extra vehicle charging all agents: "+mySimulation.getAverageExtraChargingAllVehicles());
		System.out.println("average extra costs for extra vehicle charging EV: "+mySimulation.getAverageExtraChargingAllEVs());
		System.out.println("average extra costs for extra vehicle charging PHEV: "+mySimulation.getAverageExtraChargingAllPHEVs());
		assertEquals(mySimulation.getAverageExtraChargingAllVehicles(), 0.0);
		
		//Extra joules charged from battery for additional stochastic consumption 
		
		System.out.println("average joules taken from battery for extra stochastic consumption: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionAllAgents());
		System.out.println("average joules taken from battery for extra stochastic consumption EV: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionEV());
		System.out.println("average joules taken from battery for extra stochastic consumption PHEV: "+mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionPHEV());
		assertEquals(mySimulation.getAverageJouleV2GTakenFromBatteryForExtraConsumptionAllAgents(), 0.0);
		
		//joules saved by local production
		System.out.println("average joules saved by local stochastic production all agents: "+mySimulation.getAverageJouleSavedByLocalV2GProductionAllAgents());
		System.out.println("average joules saved by local stochastic production EV: "+mySimulation.getAverageJouleSavedByLocalV2GProductionEV());
		System.out.println("average joules saved by local stochastic production PHEV: "+mySimulation.getAverageJouleSavedByLocalV2GProductionPHEV());
		assertEquals(mySimulation.getAverageJouleSavedByLocalV2GProductionAllAgents(), 0.0);
		
		/*
		 * HUBSOURCES
		 */
		System.out.println("average revenue from feed in for hub sources: "+mySimulation.getAverageFeedInRevenueHubSources());
		System.out.println("average extra charging cost for hub sources: "+mySimulation.getAverageExtraChargingHubSources());
		System.out.println("total energy for hub sources: "+mySimulation.getTotalJoulesFeedInHubSources());
		assertEquals(mySimulation.getAverageFeedInRevenueHubSources(), 0.0);
				
		
		assertEquals(mySimulation.getListOfAllEVAgents().size(), 2);
	}

	
	
}
