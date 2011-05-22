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

import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LoadDistributionInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.Schedule;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.TestSimulationSetUp;

import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests methods:
 * <li> checks example of vehicle load: attribute load to hub, reduce vehicle load
 * <li> example of stochastic load: getV2GRevenueForAgent, reduce hub load
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan_general extends TestCase{

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
	public void testThis() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
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
					
					
					myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					
					myDecentralizedSmartCharger.run();
					
					/***********************************
					 * V2G
					 * vehicle load
					 * sec 0-900   -3500  -- energy use locally requires battery discharge
					 * 
					 * stochastic
					 * sec 0-900    3500  -- energy regulation down
					 * *********************************
					 */
					
					HashMap<Integer, Schedule> stochasticLoad = new HashMap<Integer, Schedule>();
					stochasticLoad.put(1, new Schedule(
							new LoadDistributionInterval(0.0, 900.0, new PolynomialFunction(new double[]{3500.0}), true)));
						
					HashMap<Id, Schedule> agentVehicleSourceMapping= new HashMap<Id, Schedule>();
					
					for(Id id: controler.getPopulation().getPersons().keySet()){
						agentOne=id;
						// -3500 <0 --> requires regulation up
						agentVehicleSourceMapping.put(id, new Schedule(
								new LoadDistributionInterval(0.0, 900.0, new PolynomialFunction(new double[]{-3500.0}), true)));
						
					}
					
					
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
					myDecentralizedSmartCharger.setV2GRegUpAndDownStats(xPercentDown,xPercentDownUp);// normally in initializeV2G
					
					
					/**
					 * CHECK TEST FIND AND RETURN...
					 */
					testFindAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(agentOne);
					
										
					/**
					 * testCalcCompensation (double contributionInJoulesAgent, Id agentId)
					 */
					
					testCalcCompensation (100.0, agentOne);
					testCalcCompensation (-100.0, agentOne);
					
					
					checkVehiclesDischarge();
					
					checkStochasticregDown();
					
					
					/***********************************
					 * V2G
					 * vehicle load
					 * sec 0-900   3500  -- energy production locally
					 * 
					 * stochastic
					 * slast 900 sec  -- -3500 energy regulation up
					 * *********************************
					 */
					
					stochasticLoad = new HashMap<Integer, Schedule>();
					stochasticLoad.put(1, new Schedule(
							new LoadDistributionInterval(DecentralizedSmartCharger.SECONDSPERDAY-900.0, 
									DecentralizedSmartCharger.SECONDSPERDAY, new PolynomialFunction(new double[]{-3500.0}), true)));
					
					agentVehicleSourceMapping= new HashMap<Id, Schedule>();
					agentVehicleSourceMapping.put(agentOne, new Schedule(
							new LoadDistributionInterval(0.0, 900.0, new PolynomialFunction(new double[]{3500.0}), true)));
					
					
					myDecentralizedSmartCharger.setStochasticSources(
							stochasticLoad, 
							null, 
							agentVehicleSourceMapping);
					
					
					checkVehiclesGeneration();
					
					 checkStochasticregUp();
					 
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	public void checkStochasticregUp() throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		/**
		 * vehicle load
					 * sec 0-900   3500  -- energy production locally
					 * 
					 * stochastic
					 * last 900 sec  --   -3500 energy regulation up
		 */
		double revBegin=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
		myDecentralizedSmartCharger.checkHubStochasticLoads();
		//load is -3500W in first 900 sec -- cheap charging
					
		System.out.println("stochastic load after");
		myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).printSchedule();
		assertEquals(0.0, 
				((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.
						get(1).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		
		// CHECK REVENUE FOR REG DOWN
		double rev=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
		double revExp= compensationPerKWHRegulationDown*DecentralizedSmartCharger.KWHPERJOULE*900.0*3500.0;
		double newRev=rev-revBegin;
		assertEquals(Math.abs((newRev)-revExp)<0.0000001, true);
		
	}
	
	
	
	public void checkStochasticregDown() throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		/**
		 *  * vehicle load
					 * sec 0-900   -3500  -- energy use locally requires battery discharge
					 * 
					 * stochastic
					 * sec 0-900    7000  -- energy regulation down
		 */
		
		myDecentralizedSmartCharger.myHubLoadReader.calculateAndVisualizeConnectivityDistributionsAtHubsInHubLoadReader();
		
		myDecentralizedSmartCharger.checkHubStochasticLoads();
		//load is 3500W in first 900 sec -- cheap charging				
		System.out.println("stochastic load after");
		myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).printSchedule();
		//was 7000 after lat vehicle check
		// minus 3500 now
		assertEquals(3500.0, 
				((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).
						timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		
		// CHECK REVENUE FOR REG DOWN
		double rev=myDecentralizedSmartCharger.getV2GRevenueForAgent(agentOne);
		double revExp= compensationPerKWHRegulationDown*DecentralizedSmartCharger.KWHPERJOULE*900.0*3500.0;
		assertEquals(Math.abs(rev-revExp)<0.0000001, true);
		
	}
	
	
	public void checkVehiclesGeneration() throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		/**
		 * CHECK VEHICLE SOURCES
		 *   * vehicle load
					 * sec 0-900   3500  -- energy production locally
					 * 
					 * stochastic
					 * slast 900 sec  -- -3500 energy regulation up
		 */
		
				
		double chargingCosts = myDecentralizedSmartCharger.getChargingCostsForAgents().get(agentOne); 
		myDecentralizedSmartCharger.checkVehicleSources();
		
		System.out.println("agent sources before");
		myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMapping.get(agentOne).printSchedule();
		System.out.println("agent sources after");
		myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMappingAfter.get(agentOne).printSchedule();
		assertEquals(3500.0, 
				((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMapping.get(agentOne).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		
		assertEquals(0.0, 
				((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMappingAfter.get(agentOne).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		System.out.println("stochastic load after vehicle  check");
		myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).printSchedule();
		
		// stochastic load was 3500 and should still be 3500 - generted energy will just be taken up by engine
		assertEquals(900.0, myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).timesInSchedule.get(0).getIntervalLength());
		
		assertEquals(-3500.0, ((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		
		double chargingCostsAfter = myDecentralizedSmartCharger.getChargingCostsForAgents().get(agentOne); 
		// change charging costs
		assertEquals(chargingCosts>chargingCostsAfter, true);
	}
	
	
	public void checkVehiclesDischarge() throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException{
		/**
		 * CHECK VEHICLE SOURCES
		 *  * sec 0-900   -3500  -- energy use locally requires battery discharge
					 * 
					 * stochastic
					 * sec 0-900    3500  -- energy regulation down
		 */
		double chargingCosts = myDecentralizedSmartCharger.getChargingCostsForAgents().get(agentOne); 
		
		myDecentralizedSmartCharger.checkVehicleSources();
		
		System.out.println("Parking Driving Schedule after check vehicle sources");
		myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(agentOne).printSchedule();
		System.out.println("agent sources before");
		myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMapping.get(agentOne).printSchedule();
		System.out.println("agent sources after");
		myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMappingAfter.get(agentOne).printSchedule();
		assertEquals(-3500.0, 
				((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMapping.get(agentOne).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		
		assertEquals(0.0, 
				((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.agentVehicleSourceMappingAfter.get(agentOne).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		System.out.println("stochastic load after vehicle  check");
		myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).printSchedule();
		
		assertEquals(900.0, myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(1).timesInSchedule.get(0).getIntervalLength());
		// extra charging done thus stochastic load increases by 3500
		assertEquals(7000.0, ((LoadDistributionInterval)myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistributionAfter.get(1).timesInSchedule.get(0)).getPolynomialFunction().getCoefficients()[0]);
		
		//charging costs increase
		double chargingCostsAfter = myDecentralizedSmartCharger.getChargingCostsForAgents().get(agentOne); 
		
		assertEquals(chargingCosts<chargingCostsAfter, true);
		
	}
	
	public void testFindAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule agent= myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(id);
		
		Schedule overlap= myDecentralizedSmartCharger.myV2G.findAndReturnAgentScheduleWithinLoadIntervalWhichIsAtSpecificHub(
				id, 
				1, 				
				new LoadDistributionInterval(0.0, 1000.0, null, false));
		
		assertEquals(overlap.getTotalTimeOfIntervalsInSchedule(), 1000.0);
		assertEquals(overlap.timesInSchedule.get(0).getEndTime(), 1000.0);
		assertEquals(overlap.getNumberOfEntries(), 1);
		
	}
	
	
	
	public void testCalcCompensation (double contributionInJoulesAgent, Id agentId){
		//ev=0.0;
		double comp=myDecentralizedSmartCharger.myV2G.calculateCompensationUpDown(contributionInJoulesAgent, agentId);
		double expectedComp= Math.abs(0.15*myDecentralizedSmartCharger.KWHPERJOULE*contributionInJoulesAgent);
		assertEquals(comp, expectedComp);
		
	}
	
	
}


