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
package playground.wrashid.sschieffer.DSC;

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
		
		super.solveLP(schedule, 
				id, 
				batterySize, 
				batteryMin, 
				batteryMax);
		
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
		
		schedule= update();
		if(DecentralizedSmartCharger.debug){
			printSolution();
		}
				
		setEnergyFromCombustionEngine(calcEnergyUsageFromCombustionEngine(getSolver().getPtrVariables()));
		
		if(isOutput()|| id.toString().equals(Integer.toString(1)) ){
			String filename= DecentralizedSmartCharger.outputPath+ "DecentralizedCharger/SOC_of_"+vehicleType+"afterLPPHEV_Agent" + id.toString()+".png";
			visualizeSOCAgent(getSolver().getPtrVariables(),filename, id);
			
			
		}
		
		getSolver().deleteLp();
		
		return schedule;
		
		
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
			/*System.out.println("Schedule before LPPHEV: "); 
			schedule.printSchedule();*/
		}
		
		super.solveLP(schedule, 
				id, 
				batterySize, 
				batteryMin, 
				batteryMax);
		
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
				String filename= DecentralizedSmartCharger.outputPath+ "V2G/SOC_of_"+vehicleType+"afterLPPHEV_Agent" + id.toString()+currentMilli+".png";
				visualizeSOCAgent(getSolver().getPtrVariables(), filename, id);
				
			}
			
			
		} catch (Exception e) {	    
		}
		
		
		if(DecentralizedSmartCharger.debug){
			printSolution();
		}
		
		schedule= update();
		/*if(DecentralizedSmartCharger.debug){
			//System.out.println("Schedule after update LPPHEV: ");
			//schedule.printSchedule();
		}*/
		
		setEnergyFromCombustionEngine(calcEnergyUsageFromCombustionEngine(getSolver().getPtrVariables()));
		
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
		
		setInequalityContraintsForBatteryUpper();
		
		//upper & lower bounds
		setLowerAndUpperBounds();		
		
	}
	
	
	
	private void setUpLP(double startingSOC) throws LpSolveException{
		
		
		setObjectiveFunction();
		
		setInequalityContraintsForBatteryUpper();
				
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
	 */
	public double calcEnergyUsageFromCombustionEngine(double [] solution) throws LpSolveException{
		
		boolean status;
		
		double energyFromEngine=0;
		
		double statusJoule;
		
		double lastFalseJoule=0;		
		
		statusJoule=solution[0];
		
		if(statusJoule>=0){
			status=true;
			}else{
				status=false;
			}
		
		for(int i=0; i<getSchedule().getNumberOfEntries(); i++){
			if(getSchedule().timesInSchedule.get(i).isParking()){
				
				ParkingInterval thisP= (ParkingInterval)getSchedule().timesInSchedule.get(i);
				
				statusJoule+=thisP.getChargingSpeed()
							*solution[1+i];// charging speed * time= joules
				
				if(statusJoule>=0){
					status=true;
					}else{
						status=false;
					}
				
			}else{//Driving
				DrivingInterval thisD = (DrivingInterval)getSchedule().timesInSchedule.get(i);
							
				statusJoule+=(-1)*(thisD).getBatteryConsumption();
				
				if(statusJoule<0 && status==true){
					
					energyFromEngine=Math.abs(statusJoule);
					status=false;
					lastFalseJoule=statusJoule;
					
					ParkingInterval precedingP;//only need it for charging speed
					if(i>0){
						precedingP= (ParkingInterval)getSchedule().timesInSchedule.get(i-1);	
					}else{
						//if first entry.. then previous parking interval must have been last one on previous day
						precedingP= (ParkingInterval)getSchedule().timesInSchedule.get(getSchedule().getNumberOfEntries()-1);	
					}
					
					updatePrecedingParkingAndDrivingInterval(getSchedule(), 
							i, energyFromEngine, 
							thisD, precedingP);		 
					 
				}else{
					
					if(statusJoule<0 && status==false){
						energyFromEngine=Math.abs(statusJoule);
						energyFromEngine+=Math.abs(statusJoule-lastFalseJoule);
						
						ParkingInterval precedingP= (ParkingInterval)getSchedule().timesInSchedule.get(i-1);
						
						updatePrecedingParkingAndDrivingInterval(getSchedule(), i, energyFromEngine, thisD, precedingP);
					}
					
				}
			}
		}
		
		return energyFromEngine;
	}
	
	
	
	
	/**
	 * 
	 * @param s
	 * @param pos
	 * @param energyFromEngine
	 * @param thisD
	 * @param precedingP
	 */
	public void updatePrecedingParkingAndDrivingInterval(Schedule s, int pos, double energyFromEngine, DrivingInterval thisD, ParkingInterval precedingP){
		double engineTime=energyFromEngine/
		( precedingP).getChargingSpeed();
		
		s.reducePrecedingParkingBy( pos, engineTime);
		
		s.addExtraConsumptionDriving( pos, engineTime, energyFromEngine);
		
		
	}
	
	
	
	
	
	
	
		
		
	
}
