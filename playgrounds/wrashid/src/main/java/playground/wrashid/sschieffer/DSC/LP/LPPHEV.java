/* *********************************************************************** *
 * project: org.matsim.*
 * LPEV.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.wrashid.sschieffer.DSC.LP;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;


import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;

import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.DrivingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;


import lpsolve.LpSolve;
import lpsolve.LpSolveException;


/**
 * this class conducts the linear programming for PHEVs which results in the required charging times and starting SOC or each agent
 * 
 * from the agent's schedule and given battery constraints, the objective function and constraints are set up
 * - the problem is solved
 * - and the agent's schedules are updated with the results
 * 
 * This class can handle first time scheduling and rescheduling problems 
 * 
 * @author Stella
 *
 */
public class LPPHEV extends LP{
	
	// if the SOC falls below zero, the optimization needs to be iterated...
	
	private boolean iterate=true;
	private double reductionOfSOC=0;
	private int reductionOfSOCStartingAtIntervalI=0;
	
	private int printCount=0;// output only for first 10 agents
	
	public LPPHEV(boolean output){		
		super(output);
	}
	
	
	/**
	 * sets up the LP for EVs and solves the LP-Problem
	 * 
	 * @param schedule daily schedule of agent
	 * @param id  id of agent
	 * @return returns the updated schedule
	 * @throws LpSolveException
	 * @throws IOException 
	 */
	public Schedule solveLP(Schedule schedule, 
			Id id,
			double batterySize, 
			double batteryMin, 
			double batteryMax, 
			String vehicleType) throws LpSolveException, IOException{
		
		iterate=true;
		reductionOfSOC=0;
		reductionOfSOCStartingAtIntervalI=0;
		super.solveLP(schedule, 
				id, 
				batterySize, 
				batteryMin, 
				batteryMax);
		
		int ite=0;
		while (iterate){
			
			iterate=false;			
			
			if(DecentralizedSmartCharger.debug){
				System.out.println("LP PHEV for Agent: "+ id.toString()); 
			}
						
			setUpLP();
			getSolver().setTimeout(100); 
		
			int status = getSolver().solve();
			
	        if(status!=0){
	        	String text = getSolver().getStatustext(status);
	        	if(DecentralizedSmartCharger.debug){
	        		System.out.println("Status text: "+ text);
	    		}
	        	 
	        	// status=0--> OPTIMAL  2 --> INFEASIBLE
	        	return null; 
	        }
			
			try {
				
				if(DecentralizedSmartCharger.debug){
					getSolver().setOutputfile(DecentralizedSmartCharger.outputPath+"DecentralizedCharger/LP/PHEV/LP_agent"+ id.toString()+"printLp.txt");
					getSolver().printLp();
				}
				
				} catch (Exception e) {	    
			}
			
			setSchedule(update());
			
			//if engine used, boolean iterate is set to true and loop 		
			int currentMilli= (int) Math.round((System.currentTimeMillis()%100000.0));
			String name= "SOC agent"+ getPersonId().toString()+"exact"+currentMilli;
			reductionOfSOC=0;
			
			setEnergyFromCombustionEngine(calcEnergyUsageFromCombustionEngine(getSolver().getPtrVariables(), ite) );
			
			ite++;
			
		}
		
		if(isOutput()|| printCount<10){
			String filename= DecentralizedSmartCharger.outputPath+ "DecentralizedCharger/SOC_of_"+vehicleType+"afterLPPHEV_Agent" + id.toString()+".png";
			String filename2= DecentralizedSmartCharger.outputPath+ "DecentralizedCharger/SOC_of_"+vehicleType+"afterLPPHEV_AgentBat" + id.toString()+".png";
			
			visualizeSOCAgentWithAndWithoutNonBattery(getSolver().getPtrVariables(), filename,filename2, id);
			
			printCount++;
			
		}
		
		getSolver().deleteLp();
		
		return getSchedule();
		
	}
	
	
	
