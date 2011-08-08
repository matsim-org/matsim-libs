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
import playground.wrashid.sschieffer.DecentralizedSmartCharger.TestSimulationSetUp;
import playground.wrashid.sschieffer.SetUp.ElectricitySourceDefinition.GeneralSource;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeDataCollector;

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
public class V2GTestOnePlan_hubsourceV2G extends TestCase{

	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="test/output";
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
	public void testV2GCheckHubSources() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
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
					
					checkHubSource(linkId);
					
					checkHubSourceChargeExtra();
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	
	public void checkHubSource(Id linkId) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		/*
		 * reduceHubLoadByGivenLoadInterval(hub, electricSourceInterval);
		 * //3500 between 0-2000
		 */
		
		myDecentralizedSmartCharger.checkHubSources();
		// expect hubSource to be 0 and grid 3500 up =7000
		TimeDataCollector dataC= myDecentralizedSmartCharger.myHubLoadReader.locationSourceMappingAfter15MinBins.get(linkId);
		double exp1=dataC.extrapolateValueAtTimeFromDataCollector(0.0);
		assertEquals(exp1, 0.0);
		
		dataC= myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadAfter15MinBins.get(1);
		assertEquals(dataC.getYAtEntry(0),7000.0 );
		
	}
	
	
	public void checkHubSourceChargeExtra() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		//myDecentralizedSmartCharger.myV2G.reduceHubLoadByGivenLoadInterval(hubId, electricSourceInterval)
		for(Id linkId:myDecentralizedSmartCharger.myHubLoadReader.locationSourceMapping.keySet()){
			myDecentralizedSmartCharger.myV2G.hubSourceChargeExtra(
					linkId, 
					new LoadDistributionInterval(0.0, 900, -3500), 
					900*3500.0
			);
			
			// check that extra cost assigned
			double extraChargingCost=myDecentralizedSmartCharger.myV2G.getHubSourceStatistic().get(linkId).getExtraChargingCosts();
			assertEquals(extraChargingCost<0.0, true);
			
			
			// check that hub source reduced by 3500
			//reduceHubLoadByGivenLoadInterval by 3500
			// expect hubSource to be 3500 and grid 7000-3500=3500
			TimeDataCollector dataC= myDecentralizedSmartCharger.myHubLoadReader.locationSourceMappingAfter15MinBins.get(linkId);
			double exp1=dataC.extrapolateValueAtTimeFromDataCollector(0.0);
			assertEquals(exp1, 3500.0);
			
			dataC= myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadAfter15MinBins.get(1);
			double act= dataC.getYAtEntry(0);
			assertEquals(act,3500.0 );
			
		}
		
	}
	
}


