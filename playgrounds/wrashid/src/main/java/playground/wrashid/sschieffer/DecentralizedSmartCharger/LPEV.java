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
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

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



import lpsolve.LpSolve;
import lpsolve.LpSolveException;

/**
 * this class conducts the linear programming for EVs which results in the required charging times and starting SOC or each agent
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
public class LPEV extends LP{
	
	private double buffer;	
		
	public LPEV(double buffer, boolean output){
		super(output);
		this.buffer=buffer;
		
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
		
		super.solveLP(schedule, 
				id, 
				batterySize, 
				batteryMin, 
				batteryMax);
		
		if(DecentralizedSmartCharger.debug){
			System.out.println("LP EV for Agent: "+ id.toString()); 
		}
		
		setUpLP();
		
		int status = getSolver().solve();
        
        if(status!=0){
        	
        	if(DecentralizedSmartCharger.debug){
        		String text = getSolver().getStatustext(status);
        		System.out.println("Status text: "+ text); 
    		}
        	// status=0--> OPTIMAL  2 --> INFEASIBLE
        	return null; 
        }
        
      
		
		try {
			if(DecentralizedSmartCharger.debug){
				getSolver().setOutputfile(DecentralizedSmartCharger.outputPath+"DecentralizedCharger\\LP\\EV\\LP_agent"+ id.toString()+"printLp.txt");
				getSolver().printLp();
			}
			
			
			} catch (Exception e) {	    
		}
		
		schedule= update();
	
		if(isOutput()|| id.toString().equals(Integer.toString(1))){
			visualizeSOCAgent(getSolver().getPtrVariables(), vehicleType, id);
			
		}
		
		getSolver().deleteLp();
		
		return schedule;
		
		
	}
	
	
	
	
	
	public Schedule solveLPReschedule(Schedule schedule, 
			Id id, 
			double batterySize, double batteryMin, double batteryMax, 
			String vehicleType, 
			double startingSOC) throws LpSolveException, IOException{
		
		
		super.solveLP(schedule, 
				id, 
				batterySize, 
				batteryMin, 
				batteryMax);
		
		if(DecentralizedSmartCharger.debug){
			System.out.println("LP EV Resolve for Agent: "+ id.toString()); 
			/*schedule.printSchedule();*/
		}
		
		
		setUpLP(startingSOC);
		int status = getSolver().solve();
        
        if(status!=0){
        	if(DecentralizedSmartCharger.debug){
        		String text =getSolver().getStatustext(status);
            	System.out.println("Status text: "+ text); 
            	// status=0--> OPTIMAL
            	// 2 --> INFEASIBLE
    		}
        	
        	return null; 
        }
		
		try {
			
			if(DecentralizedSmartCharger.debug){
				getSolver().setOutputfile(DecentralizedSmartCharger.outputPath+"V2G\\LP\\EV\\LP_agent_reschedule"+ id.toString()+"printLp.txt");
				getSolver().printLp();
			}
			
		
		} catch (Exception e) {	    
		}
		
		schedule= update();

		
		getSolver().deleteLp();
		
		return schedule;
		
		
	}
	
	
	
	
	/**
	 * sets objective, inequalities and bounds on solution
	 * @param schedule
	 * @param id
	 * @throws LpSolveException
	 */
	public void setUpLP() throws LpSolveException{
		
		setObjectiveFunction();
		
		setInequalityContraintsForBatteryUpperLower();
		
		// setDrivingConsumption < SOC +buffer
		for(int i=0; i<getSchedule().numberOfDrivingTimes();i++){
			setDrivingConsumptionSmallerSOC(i,  buffer);
			
		}
		
		//upper & lower bounds
		setLowerAndUpperBounds();
		
		
	}
	
	
	
		
	
	private void setUpLP(double startingSOC) throws LpSolveException{
		
		setObjectiveFunction();
		
		setInequalityContraintsForBatteryUpperLower();
		
		// setDrivingConsumptionSmallerSOC Inequality
		for(int i=0; i<getSchedule().numberOfDrivingTimes();i++){
			setDrivingConsumptionSmallerSOC(i,  buffer);
			
		}
		
		
		//upper & lower bounds
		setLowerAndUpperBoundsWithStartingSOC(startingSOC);
		
		
		
	}
	
	
	
	
	/**
	 * sets objective function
	 * 
	 * minimizing time in peak hours
	 * minimizing (-)*charging in off peak hours
	 * @throws LpSolveException
	 */
	private void setOldObjectiveFunction() throws LpSolveException{
		String objectiveStr="-1 ";// first entry for SOC, maximize starting SOC. minimize -(SOC)
		
		
		// peakTimes 1 else 0 
		
		for(int i=0; i<getSchedule().timesInSchedule.size(); i++){
			// if Parking interval
			if(getSchedule().timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)getSchedule().timesInSchedule.get(i);
				if(thisParkingInterval.isInSystemOptimalChargingTime()){
					
					// want to maximize charging in this time. thus need to minimize the negative of the weight
					double weightOptimal=calculateWeightOptimal(thisParkingInterval);
					objectiveStr=objectiveStr.concat(weightOptimal +" ");
					}
				
				else{
					// want to minimize charging in this time. thus need to minimize the absolute value of the weight
					double weightSubOptimal=calculateWeightSubOptimal(thisParkingInterval);
					
					objectiveStr=objectiveStr.concat(weightSubOptimal +" ");
				}
			}else{
				objectiveStr=objectiveStr.concat("0 ");
			}
			}
		
	
		getSolver().strSetObjFn(objectiveStr);	
		
		getSolver().setMinim(); //minimize the objective
	}
	

	
	
	
	
	
	
	
	
	
	
	/**
	 * adds constraint to LP problem
	 * (1  charging speed  -consumption charging speed 0 0 )* x  >= (1+buffer)* nextE consumption
	 * @param a
	 * @param buffer
	 * @throws LpSolveException
	 */
	
	private void setDrivingConsumptionSmallerSOC(int a, double buffer) throws LpSolveException{
		String objectiveStr="1 ";// first entry for SOC
		int pos=getSchedule().positionOfIthDrivingTime(a);
		
		for(int i=0; i<getSchedule().timesInSchedule.size(); i++){
			if(i<pos){
				if(getSchedule().timesInSchedule.get(i).isParking()){
					objectiveStr=objectiveStr.concat(Double.toString(((ParkingInterval)getSchedule().timesInSchedule.get(i)).getChargingSpeed()) 
							+ " ");
				}
				
				if(getSchedule().timesInSchedule.get(i).isDriving()){
					objectiveStr=objectiveStr.concat(Double.toString(
							((DrivingInterval)getSchedule().timesInSchedule.get(i)).getConsumption()*(-1))+ " ");
				}
				
			}else{
				objectiveStr=objectiveStr.concat("0 ");
			}
		}
		
		DrivingInterval d= (DrivingInterval)getSchedule().timesInSchedule.get(pos);
		getSolver().strAddConstraint(objectiveStr, 
				LpSolve.GE, 
				(1+buffer)*d.getConsumption());
	}
	
	
	
	
	
	
	
}
