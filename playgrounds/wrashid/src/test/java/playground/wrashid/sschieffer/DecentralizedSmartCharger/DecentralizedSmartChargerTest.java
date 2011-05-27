/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedSmartChargerTest.java
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
import java.util.LinkedList;

import lpsolve.LpSolveException;

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
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.ChargingInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.DrivingInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.ParkingInterval;
import playground.wrashid.sschieffer.DecentralizedSmartCharger.DSC.Schedule;

/**
 * 
 * tests methods:
 * 
 * <li> size of list of EVs and PHEVs after simulation
 * <li>joulesExtraConsumptionToGasCosts
 * <li>joulesToEmissionInKg
 * <li>calcChargingCost
 * 
 * @author Stella
 *
 */
public class DecentralizedSmartChargerTest extends MatsimTestCase {

	String configPath="test/input/playground/wrashid/sschieffer/config_plans2.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	public Id agentOne=null;
	
	Controler controler; 
	
	final double electrification= 1.0; 
	// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
	final double ev=0.5; 
	
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=15*60;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	
	public static void testMain(String[] args) throws MaxIterationsExceededException, OptimizationException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, IOException {
				
	}
	
	
	/**
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws LpSolveException
	 * @throws OptimizationException
	 * @throws IOException
	 */
	public void testDecentralizedCharger() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
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
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					
					myDecentralizedSmartCharger.run();
					int numPpl= (myDecentralizedSmartCharger.vehicles.getKeySet().size());
					
					
					LinkedList<Id> agentsWithPHEV = myDecentralizedSmartCharger.getAllAgentsWithPHEV();
					System.out.println(1-ev);
					int sizeExp= (int)Math.round((1-ev)*numPpl);
					int sizeAct= agentsWithPHEV.size();
					assertEquals(sizeExp, sizeAct);
					
					LinkedList<Id> agentsWithEV = myDecentralizedSmartCharger.getAllAgentsWithEV();
					sizeExp= (int)Math.round(ev*numPpl);
					sizeAct= agentsWithEV.size();
					assertEquals(sizeExp, sizeAct);
								
					
					/*
					 * 
					 */
					for(Id id : myDecentralizedSmartCharger.vehicles.getKeySet()){
						Schedule testSchedule = mySimulation.makeFakeSchedule();
						
						ParkingInterval p1= (ParkingInterval) testSchedule.timesInSchedule.get(0);
						Schedule pS= new Schedule();
						pS.addTimeInterval(new ChargingInterval(p1.getStartTime(), p1.getEndTime()));
						p1.setChargingSchedule(pS);
						
						ParkingInterval p2= (ParkingInterval) testSchedule.timesInSchedule.get(1);
						Schedule pS2= new Schedule();
						pS2.addTimeInterval(new ChargingInterval(p2.getStartTime(), p2.getEndTime()));
						p2.setChargingSchedule(pS2);
						
						ParkingInterval p3= (ParkingInterval) testSchedule.timesInSchedule.get(3);
						Schedule pS3= new Schedule();
						pS3.addTimeInterval(new ChargingInterval(p3.getStartTime(), p3.getEndTime()));
						p3.setChargingSchedule(pS3);
						
						/*
						 * Parking 0  10  true  joules =100
						 * Parking 10  20 false joules =100
						 * Driving 20  30  consumption =-10
						 * Parking 30  40  false joules =100
						 * final double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
						final double suboptimalPrice=optimalPrice*3; // cost/second  
						 */
						double optimalPrice=0.25*1/1000*1/3600*3500;
						double suboptimalPrice=optimalPrice*3;
						
						double expectedCost=30*optimalPrice;
						double calcCost= myDecentralizedSmartCharger.calculateChargingCostForAgentSchedule(id, testSchedule);
						assertEquals(calcCost, expectedCost);
						if(myDecentralizedSmartCharger.hasAgentEV(id)){
							// extra consumption very expensive
							((DrivingInterval)testSchedule.timesInSchedule.get(2)).setExtraConsumption(10, 10);
							expectedCost=30*optimalPrice+ Double.MAX_VALUE;
							calcCost= myDecentralizedSmartCharger.calculateChargingCostForAgentSchedule(id, testSchedule);
							assertEquals(calcCost, expectedCost);
						}
						if(myDecentralizedSmartCharger.hasAgentPHEV(id)){
							/*DATA in TEstVehicle Collector
							 * double gasPricePerLiter= 0.25; 
							double gasJoulesPerLiter = 43.0*1000000.0;// Benzin 42,7–44,2 MJ/kg
							double emissionPerLiter = 23.2/10; // 23,2kg/10l= xx/mass   1kg=1l*/
							double gasPricePerLiter= 0.25; 
							double gasJoulesPerLiter = 43.0*1000000.0;
							double emissionPerLiter = 23.2/10; 
							
							((DrivingInterval)testSchedule.timesInSchedule.get(2)).setExtraConsumption(gasJoulesPerLiter,0);
							expectedCost=30*optimalPrice+ gasPricePerLiter/0.3;
							calcCost= myDecentralizedSmartCharger.calculateChargingCostForAgentSchedule(id, testSchedule);
							assertEquals(calcCost, expectedCost);
							
							assertEquals(myDecentralizedSmartCharger.joulesExtraConsumptionToGasCosts(id, gasJoulesPerLiter), gasPricePerLiter*1/(0.3));
							
							assertEquals(myDecentralizedSmartCharger.joulesToEmissionInKg(id, gasJoulesPerLiter), emissionPerLiter*1/(0.3));
							
							
						}
											
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