	/**
	 * rescheduling problem, by providing a starting SOC, the first entry of the solution vector is fixed. Otherwise the same optimization is performed
	 * @param schedule
	 * @param id
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @param vehicleType
	 * @param startingSOC
	 * @return
	 * @throws LpSolveException
	 * @throws IOException
	 */
	public Schedule solveLPReschedule(Schedule schedule, Id id, double batterySize, double batteryMin, double batteryMax, String vehicleType, double startingSOC) throws LpSolveException, IOException{
		
		if(DecentralizedSmartCharger.debug){
			System.out.println("LP PHEV Resolve for Agent: "+ id.toString()); 			
		}
		super.solveLP(schedule, 
				id, 
				batterySize, 
				batteryMin, 
				batteryMax);
		
		iterate=true;
		reductionOfSOC=0;
		reductionOfSOCStartingAtIntervalI=0;
		
		int ite=0;
		
		while (iterate){
			iterate=false;
			
			setUpLP(startingSOC);
			getSolver().solve();
			
			try {
				
				if(DecentralizedSmartCharger.debug){
					getSolver().setOutputfile(DecentralizedSmartCharger.outputPath+"V2G/LP/PHEV/LP_agent_reschedule"+ id.toString()+"printLp.txt");
					getSolver().printLp();
				}
				if(isOutput()|| id.toString().equals(Integer.toString(1))){
					double currentM= System.currentTimeMillis();
					int currentMilli= (int) Math.round(currentM-(System.currentTimeMillis()%100000.0));
					String filename= DecentralizedSmartCharger.outputPath+ "V2G/SOC_of_"+vehicleType+"afterLPPHEV_Agent" + id.toString()+"_"+currentMilli+".png";
					String filename2= DecentralizedSmartCharger.outputPath+ "V2G/SOC_of_"+vehicleType+"afterLPPHEV_AgentBat" + id.toString()+"_"+currentMilli+".png";
					
					visualizeSOCAgentWithAndWithoutNonBattery(getSolver().getPtrVariables(), filename,filename2, id);
					
				}
				
			} catch (Exception e) {	    
			}
			
			
			if(DecentralizedSmartCharger.debug){
				printSolution();
			}
			
			schedule= update();
			int currentMilli= (int) Math.round((System.currentTimeMillis()%100000.0));
			String name= "SOC agentReschedule"+ getPersonId().toString()+"exact"+currentMilli;
			
			reductionOfSOC=0;
			
			setEnergyFromCombustionEngine(calcEnergyUsageFromCombustionEngine(getSolver().getPtrVariables(),ite));
			
			ite++;
			
		}
		getSolver().deleteLp();
		
		return schedule;
		
	}
	
	/**
	 * sets objective, inequalities and bounds on solution
	 * @param schedule
	 * @param id
	 * @throws LpSolveException
	 */
	private void setUpLP() throws LpSolveException{		
		
		setObjectiveFunction();
		
		setInequalityContraintsForBatteryUpper(reductionOfSOC, reductionOfSOCStartingAtIntervalI);
		
		//upper & lower bounds
		setLowerAndUpperBounds();		
		
	}
	
	
	
	private void setUpLP(double startingSOC) throws LpSolveException{
		
		
		setObjectiveFunction();
		
		setInequalityContraintsForBatteryUpper(reductionOfSOC, reductionOfSOCStartingAtIntervalI);
				
		//upper & lower bounds
		setLowerAndUpperBoundsWithStartingSOC(startingSOC);
		
		
	}
	
	
	
	
	
	
	/**
	 * goes over solution of LP 
	 * calculates SOC after each time interval
	 * 
	 * if SOC is below 0 (or lower than the last SOC below 0) , then obviously energy was charged from the engine
	 * the total Energy from Engine is computed and returned;
	 * 
	 * Knowing in which interval how much and how long needs to be charged from the engine,
	 * parking and driving times are corrected;
	 * parking times-->  the required charging times is adjusted;
	 * driving times --> consumption from engine for an interval is reduced 
	 * and instead the term consumption from Engine is increased 
	 * 
	 * 
	 * @return
	 * @throws LpSolveException
	 * @throws IOException 
	 */
	public double calcEnergyUsageFromCombustionEngine(double [] solution,  int ite) throws LpSolveException, IOException{
				
		EnergyFromEngineCheckPHEV eCalc=new EnergyFromEngineCheckPHEV();
		
		eCalc.run(getSchedule(), 
				solution, 
				reductionOfSOCStartingAtIntervalI);
		
		iterate=eCalc.isIterate();
		reductionOfSOCStartingAtIntervalI=eCalc.getReductionOfSOCStartingAtInterval();
		reductionOfSOC=eCalc.getReductionOfSOC();
		if (ite>=getSchedule().numberOfDrivingTimes()||iterate==false){
			setSchedule(eCalc.getWorkingSchedule());
			iterate=false;
		}
		
		return eCalc.getTotalEnergyFromEngine();
		
	}
	
	
	
	

	
	
	
	
		
	
}
