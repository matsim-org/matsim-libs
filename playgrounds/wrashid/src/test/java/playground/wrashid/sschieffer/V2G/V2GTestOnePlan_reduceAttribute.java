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

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.LoadDistributionInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.Schedule;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.TestSimulationSetUp;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.V2G.V2G;
import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests methods:
 * <li> checkVehicle sources
 * 
 * @author Stella
 *
 */
public class V2GTestOnePlan_reduceAttribute extends TestCase{

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
					
					V2G myV2G= new V2G(myDecentralizedSmartCharger);
					myDecentralizedSmartCharger.setV2G(myV2G);
					
					for(Id id: myDecentralizedSmartCharger.vehicles.getKeySet()){
						System.out.println("AGENT VEHICLE SOURCE BEFORE V2G of -3500 between 0-300");
						
						agentVehicleSourceMapping.get(id).printSchedule();
						//0-2000 at -3500
						// -3500+3500=0
						myV2G.reduceAgentVehicleLoadsByGivenLoadInterval(
								id, 
								new LoadDistributionInterval(0,
										300, 
										new PolynomialFunction(new double[]{-3500}),
										false));
						
						System.out.println("AGENT VEHICLE SOURCE AFTER V2G of -3500 between 0-300");
						agentVehicleSourceMapping.get(id).printSchedule();
						
						LoadDistributionInterval lFirst= 
							(LoadDistributionInterval) agentVehicleSourceMapping.get(id).timesInSchedule.get(0);
						
						LoadDistributionInterval lSecond= 
							(LoadDistributionInterval) agentVehicleSourceMapping.get(id).timesInSchedule.get(1);
						
						assertEquals(agentVehicleSourceMapping.get(id).getNumberOfEntries(), 3);
						
						assertEquals(lFirst.getEndTime(),
								300.0);
						assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0],
								0.0);
						assertEquals(lSecond.getPolynomialFunction().getCoefficients()[0],
								-3500.0);
						
						
						//20000.0, 20300.0  at 3500
						// 3500-(3500)=0
						myV2G.reduceAgentVehicleLoadsByGivenLoadInterval(
								id, 
								new LoadDistributionInterval(20000.0,
										20300, 
										new PolynomialFunction(new double[]{3500}),
										true));
						
						
						System.out.println("AGENT VEHICLE SOURCE AFTER V2G of 3500 between 20000-20300");
						agentVehicleSourceMapping.get(id).printSchedule();
						//writing out
						lFirst= 
							(LoadDistributionInterval) agentVehicleSourceMapping.get(id).timesInSchedule.get(0);
						
						lSecond= 
							(LoadDistributionInterval) agentVehicleSourceMapping.get(id).timesInSchedule.get(1);
						
						LoadDistributionInterval lThird= 
							(LoadDistributionInterval) agentVehicleSourceMapping.get(id).timesInSchedule.get(2);
						
						
						assertEquals(agentVehicleSourceMapping.get(id).getNumberOfEntries(), 3);
						
						assertEquals(lFirst.getEndTime(),
								300.0);
						assertEquals(lFirst.getPolynomialFunction().getCoefficients()[0],
								0.0);
						assertEquals(lSecond.getPolynomialFunction().getCoefficients()[0],
								-3500.0);
						
						assertEquals(lThird.getPolynomialFunction().getCoefficients()[0],
								0.0);
						assertEquals(lThird.getEndTime(),
								20300.0);
						
						
						
						/*
						 *  * CHECK
						 * 
						 * attributeSuperfluousVehicleLoadsToGridIfPossible(Id agentId, Schedule agentParkingDrivingSchedule, 
						LoadDistributionInterval electricSourceInterval)
						
						 */
						
						System.out.println("check: attributeSuperfluousVehicleLoadsToGridIfPossible");
						
						System.out.println("hubLoad at beginning");
						Schedule hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(1);
						hubSchedule.printSchedule();
						
						
						System.out.println("agent schedule");
						Schedule agentParkingDrivingSchedule= 
							myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(id);
						agentParkingDrivingSchedule.printSchedule();
						
						//attribute in Parking time - should be successful
						myV2G.attributeSuperfluousVehicleLoadsToGridIfPossible(id, 
								agentParkingDrivingSchedule, 
								new LoadDistributionInterval(0, 
										300, 
										new PolynomialFunction(new double[]{-2000}), 
										false));
						
						System.out.println("hub schedule after attributing -2000 to grid between 0-300");
						hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(1);
						hubSchedule.printSchedule();
						
						//attribute in Driving time - cannot be successful
						myV2G.attributeSuperfluousVehicleLoadsToGridIfPossible(id, 
								agentParkingDrivingSchedule, 
								new LoadDistributionInterval(21600.0, 
										22000, 
										new PolynomialFunction(new double[]{-2000}), 
										false));
						System.out.println("hub schedule after attributing -2000  to grid between 21600-22000 driving time");
						
						hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(1);
						hubSchedule.printSchedule();
						
						lFirst= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(0);
						
						
						assertEquals(hubSchedule.getNumberOfEntries(), 2);
						
						assertEquals(1500.0, lFirst.getPolynomialFunction().getCoefficients()[0]);
						
						
						/**********************************************
						 *  * CHECK
						 * reduceHubLoadByGivenLoadInterval
						 */
						
						System.out.println("CHECK: reduceHubLoadByGivenLoadInterval");
						
						myV2G.reduceHubLoadByGivenLoadInterval(1, 
								new LoadDistributionInterval(21600.0, 
										22000, 
										new PolynomialFunction(new double[]{-2000}), 
										false));
						System.out.println("hub schedule after reducing it by 2000 between 21600-22000 driving time");
						
						hubSchedule=myDecentralizedSmartCharger.myHubLoadReader.stochasticHubLoadDistribution.get(1);
						hubSchedule.printSchedule();
						//bullshit
						//3500+2000=5500
						assertEquals(hubSchedule.getNumberOfEntries(), 4);
						lFirst= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(0);
						lSecond= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(1);
						lThird= 
							(LoadDistributionInterval)hubSchedule.timesInSchedule.get(2);
						assertEquals(1500.0, lFirst.getPolynomialFunction().getCoefficients()[0]);
						assertEquals(3500.0, lSecond.getPolynomialFunction().getCoefficients()[0]);
						assertEquals(5500.0, lThird.getPolynomialFunction().getCoefficients()[0]);
						
						
					}
					
				} catch (Exception e1) {
					
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
							
			}
		});
		controler.run();
		
	}


	
	
	
}


