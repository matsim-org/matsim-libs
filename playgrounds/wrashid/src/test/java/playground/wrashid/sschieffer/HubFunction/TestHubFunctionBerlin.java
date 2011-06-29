

/* *********************************************************************** *
 * project: org.matsim.*
 * TestHubFunctionBerlin.java
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


package playground.wrashid.sschieffer.HubFunction;

import java.io.BufferedWriter;
import java.io.FileWriter;
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
 *  We test if having different hubs with different price policies works and 
 *  really results in significantly different final charging costs for the agents
 *  </br> </br>
 *  for this purpose we divide the Berlin network into two hubs:
 *  <li> 2 hubs in x direction, 1 hub in y direction
 *  <li> hub2 is expensive, hub 1 is very cheap to charge
 *  
 *  </br> </br>
 *  in the end the average charging costs of agents commuting to hub2 versus to hub 1 are compared. </br>
 *  It is confirmed that those commuting to hub 2 have a much higher charging cost
 * 
 * @author Stella
 *
 */
public class TestHubFunctionBerlin extends TestCase{
	
	public static DecentralizedChargingSimulation mySimulation;
	final String outputPath="D:/ETH/MasterThesis/TestOutput/";
	
	public static void testMain(String[] args) throws IOException, ConvergenceException, FunctionEvaluationException, IllegalArgumentException {		
		
	}
	
	
	public void testResults() throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException, IOException{
		
		final double electrification= 1.0; 
	
		final double ev=1.0; 
		
		
		String configPath="test/scenarios/berlin/config.xml";				
		String freeLoadTxt= "test/input/playground/wrashid/sschieffer/freeLoad15minBinSec_berlin16000.txt";
		
		final double bufferBatteryCharge=0.0;
				
		final double standardChargingLength=15*60;
		
		
		/*
		 * TWO HUBS
		 * hub 1 - normal price
		 * hub 2- extremely expensive
		 */
		int numberOfHubsInX=2;
		int numberOfHubsInY=1;
		StellasHubMapping myMappingClass= new StellasHubMapping(numberOfHubsInX,numberOfHubsInY);
		
		double priceMaxPerkWhHub1=0.11;// http://www.ekz.ch/internet/ekz/de/privatkunden/Tarife_neu/Tarife_Mixstrom.html
		double priceMinPerkWhHub1=0.07;
		
		double priceMaxPerkWhHub2=100.0;// http://www.ekz.ch/internet/ekz/de/privatkunden/Tarife_neu/Tarife_Mixstrom.html
		double priceMinPerkWhHub2=50.0;
		
		// add two hubs to HubInformation
		ArrayList<HubInfoDeterministic> myHubInfo = new ArrayList<HubInfoDeterministic>(0);
		myHubInfo.add(new HubInfoDeterministic(1, freeLoadTxt, priceMaxPerkWhHub1, priceMinPerkWhHub1));
		myHubInfo.add(new HubInfoDeterministic(2, freeLoadTxt, priceMaxPerkWhHub2, priceMinPerkWhHub2));
		
		
		double kWHEV =24;
		double kWHPHEV =24;
		boolean gasHigh = true;
		
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
		
		mySimulation.addControlerListenerDecentralizedCharging();		
		mySimulation.controler.run();
		
		testChargingCosts();
		
	}
	
	
	
	public void testChargingCosts(){
		
		double aveHub1=0;
		int numHub1=0;
		double aveHub2=0;
		int numHub2=0;
		
		try{
		    // Create file 
			String title=(outputPath +"_costSummary.txt");
		    FileWriter fstream = new FileWriter(title);
		    BufferedWriter out = new BufferedWriter(fstream);
		    
		    out.write("Agent Id: \t");
		    out.write("EV?: \t");
		    
		    out.write("at Hub2?\t");
		    
		    out.write("charging cost:\t");
		    out.write("charging time: \n");
		    
		    for(Id id: mySimulation.mySmartCharger.vehicles.getKeySet()){
			    
			    	out.write(id.toString()+ "\t");
			    	out.write(mySimulation.mySmartCharger.hasAgentEV(id)+ "\t");
			    	
			    	boolean atHub2= mySimulation.mySmartCharger.isAgentAtHub(id, 2);
			    	out.write(atHub2+ "\t");
			    	
			    	//charging cost
			    	double cost= mySimulation.mySmartCharger.agentChargingCosts.get(id);
			    	out.write(cost+ "\t");
			    	
			    	if(atHub2){
			    		 aveHub2+=cost;
			    		 numHub2++;
			    	}else{ 
			    		aveHub1+=cost;
			    		numHub1++;
			    		}
			    	
			    	out.write(mySimulation.mySmartCharger.agentChargingSchedules.get(id).getTotalTimeOfIntervalsInSchedule()+ "\n");
			    	
		    }
		//Close the output stream
	    out.close();
	    
	    }catch (Exception e){
	    	//Catch exception if any
	    }
	    
	    System.out.println("average hub1"+ aveHub1/numHub1);
	    
	    System.out.println("average hub2"+ aveHub2/numHub2);		
		assertTrue(aveHub1/numHub1<aveHub2/numHub2);
	}
	

}
