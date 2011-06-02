/* *********************************************************************** *
 * project: org.matsim.*
 * V2GTestOnePlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.wrashid.sschieffer.V2G;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DSC.GeneralSource;
import playground.wrashid.sschieffer.DSC.LoadDistributionInterval;
import playground.wrashid.sschieffer.DSC.Schedule;
import playground.wrashid.sschieffer.DSC.TimeDataCollector;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.TestSimulationSetUp;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests methods:
 * <li> checks reducing agent vehicle load
 * <li> checks reducing hub load
 * <li> checks calc of feed in revenue
 * <li> checks if feed in revenue is correctly attributed
 * <li> checks attributeSuperfluousVehicleLoads Function
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan_vehicleV2G extends TestCase{

	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	public Id agentOne=null;
	
	Controler controler; 
	
	final double electrification= 1.0; 
	// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
	final double ev=0.0;
	
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=15*60;
	
	double compensationPerKWHRegulationUp=0.15;
	double compensationPerKWHRegulationDown=0.15;
	double compensationPERKWHFeedInVehicle=0.15;
	double xPercentNone=0;
	double xPercentDown=0;
	double xPercentDownUp=1.0;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	public static void testMain(String[] args) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException {
		
	}
	

	/**
	*  
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void testV2GCheckVehicles() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		final TestSimulationSetUp mySimulation = new TestSimulationSetUp(
				configPath, 
				electrification, 
				ev 
				);
		
		controler= mySimulation.getControler();
		
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					Id linkId=null;
					for (Id link: mySimulation.getControler().getNetwork().getLinks().keySet()){
						linkId=link;
						break;
					}
					
					mySimulation.setUpStochasticLoadDistributions(linkId);
					myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					myDecentralizedSmartCharger.run();
				
					
					HashMap<Integer, Schedule> stochasticLoad = mySimulation.getStochasticLoadSchedule();
					HashMap<Id, GeneralSource> g= mySimulation.getStochasticHubSource();
					HashMap<Id, Schedule> agentVehicleSourceMapping =mySimulation.getAgentStochasticLoadSources();
					
					DecentralizedSmartCharger.linkedListIntegerPrinter(stochasticLoad, "Before stochastic general");
					
					
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticLoad, 
							g, 
							agentVehicleSourceMapping);
					
					mySimulation.setUpAgentSchedules(
							myDecentralizedSmartCharger, 
							compensationPerKWHRegulationUp, 
							compensationPerKWHRegulationDown,
							compensationPERKWHFeedInVehicle,
							xPercentDown, 
							xPercentDownUp);
					
					myDecentralizedSmartCharger.setAgentContracts(mySimulation.getAgentContracts());
					myDecentralizedSmartCharger.myV2G.initializeAgentStats();
					
					/*
					 * CHECK VEHICLE
					 * BEFORE
					 *  * //-3500 between 0 and 2000
						// 3500 between 20000 and 21000
					 */
					DecentralizedSmartCharger.linkedListIdPrinter(agentVehicleSourceMapping,  "Before agent");
					for(Id id: agentVehicleSourceMapping.keySet()){
						
						/*
						 * -3500              3500
						 * 0-2000            20000-21000
						 */
						checkReduceAgentVehicleLoad(id);
						/*
						 * 0                 7000
						 * 0-2000            20000-21000
						 */
						checkAttributeSuperfluousVehicleLoadToGrid(id);
						/*
						 * Vehicle
						 * -1000              7000
						 * 0-2000            20000-21000
						 * 
						 * Hub
						 * 4500
						 */
						checkChargeMore(id);
						
					}			
					
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	
	public void checkChargeMore(Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		/*
		 * Vehicle
		 * -1000              7000
		 * 0-2000            20000-21000
		 * 
		 * Hub
		 * 4500
		 */
		
		myDecentralizedSmartCharger.myV2G.chargeMoreToAccomodateExtraVehicleLoad(
				id, 
				 new LoadDistributionInterval(0, 2000, -3500));
		
		// NEW VEHICLE LOAD == -1000+3500
		TimeDataCollector dataC= myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceAfter15MinBins.get(id);
		double exp1=dataC.extrapolateValueAtTimeFromDataCollector(0.0);
		assertEquals(exp1, 2500.0);
		
		
		// NEW HUB LOAD == 4500-3500=1000
		dataC= myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadAfter15MinBins.get(1);
		double newVal=dataC.getYAtEntry(0);
		assertEquals(newVal, 1000.0 );
		
	}
	
	
	public void checkReduceAgentVehicleLoad(Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		myDecentralizedSmartCharger.myV2G.reduceAgentVehicleLoadsByGivenLoadInterval(
				id, new LoadDistributionInterval(0, 2000, -3500));
		myDecentralizedSmartCharger.myV2G.reduceAgentVehicleLoadsByGivenLoadInterval(
				id, new LoadDistributionInterval(20000, 21000, -3500));
		// now it should be zero in first part and 7000 in second part
		//CHECK
		TimeDataCollector dataC= myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceAfter15MinBins.get(id);
		double exp1=dataC.extrapolateValueAtTimeFromDataCollector(0.0);
		
		double exp2= dataC.getYAtEntry(23); //23*900=20700
		
		assertEquals(exp1, 0.0);
		assertEquals(exp2, 7000.0);
	}
	
	
	
	public void checkAttributeSuperfluousVehicleLoadToGrid(Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		// get first entry of HubLoadGeneral to cmpare at the end
				
		myDecentralizedSmartCharger.myV2G.attributeSuperfluousVehicleLoadsToGridIfPossible(
				id, 
				new LoadDistributionInterval(0, 900, 1000));
		
		double expectedFeedInRev= 900*1000.0*compensationPERKWHFeedInVehicle*DecentralizedSmartCharger.KWHPERJOULE;
		assertEquals(myDecentralizedSmartCharger.myV2G.calculateCompensationFeedIn(900*1000.0, id),
				expectedFeedInRev);
		
		double actualAssignedRev=myDecentralizedSmartCharger.myV2G.getAgentRevenueFeedIn(id);
		assertEquals(actualAssignedRev, expectedFeedInRev);
		
		// expect vehicle load to be 1000 lower than before - currently -3500+3500=0
		TimeDataCollector dataC= myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceAfter15MinBins.get(id);
		double exp1=dataC.extrapolateValueAtTimeFromDataCollector(0.0);
		
		assertEquals(exp1, -1000.0);
		
		// check if hub load was increased accordingly - hubLoad before was 3500 should now be 4500
		dataC= myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadAfter15MinBins.get(1);
		assertEquals(dataC.getYAtEntry(0),4500.0 );
	}
	
	
	
	
	
}


