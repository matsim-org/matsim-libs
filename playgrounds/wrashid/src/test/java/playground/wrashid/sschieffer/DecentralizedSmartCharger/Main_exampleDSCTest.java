

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
import playground.wrashid.sschieffer.DSC.DecentralizedChargingSimulation;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.HubInfoDeterministic;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.NetworkTopology.StellasHubMapping;

import java.util.*;

import junit.framework.TestCase;

/**
 * tests results of Decentralized Smart Charger on highest level
 * using the equil scenario with 2 agents
 * 
 * @author Stella
 */
public class Main_exampleDSCTest extends TestCase{
	
	public static DecentralizedChargingSimulation mySimulation;
	
	public static void testMain(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {		
		
	}
	
	
	public void testResults() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		final double electrification= 1.0; 
	
		final double ev=0.5; 
		
		final String outputPath="test/output/";
		String configPath="test/input/playground/wrashid/sschieffer/config_plans2.xml";				
		String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_berlin16000.txt";
		
		final double bufferBatteryCharge=0.0;
				
		final double standardChargingLength=15*60;
		
		int numberOfHubsInX=1;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
		
		double priceMaxPerkWh=0.11;// http://www.ekz.ch/internet/ekz/de/privatkunden/Tarife_neu/Tarife_Mixstrom.html
		double priceMinPerkWh=0.07;
		ArrayList<HubInfoDeterministic> myHubInfo = new ArrayList<HubInfoDeterministic>(0);
		myHubInfo.add(new HubInfoDeterministic(1, freeLoadTxt, priceMaxPerkWh, priceMinPerkWh));
				
		double kWHEV =24;
		double kWHPHEV =24;
		boolean gasHigh = true;
		
		double standardConnectionWatt=3500;
		
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
				standardConnectionWatt
				);
		
		mySimulation.addControlerListenerDecentralizedCharging();		
		mySimulation.controler.run();
		
		resultsCheck();
	}
	
	
	public static void resultsCheck(){
		/***********************
		 * CHECK
		 * **********************
		 */		
		assertEquals(mySimulation.getListOfAllEVAgents().size(), 1);
		assertEquals(mySimulation.getListOfAllPHEVAgents().size(), 1);
		// all LPs should be successful
		assertEquals(mySimulation.getListOfIdsOfEVAgentsWithFailedOptimization().size(), 0);
		
		// Emissions
		double emissions=mySimulation.getTotalEmissions();// no battery used in this case
		assertEquals(emissions, 0.0);
		
		
		for(Id id: mySimulation.mySmartCharger.vehicles.getKeySet()){
			// check parkingDrivingSchedule
			if(id.toString().equals(Integer.toString(2))){
				System.out.println("parking and driving schedule agent "+id.toString());
				System.out.println("has EV: "+mySimulation.mySmartCharger.hasAgentEV(id));
				mySimulation.getAllAgentParkingAndDrivingSchedules().get(id).printSchedule();
				
				System.out.println("charging Schedule agent "+id.toString());
				mySimulation.getAllAgentChargingSchedules().get(id).printSchedule();
				System.out.println("total charging time "+mySimulation.getAllAgentChargingSchedules().get(id).getTotalTimeOfIntervalsInSchedule());
				assertEquals(36060.76862282351, mySimulation.getAllAgentChargingSchedules().get(id).getTotalTimeOfIntervalsInSchedule());
				
				
				System.out.println("Total consumption from battery [joules]" 
						+ mySimulation.getTotalDrivingConsumptionOfAgentFromBattery(id));
				double testDiff= Math.abs(mySimulation.getTotalDrivingConsumptionOfAgentFromBattery(id)-(6.758097974737924*Math.pow(10,7)));
				assertEquals(testDiff<1.0, true);
				System.out.println("Total consumption from engine [joules]" +
						mySimulation.getTotalDrivingConsumptionOfAgentFromOtherSources(id));			
				assertEquals(mySimulation.getTotalDrivingConsumptionOfAgentFromOtherSources(id), 0.0);
				
			}
			
			if(id.toString().equals(Integer.toString(1))){
				System.out.println("parking and driving schedule agent "+id.toString());
				System.out.println("has EV: "+mySimulation.mySmartCharger.hasAgentEV(id));
				mySimulation.getAllAgentParkingAndDrivingSchedules().get(id).printSchedule();
				assertEquals(mySimulation.getAllAgentParkingAndDrivingSchedules().get(id).getNumberOfEntries(), 9);
				assertEquals(((ParkingInterval)mySimulation.getAllAgentParkingAndDrivingSchedules().get(id).timesInSchedule.get(7)).getRequiredChargingDuration(), 10866.919649567433);
				
				/*
				 * Starting SOC: 8640000.0
				Parking Interval 	 start: 0.0	  end: 165.78956623095112	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -4.502199259229531E8	  ChargingSchedule of size:  0
				Parking Interval 	 start: 165.78956623095112	  end: 21600.0	  ChargingTime:  19748.571428571424	  Optimal:  true	  Joules per Interval:  8.960859713694156E11	  ChargingSchedule of size:  1
				Driving Interval 	  start: 21600.0	  end: 22500.0	  battery consumption: 1.8786265875308476E7	 extra consumption: 0.0
				Parking Interval 	 start: 22500.0	  end: 35700.0	  ChargingTime:  5367.504535803352	  Optimal:  true	  Joules per Interval:  9.23969625786535E10	  ChargingSchedule of size:  6
				Driving Interval 	  start: 35700.0	  end: 38040.0	  battery consumption: 4.879471387207076E7	 extra consumption: 0.0
				Parking Interval 	 start: 38040.0	  end: 38117.7730088813	  ChargingTime:  77.77300888129685	  Optimal:  true	  Joules per Interval:  2256009.94700933	  ChargingSchedule of size:  1
				Parking Interval 	 start: 38117.7730088813	  end: 50357.16020410957	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -5.433030943174914E10	  ChargingSchedule of size:  0
				Parking Interval 	 start: 50357.16020410957	  end: 61224.079853677	  ChargingTime:  10866.919649567433	  Optimal:  true	  Joules per Interval:  1.0676896051476192E11	  ChargingSchedule of size:  1
				Parking Interval 	 start: 61224.079853677	  end: 86400.0	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -9.249470386024418E11	  ChargingSchedule of size:  0
				*************************
				 */
								
				System.out.println("charging Schedule agent "+id.toString());
				mySimulation.getAllAgentChargingSchedules().get(id).printSchedule();
				System.out.println("total charging time "+mySimulation.getAllAgentChargingSchedules().get(id).getTotalTimeOfIntervalsInSchedule());
				assertEquals(36060.76862282351, mySimulation.getAllAgentChargingSchedules().get(id).getTotalTimeOfIntervalsInSchedule());
				
				
				System.out.println("Total consumption from battery [joules]" 
						+ mySimulation.getTotalDrivingConsumptionOfAgentFromBattery(id));
				double testDiff= Math.abs(mySimulation.getTotalDrivingConsumptionOfAgentFromBattery(id)-(6.758097974737924*Math.pow(10,7)));
				assertEquals(testDiff<1.0, true);
				System.out.println("Total consumption from engine [joules]" +
						mySimulation.getTotalDrivingConsumptionOfAgentFromOtherSources(id));			
				assertEquals(mySimulation.getTotalDrivingConsumptionOfAgentFromOtherSources(id), 0.0);
				
			}
			
		}
	}

}
