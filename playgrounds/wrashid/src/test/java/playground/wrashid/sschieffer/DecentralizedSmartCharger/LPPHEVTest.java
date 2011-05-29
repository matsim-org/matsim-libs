/* *********************************************************************** *
 * project: org.matsim.*
 * LPPHEVTest.java
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.StringTokenizer;

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
import playground.wrashid.sschieffer.DSC.DrivingInterval;
import playground.wrashid.sschieffer.DSC.LPPHEV;
import playground.wrashid.sschieffer.DSC.ParkingInterval;
import playground.wrashid.sschieffer.DSC.Schedule;
import junit.framework.TestCase;
import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 * 1) at example schedule, test if the LP PHEV is set up correctly
 *<li>objective
 *<li>(in)equalities
 *<li>bounds
 *
 *2) checks if calcEnergyUsageFromCombustionEngine returns correct result for mock case
 *
 * @author Stella
 *
 */
public class LPPHEVTest extends TestCase{

	private Schedule sTestEnergyCalc;
	
	String configPath="test/input/playground/wrashid/sschieffer/config_plans1.xml";
	final String outputPath ="D:\\ETH\\MasterThesis\\TestOutput\\";
	
	final double electrification= 1.0; 
	// rate of Evs in the system - if ev =0% then phev= 100-0%=100%
	final double ev=0.0; 
	
	
	private double chargingSpeed=3500.0;
	
	private TestSimulationSetUp mySimulation;
	private Controler controler;
	
	final double bufferBatteryCharge=0.0;
	
	final double standardChargingSlotLength=15*60;
	
	public static DecentralizedSmartCharger myDecentralizedSmartCharger;
	
	final static double optimalPrice=0.25*1/1000*1/3600*3500;//0.25 CHF per kWh		
	final static double suboptimalPrice=optimalPrice*3; // cost/second  
	
