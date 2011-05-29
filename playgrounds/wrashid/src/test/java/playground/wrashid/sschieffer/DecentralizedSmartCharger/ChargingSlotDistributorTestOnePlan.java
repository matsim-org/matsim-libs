/* *********************************************************************** *
 * project: org.matsim.*
 * ChargingSlotDistributorTestOnePlan.java
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
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.DSC.ParkingInterval;
import playground.wrashid.sschieffer.DSC.Schedule;
import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * <li>checks if full required charging time calculated by linear programming is 
 * attributed to agent's charging schedule
 * 
 * @author Stella
 *
 */
public class ChargingSlotDistributorTestOnePlan extends TestCase{

	
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	public Id agentOne=null;
	
	Controler controler; 
	
	final double electrification= 1.0; 
	// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
	final double ev=1.0;
		
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=15*60;
	
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
	public void testAgentTimeIntervalReader() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		
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
					
					for(Id id : myDecentralizedSmartCharger.vehicles.getKeySet()){
						
						agentOne=id;						
						
						
						myDecentralizedSmartCharger.run();						
						Schedule parkingDrivingTimes= new Schedule();
						parkingDrivingTimes= myDecentralizedSmartCharger.getAllAgentParkingAndDrivingSchedules().get(id);
						System.out.println("parking & driving times");
						parkingDrivingTimes.printSchedule();
						
						
						/**
						 * TOTAL CHARGING TIME CALCULATION
						 */
						double totalChargingTimeParkingDriving=0;
						for(int i=0; i< parkingDrivingTimes.getNumberOfEntries();i++){
							if (parkingDrivingTimes.timesInSchedule.get(i).isParking()){
								ParkingInterval p=(ParkingInterval)parkingDrivingTimes.timesInSchedule.get(i);
								
								totalChargingTimeParkingDriving+=p.getRequiredChargingDuration();
							}
						}
						
						System.out.println("CHECK Charging Length");
						System.out.println(totalChargingTimeParkingDriving);
						System.out.println(
								myDecentralizedSmartCharger.getAgentChargingSchedule(id).getTotalTimeOfIntervalsInSchedule());
						
						assertEquals(
								Math.abs(myDecentralizedSmartCharger.getAgentChargingSchedule(id).getTotalTimeOfIntervalsInSchedule()-
								totalChargingTimeParkingDriving)<0.0001, true
								);
						
						
						
						
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


