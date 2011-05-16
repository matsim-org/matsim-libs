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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

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

import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests methods:
 * <li> checks if vehicle source is reduced in simulation as expected
 * <li> check if addRevenueToAgentFromV2G has expected results
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan_checkVehicles extends TestCase{

	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	public Id agentOne=null;
	
	Controler controler; 
	
	final double phev=1.0;
	final double ev=0.0;
	final double combustion=0.0;
	
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=15*60;
	
	double compensationPerKWHRegulationUp=0.15;
	double compensationPerKWHRegulationDown=0.15;
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
				phev, 
				ev, 
				combustion);
		
		controler= mySimulation.getControler();
		
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					mySimulation.setUpStochasticLoadDistributions();
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					
					myDecentralizedSmartCharger.run();
					
					/***********************************
					 * V2G
					 * *********************************
					 */
					// all day 3500
					HashMap<Integer, Schedule> stochasticLoad = 
						mySimulation.getStochasticLoadSchedule();
						
					HashMap<Id, Schedule> agentVehicleSourceMapping =
						mySimulation.getAgentStochasticLoadSources();
					
					DecentralizedSmartCharger.linkedListIntegerPrinter(stochasticLoad, "Before stochastic general");
					
					DecentralizedSmartCharger.linkedListIdPrinter(agentVehicleSourceMapping,  "Before agent");
					
					myDecentralizedSmartCharger.setStochasticSources(
							mySimulation.getStochasticLoadSchedule(), 
							null, 
							mySimulation.getAgentStochasticLoadSources());
					
					mySimulation.setUpAgentSchedules(
							myDecentralizedSmartCharger, 
							compensationPerKWHRegulationUp, 
							compensationPerKWHRegulationDown, 
							xPercentNone, 
							xPercentDown, 
							xPercentDownUp);
					
					myDecentralizedSmartCharger.setAgentContracts(mySimulation.getAgentContracts());
					
					
					for(Id id: controler.getPopulation().getPersons().keySet()){
						agentOne=id;
						System.out.println("AGENT VEHICLE SOURCE BEFORE V2G of -3500 between 0-300");
						agentVehicleSourceMapping.get(id).printSchedule();
						LoadDistributionInterval lFirst= 
							(LoadDistributionInterval)agentVehicleSourceMapping.get(agentOne).timesInSchedule.get(0);
						assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0], 
								-3500.0);
					}
					
					myDecentralizedSmartCharger.initializeAndRunV2G();
						
					
					System.out.println("AGENT VEHICLE SOURCE AFTER V2G ");
					agentVehicleSourceMapping.get(agentOne).printSchedule();
						
						
					LoadDistributionInterval lFirst= 
							(LoadDistributionInterval)agentVehicleSourceMapping.get(agentOne).timesInSchedule.get(0);
					assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0], 
								0.0);
					
					
					double revenue=myDecentralizedSmartCharger.getAgentV2GRevenues().get(agentOne);
					
					myDecentralizedSmartCharger.myV2G.addRevenueToAgentFromV2G(100.0, agentOne);
					
					double revenueNew=myDecentralizedSmartCharger.getAgentV2GRevenues().get(agentOne);
					
					assertEquals(revenue+100.0, revenueNew);
					
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	
	
	
	
	
}