	public Id agentOne;
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
	public void testLPPHEV() throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, LpSolveException, OptimizationException, IOException{
		
		mySimulation = new TestSimulationSetUp(
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
					
					/***********************************
					 * LP TEST
					 * *********************************
					 */
					
					
					for(Id id : myDecentralizedSmartCharger.vehicles.getKeySet()){
						
							agentOne=id;
														
							Schedule testSchedule = mySimulation.makeFakeSchedule();
							
							testSchedule=myDecentralizedSmartCharger.getLPPHEV().solveLP(testSchedule, 
									id, 
									100, 
									0.1,
									0.9, 
									"lpphevTEST"
									);
							
							testSchedule.printSchedule();
							String name= outputPath+"DecentralizedCharger\\LP\\PHEV\\LP_agent"+ id.toString()+"printLp.txt";
							
							 try
							    {
								 FileReader fro = new FileReader( name );
							       BufferedReader bro = new BufferedReader( fro );
							       
							       // declare String variable and prime the read
							       String stringRead = bro.readLine( ); // model name
							       stringRead = bro.readLine( ); // C1 C2...
							       
							       //*********************
							       String objStringRead = bro.readLine( );  
							       StringTokenizer st = new StringTokenizer(objStringRead);
							       st.nextToken(); // minimize
							       
							       
							       /*
							        * OBJECTIVE FUNCTION
							        * /*
									 * /*
										/*
									 * Parking 0  10  true  joules =100
									 * Parking 10  20 false joules =100
									 * Driving 20  30  consumption =1
									 * Parking 30  40  false joules =100
									 */
									 
							       
							       //SOC=-2
							       String next= st.nextToken(); 
							       System.out.println(next);
							       							       
							       assertEquals(Integer.toString(-2), next);
							       /*
							        * optimal weight
							        * (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
							        * 
							        */
							       
							       next= st.nextToken(); 
							       System.out.println(next);
							       double expected= -1.0*100.0/100.0 - 2*3500.0;							       
							       int expectedInt = (int)expected;
							       assertEquals(Integer.toString(expectedInt), next);
							       /*
							        * Parking suboptimal
							        * thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
							        * 
							        */
							       next= st.nextToken(); 
							       System.out.println(next);
							       expected= - 2*3500.0+100.0/200.0;
							       assertEquals(Double.toString(expected), next);
								     /*
								      * Driving  = btteryconsumption
								      * objectiveToMinimizeCombustionEngineUse
								      */
							       next= st.nextToken(); 
							       System.out.println(next);
							       assertEquals(Integer.toString(1),  next);
							       
							       /*
							        * Parking suboptimal
							        * thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
							        *  
							        */
							       next= st.nextToken(); 
							       System.out.println(next);							       
							       expected= 100.0/200.0;							       
							       assertEquals(Double.toString(expected), next);
							       
							     //*********************							      
							       //R1               1     3500        0        0        0        0        0 <=       90
							       String constraint1 = bro.readLine( ); 
							       st = new StringTokenizer(constraint1);
							       next= st.nextToken(); 							      
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(3500), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							      
							       
							       //*********************
							       String constraint2 = bro.readLine( );//*********************
							       String constraint3 = bro.readLine( );//*********************
							       String constraint4 = bro.readLine( );//*********************
							       String constraint5 = bro.readLine( ); //*********************
								  
							     //*********************
							       String upBo = bro.readLine();
							       //upbo            90    21600        1    13200        1    24450    23910
							       st = new StringTokenizer(upBo);
							       st.nextToken();
							       assertEquals(Integer.toString(90), st.nextToken());
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(10), st.nextToken());
							     
							       //*********************	
							       
							       String lowBo = bro.readLine( );
							       //lowbo           10        0        1        0        1        0        0 
							       st = new StringTokenizer(lowBo);
							       st.nextToken();
							       assertEquals(Integer.toString(10), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							       assertEquals(Integer.toString(1), st.nextToken());
							       assertEquals(Integer.toString(0), st.nextToken());
							      
							       
							       bro.close( );  
							       
							       
							    }
							 
							    catch( FileNotFoundException filenotfoundexxption )
							    {
							      System.out.println( name +" , does not exist" );
							    }
							 
							    catch( IOException ioexception )
							    {
							      ioexception.printStackTrace( );
							    }
						}
					
					/*
					 * RESOLVE
					 */
					for(Id id : myDecentralizedSmartCharger.vehicles.getKeySet()){
						
						agentOne=id;
						
						Schedule testSchedule = mySimulation.makeFakeSchedule();
						double startingSOC=75.0;
						testSchedule=myDecentralizedSmartCharger.getLPPHEV().solveLPReschedule(
								testSchedule, 
								id, 
								100, 
								0.1,
								0.9, 
								"lpphevTEST",
								startingSOC
								);
						
						testSchedule.printSchedule();
						String name= outputPath+"DecentralizedCharger\\LP\\PHEV\\LP_agent_reschedule"+ id.toString()+"printLp.txt";
						
						try
						    {
						       FileReader fro = new FileReader( name );
						       BufferedReader bro = new BufferedReader( fro );
						       
						       // declare String variable and prime the read
						       String stringRead = bro.readLine( ); // model name
						       stringRead = bro.readLine( ); // C1 C2...
						       
						       //*********************
						       String objStringRead = bro.readLine( );  
						       // Minimize        -1 -0.0434251        0 -0.146437        0 -0.810138        1 
						       StringTokenizer st = new StringTokenizer(objStringRead);
						       st.nextToken(); // minimize
						       
						       
						       /*
						        * OBJECTIVE FUNCTION
						        * /*
								 * /*
									 * Parking 0  10  true  joules =100
									 * Parking 10  20 false joules =100
									 * Driving 20  30  consumption =-10
									 * Parking 30  40  false joules =100
									 *
								 */
						
						       
						       //SOC=-1  +  minimize time with SOC<0  -1
						       String next= st.nextToken(); //-2
						       System.out.println(next);
						       							       
						       assertEquals(Integer.toString(-2), next);
						       /*
						        * optimal weight
						        * (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
						        * +
						        * minimize time with SOC<0  - chargingSpeed
						        */
						       
						       next= st.nextToken(); 
						       System.out.println(next);
						       double expected= -1.0*100.0/100.0 - chargingSpeed;							       
						       int expectedInt = (int)expected;
						       assertEquals(Integer.toString(expectedInt), next);
						       /*
						        * Parking suboptimal
						        * thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
						        * +
						        * minimize time with SOC<0  - chargingSpeed
						        */
						       next= st.nextToken(); 
						       System.out.println(next);
						       expected= 100.0/200.0 - chargingSpeed;
						       assertEquals(Double.toString(expected), next);
							     /*
							      * Driving 0								      * +
							      * minimize time with SOC<0  + Energyout=1
							      */
						       next= st.nextToken(); 
						       assertEquals(Integer.toString(1), next);
						       
						       /*
						        * Parking suboptimal
						        * thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
						        *   * +
						        * minimize time with SOC<0  =0 because after driving interval
						        */
						       next= st.nextToken(); 
						       System.out.println(next);
						       expected= 100.0/200.0;							       
						       assertEquals(Double.toString(expected), next);
						       
						     //*********************
						       String constraint1 = bro.readLine( ); 
						       //R1               1     3500        0        0        0        <=       90
						       
						       st = new StringTokenizer(constraint1);
						       st.nextToken();
						       assertEquals(Integer.toString(1), st.nextToken());
						       assertEquals(Integer.toString(3500), st.nextToken());
						       assertEquals(Integer.toString(0), st.nextToken());
						       assertEquals(Integer.toString(0), st.nextToken());
						      
						       
						       //*********************
						       String constraint2 = bro.readLine( );// R2               1     3500        0        0        0        0        0 >=       10
						       //*********************
						       String constraint3 = bro.readLine( );//R3               1     3500 -1.87863e+007        0        0        0        0 <=       90
						       //*********************
						       String constraint4 = bro.readLine( );//R4               1     3500 -1.87863e+007        0        0        0        0 >=       10
						       //*********************
						       //*********************
						       stringRead = bro.readLine( ); // Type..
						       //*********************
						       String upBo = bro.readLine();
						       //upbo            90    21600        1    13200        1    24450    23910
						       st = new StringTokenizer(upBo);
						       st.nextToken();
						       assertEquals(Integer.toString((int)startingSOC), st.nextToken());
						       assertEquals(Integer.toString(10), st.nextToken());
						       assertEquals(Integer.toString(10), st.nextToken());
						       assertEquals(Integer.toString(1), st.nextToken());
						       assertEquals(Integer.toString(10), st.nextToken());
						     
						       //*********************	
						       
						       String lowBo = bro.readLine( );
						       //lowbo           10        0        1        0        1        0        0 
						       st = new StringTokenizer(lowBo);
						       st.nextToken();
						       assertEquals(Integer.toString((int)startingSOC), st.nextToken());
						       assertEquals(Integer.toString(0), st.nextToken());
						       assertEquals(Integer.toString(0), st.nextToken());
						       assertEquals(Integer.toString(1), st.nextToken());
						       assertEquals(Integer.toString(0), st.nextToken());
						      
						       							       
						       bro.close( );
						    }
						 
						    catch( FileNotFoundException filenotfoundexxption )
						    {
						      System.out.println( name +" , does not exist" );
						    }
						 
						    catch( IOException ioexception )
						    {
						      ioexception.printStackTrace( );
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
	
	
	
		
	/*
	 * calcEnergyUsageFromCombustionEngine
	 * check energy use from engine/battery for one case
	 */
	public void testLPPHEVEnergyUsageFromCombustionEngine() throws LpSolveException{
		LPPHEV lp= new LPPHEV(false);
		double [] solution = setUpEnergyUsageFromCombustionEngine();
		lp.setSchedule(sTestEnergyCalc);
		double eEngine= lp.calcEnergyUsageFromCombustionEngine(solution);
		assertEquals(eEngine, 15000.0);
		// * parking times-->  the required charging times is adjusted;
		// dependent on charing speed.. currently 3500 but should be flexible in future..
		// so not implemented right now test for this
		
		
		// * driving times --> consumption from engine for an interval is reduced 
		DrivingInterval d= (DrivingInterval) sTestEnergyCalc.timesInSchedule.get(1);
		
		assertEquals(15000.0, d.getExtraConsumption());
		assertEquals(35000.0, d.getBatteryConsumption());
		
	}
	
	
	
	
	public double [] setUpEnergyUsageFromCombustionEngine(){
		
		sTestEnergyCalc= new Schedule();
		
		sTestEnergyCalc.addTimeInterval(new ParkingInterval(0, 10, null));
		
		sTestEnergyCalc.addTimeInterval(new DrivingInterval(10, 20, 50000));
		
		sTestEnergyCalc.addTimeInterval(new ParkingInterval(20, 30, null));
		sTestEnergyCalc.addTimeInterval(new ParkingInterval(30, 40, null));
		
		for (int i=0; i<sTestEnergyCalc.getNumberOfEntries(); i++){
			if (sTestEnergyCalc.timesInSchedule.get(i).isParking()){
				
				ParkingInterval p= (ParkingInterval) sTestEnergyCalc.timesInSchedule.get(i);
				p.setRequiredChargingDuration(0.0);
			}
		}
		
		double[] solution={0.0, 10.0, 1.0, 0.0, 0.0};
		return solution;
	}

	
}


