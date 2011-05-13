/* *********************************************************************** *
 * project: org.matsim.*
 * AgentTimeIntervalReaderTestOnePlan.java
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
import org.matsim.testcases.MatsimTestCase;

import playground.wrashid.PSF.data.HubLinkMapping;
import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.EventHandlerAtStartupAdder;
import playground.wrashid.lib.obj.LinkedListValueHashMap;
import junit.framework.TestCase;
import lpsolve.LpSolveException;

/**
 * tests if
 *  <li>parking times are read in correctly for first agent 
 *  in readParkingTimes method
	<li>driving times are read in correctly from addDrivingTimes method
	<li>intervals in schedule are correctly distinguished into optimal and suboptimal time intervals according to the determistic hubload
	in checkTimesWithHubSubAndOptimalTimes method
 * <li> Joules for intervals are calculated correctly in getJoulesForEachParkingInterval
 * using config_plans1.xml
 * 
 * @author Stella
 *
 */
public class AgentTimeIntervalReaderTestOnePlan extends MatsimTestCase{

	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	public Id agentOne=null;
	
	Controler controler; 
	
	final double phev=1.0;
	final double ev=0.0;
	final double combustion=0.0;
	
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=5*60;
	
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
				phev, 
				ev, 
				combustion);
		
		controler= mySimulation.getControler();
		
		controler.addControlerListener(new IterationEndsListener() {
			
			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				
				try {
					
					
					DecentralizedSmartCharger myDecentralizedSmartCharger = mySimulation.setUpSmartCharger(
							outputPath,
							bufferBatteryCharge,
							standardChargingSlotLength);
					
					/***********************************
					 * AGENTTIMEINTERVALREADER
					 * *********************************
					 */
					
					for(Id id : controler.getPopulation().getPersons().keySet()){
						agentOne=id;						
						System.out.println(id.toString());
						
						Schedule parkingTimes= new Schedule();
						parkingTimes= myDecentralizedSmartCharger.myAgentTimeReader.readParkingTimes(id, parkingTimes);
						System.out.println("parking times");
						parkingTimes.printSchedule();
						/**
						 * Parking Interval 	 start: 0.0	  end: 21600.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							Parking Interval 	 start: 22500.0	  end: 35700.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
							Parking Interval 	 start: 38040.0	  end: 86400.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0

						 */
						assertEquals(3, parkingTimes.getNumberOfEntries());
						assertEquals(parkingTimes.timesInSchedule.get(0).getIntervalLength(), 6*3600.0);
						assertEquals(parkingTimes.timesInSchedule.get(1).getIntervalLength(), (40.0)*60.0+3*3600.0);
						
						
						parkingTimes= myDecentralizedSmartCharger.myAgentTimeReader.addDrivingTimes(id, parkingTimes);
						System.out.println("parking + driving times");
						parkingTimes.printSchedule();
						assertEquals(5, parkingTimes.getNumberOfEntries());
						assertEquals(parkingTimes.timesInSchedule.get(0).getIntervalLength(), 6*3600.0);
						assertEquals(parkingTimes.timesInSchedule.get(1).getIntervalLength(), 22500.0-21600.0);
						assertEquals(parkingTimes.timesInSchedule.get(2).getIntervalLength(), (40.0)*60.0+3*3600.0);
						/**
						 * Parking Interval 	 start: 0.0	  end: 21600.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
						Driving Interval 	  start: 21600.0	  end: 22500.0	  consumption: 1.8786265875308476E7
						Parking Interval 	 start: 22500.0	  end: 35700.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0
						Driving Interval 	  start: 35700.0	  end: 38040.0	  consumption: 4.879471387207076E7
						Parking Interval 	 start: 38040.0	  end: 86400.0	  ChargingTime:  -1.0	  Optimal:  false	  Joules per Interval:  0.0	  ChargingSchedule:  0

						 */
						
						
						   /*
						    * 0.0 -- 62490.0    10
						      62490.0 -- Ende  -10	
						    */
						parkingTimes = myDecentralizedSmartCharger.myAgentTimeReader.checkTimesWithHubSubAndOptimalTimes(parkingTimes, id);
						System.out.println("parking + driving times crosschecked with hub optimal");
						parkingTimes.printSchedule();
						
						assertEquals(6, parkingTimes.getNumberOfEntries());
						assertEquals(parkingTimes.timesInSchedule.get(0).getIntervalLength(), 6*3600.0);
						assertEquals(parkingTimes.timesInSchedule.get(1).getIntervalLength(), 22500.0-21600.0);
						assertEquals(parkingTimes.timesInSchedule.get(2).getIntervalLength(), (40.0)*60.0+3*3600.0);
						assertEquals(parkingTimes.timesInSchedule.get(5).getIntervalLength(), 24*3600.0-62490.0);
						
						
						
						//***************
						// check joules  Function 10 or /-10 in last interval
						parkingTimes = myDecentralizedSmartCharger.myAgentTimeReader.getJoulesForEachParkingInterval(id, parkingTimes);
						
						TimeInterval tFirst= parkingTimes.timesInSchedule.get(0);
						TimeInterval tLast= parkingTimes.timesInSchedule.get(5);
						assertEquals(tFirst.isParking(), true);
						assertEquals(tLast.isParking(), true);
						ParkingInterval pFirst= (ParkingInterval) tFirst;
						ParkingInterval pLast= (ParkingInterval) tLast;
						
						assertEquals(pFirst.isInSystemOptimalChargingTime(), true);
						assertEquals(pLast.isInSystemOptimalChargingTime(), false);
						
						assertEquals(pFirst.getJoulesInInterval(), 10.0*pFirst.getIntervalLength());
						assertEquals(pLast.getJoulesInInterval(), -1000000.0*pLast.getIntervalLength());
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


