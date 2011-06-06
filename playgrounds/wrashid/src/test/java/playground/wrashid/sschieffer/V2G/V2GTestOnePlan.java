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
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeDataCollector;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests methods:
 * <li> checks if adding revenue from V2G works and statistik calc function works
 * <li> checks the most important V2G function reschedule at the example of one agent
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan extends TestCase{

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
					
					mySimulation.setUpStochasticLoadDistributions();
					myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					myDecentralizedSmartCharger.run();
					
				
					// all day 3500
					HashMap<Integer, Schedule> stochasticLoad = mySimulation.getStochasticLoadSchedule();
					HashMap<Id, Schedule> agentVehicleSourceMapping =mySimulation.getAgentStochasticLoadSources();
					
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticLoad, 
							null, 
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
					myDecentralizedSmartCharger.setV2GRegUpAndDownStats(xPercentDown,xPercentDownUp);
					
					for(Id id: mySimulation.getControler().getPopulation().getPersons().keySet()){
						agentOne=id;
						break;
					}
					
					
					
					checkRevenueStats();
					
					
					checkRescheduling();
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	
	public void checkRescheduling() throws Exception{
		
		myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).printSchedule();
		double chargingTimeFirstInterval= ((ParkingInterval)myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).
				timesInSchedule.get(0)).getRequiredChargingDuration();
		/*
		 * Decentralized Smart Charger DONE
			*************************
			Starting SOC: 8640000.0
			Parking Interval 	 start: 0.0	  end: 21600.0	  ChargingTime:  19748.571428571428	  Optimal:  true	  Joules per Interval:  216000.0	  ChargingSchedule of size:  1
			Driving Interval 	  start: 21600.0	  end: 22500.0	  consumption: 1.8786265875308476E7
			Parking Interval 	 start: 22500.0	  end: 35700.0	  ChargingTime:  5367.504535803354	  Optimal:  true	  Joules per Interval:  132000.0	  ChargingSchedule of size:  6
			Driving Interval 	  start: 35700.0	  end: 38040.0	  consumption: 4.879471387207076E7
			Parking Interval 	 start: 38040.0	  end: 62490.0	  ChargingTime:  13941.346820583964	  Optimal:  true	  Joules per Interval:  244500.0	  ChargingSchedule of size:  16
			Parking Interval 	 start: 62490.0	  end: 86400.0	  ChargingTime:  0.0	  Optimal:  false	  Joules per Interval:  -239100.0	  ChargingSchedule of size:  0
			*************************
		 */
		
		
		double costChargingBefore= myDecentralizedSmartCharger.getChargingCostsForAgents().get(agentOne);
		
		//PREPARATION
		Schedule answerScheduleAfterElectricSourceInterval= myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).
								cutScheduleAtTimeSecondHalf(21600, 0, agentOne);
		((ParkingInterval)answerScheduleAfterElectricSourceInterval.timesInSchedule.get(1)).setRequiredChargingDuration(0);
		
		LoadDistributionInterval electricSourceInterval = new LoadDistributionInterval(0, 21600, 1);
		
		
		// RESCHEDULE
		myDecentralizedSmartCharger.myV2G.reschedule(agentOne, 
				answerScheduleAfterElectricSourceInterval, 
				electricSourceInterval, 
				1, 
				0, 3500);
		
		
		//CHECKS
		double costChargingAfter= myDecentralizedSmartCharger.getChargingCostsForAgents().get(agentOne);
		assertEquals(costChargingBefore-1, costChargingAfter);
		
		
		/*UPDATE AGENTPARKINGADNDRIVINGSCHEDULE WITH NEW required charging times
		 * 3500 joules >0 is reg down, is as if agent charge longer 
		 * 1 second longer charging in first interval joules= 3500 correponds to 1sec charging time at standard connection speed
		 */
		double newChargingTimeFirstInterval= ((ParkingInterval)myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).
				timesInSchedule.get(0)).getRequiredChargingDuration();
		
		assertEquals(newChargingTimeFirstInterval-chargingTimeFirstInterval, 1.0);
		
		// charging time was set 0
		double newChargingTimeThirdInterval= ((ParkingInterval)myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).
				timesInSchedule.get(2)).getRequiredChargingDuration();
		assertEquals(newChargingTimeThirdInterval, 0.0);
		
		
		/*
		 * DISTRIBUTE new found required charging times for agent	
		//check times required charging after
		 */
		Schedule newChargingSchedule = ((ParkingInterval)myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).
				timesInSchedule.get(0)).getChargingSchedule();
		assertEquals(newChargingTimeFirstInterval, newChargingSchedule.getTotalTimeOfIntervalsInSchedule());
		
		newChargingSchedule = ((ParkingInterval)myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).
				timesInSchedule.get(2)).getChargingSchedule();
		assertEquals(newChargingSchedule, null);	
			
	}
	
		
		
		
		public void checkRevenueStats(){
			double revenue=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
			myDecentralizedSmartCharger.myV2G.addRevenueToAgentFromV2G(100.0, agentOne);
			double revenueNew=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
			
			assertEquals(revenue+100.0, revenueNew);
			
			
			/**********************************************
			 *  * CHECK
			 * addJoulesV2G
			 */
			
			// regulation up joules<0
			double up = myDecentralizedSmartCharger.myV2G.getTotalRegulationUp();
			myDecentralizedSmartCharger.myV2G.addJoulesUpDownToAgentStats(-1000.0, agentOne);
			myDecentralizedSmartCharger.myV2G.calcV2GVehicleStats();
			double newUp = myDecentralizedSmartCharger.myV2G.getTotalRegulationUp();
			
			assertEquals(-1000.0, newUp-up);
			
			// regulation up joules>0
			double down = myDecentralizedSmartCharger.myV2G.getTotalRegulationDown();
			myDecentralizedSmartCharger.myV2G.addJoulesUpDownToAgentStats(1000.0, agentOne);
			myDecentralizedSmartCharger.myV2G.calcV2GVehicleStats();
			double newDown = myDecentralizedSmartCharger.myV2G.getTotalRegulationDown();
		
			assertEquals(1000.0, newDown-down);
			
			/**********************************************
			 *  * CHECK
			 * sensible outcome averageV2G
			 */
			myDecentralizedSmartCharger.myV2G.calcV2GVehicleStats();
			assertEquals(myDecentralizedSmartCharger.myV2G.getAverageV2GRevenuePHEV(), myDecentralizedSmartCharger.myV2G.getAverageV2GRevenueAgent());
			
		}
	
	
}


