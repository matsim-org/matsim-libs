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

import org.matsim.api.core.v01.Id;



import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class LPEV {
	
	private Schedule schedule;
	private LpSolve solver; 
	private int numberOfVariables;
	private Id personId;
	
	public LPEV(){
		
	}
	
	
	
	/**
	 * sets up the LP for EVs and solves the LP-Problem
	 * 
	 * @param schedule daily schedule of agent
	 * @param id  id of agent
	 * @return returns the updated schedule
	 * @throws LpSolveException
	 */
	public Schedule solveLP(Schedule schedule, Id id, double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
		
		setUpLP(schedule, id, batterySize, batteryMin, batteryMax);
		int status = solver.solve();
        
        if(status!=0){
        	String text = solver.getStatustext(status);
        	System.out.println("Status text: "+ text); 
        	// status=0--> OPTIMAL
        	// 2 --> INFEASIBLE
        	return null; 
        }
        
      
		
		try {
			
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\EV\\LPEV_V1G_agent"+ personId.toString()+"printLp.txt");
			solver.printLp();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\EV\\LPEV_V1G_agent"+ personId.toString()+"objective.txt");
			solver.printObjective();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\EV\\LPEV_V1G_agent"+ personId.toString()+"tableau.txt");
			solver.printTableau();
		} catch (Exception e) {	    
		}
		
		
		schedule= update();
		System.out.println("updated schedule with required charging times:");
		schedule.printSchedule();
		
		printLPSolution();
		
		solver.deleteLp();
		
		return schedule;
		
		
	}
	
	
	
	/**
	 * sets objective, inequalities and bounds on solution
	 * @param schedule
	 * @param id
	 * @throws LpSolveException
	 */
	public void setUpLP(Schedule schedule, Id id, double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
		this.schedule=schedule;
		personId=id;
		
		
		double buffer=0.0;
		/*
		 * TODObattery buffer... related to contract type
		and EV things not working... wrong impId???
		 * TODO
		 * TODO
		 * TODO
		 * =Main.vehicles.getValue(personId).
		 */
		
		System.out.println("LP summary for agent"+ id.toString());
		System.out.println("batterySize"+ batterySize+ " \t batteryMin "+ batteryMin+ " \t batteryMax (default)"+ batteryMax);
		
		
		
		numberOfVariables= schedule.getNumberOfEntries()+1;
		
		solver = LpSolve.makeLp(0, numberOfVariables);
		
		setObjectiveFunction();
		
		
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			String inequality=setInEqualityBatteryConstraint(i);
			solver.strAddConstraint(inequality, LpSolve.LE, batterySize*batteryMax);
			solver.strAddConstraint(inequality, LpSolve.GE, batterySize*batteryMin);
			
		}
		
		
		// setDrivingConsumptionSmallerSOC Inequality
		for(int i=0; i<schedule.numberOfDrivingTimes();i++){
			setDrivingConsumptionSmallerSOC(i,  buffer);
			
		}
		
		
		//upper & lower bounds
		setLowerAndUpperBounds(batterySize, batteryMin, batteryMax);
		
		
	}
	
	
	
	/**
	 * sets objective function
	 * 
	 * minimizing time in peak hours
	 * minimizing (-)*charging in off peak hours
	 * @throws LpSolveException
	 */
	public void setObjectiveFunction() throws LpSolveException{
		String objectiveStr="-1 ";// first entry for SOC, maximize starting SOC. minimize -(SOC)
		
		
		// peakTimes 1 else 0 
		
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			// if Parking interval
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
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
		
	
		solver.strSetObjFn(objectiveStr);	
		
		solver.setMinim(); //minimize the objective
	}
	
	
	
	
	/**
	 * weight is sum of free Joules in interval/total free Joules for agent
	 * @param thisParkingInterval
	 * @return
	 */
	public double calculateWeightOptimal(ParkingInterval thisParkingInterval){
		// want to maximize charging in this time. thus need to minimize the negative of the weight
		
		return (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
		//return (-1 )*Math.pow(1.1, (10*thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes));
	}
	
	
	
	
	public double calculateWeightSubOptimal(ParkingInterval thisParkingInterval){
		// TODO or to think... add constant term or not?
		// negative joules/negative total = positive
		
		return thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes;
		//return Math.pow(1.1, 10*thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInSubOptimalParkingTimes) ;
	}
	
	
	
	/**
	 * returns String for inequality vector
	 * (1  charging speed  -consumption  charging speed  0 0 0) *x <SOC
	 * @param threshold
	 * @return
	 */
	public String setInEqualityBatteryConstraint(int threshold){
		
		String objectiveStr="1 ";// first entry for SOC
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			
			
			if (i<=threshold){
				if(schedule.timesInSchedule.get(i).isParking()){
					ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
					String s= Double.toString(thisParkingInterval.getChargingSpeed());
					s= s.concat(" ");
					objectiveStr=objectiveStr.concat(s);
					
					}
				if(schedule.timesInSchedule.get(i).isDriving() ){
					DrivingInterval thisDrivingInterval= (DrivingInterval)schedule.timesInSchedule.get(i);
					String s= Double.toString(thisDrivingInterval.getConsumption()*(-1));
					s= s.concat(" ");
					objectiveStr=objectiveStr.concat(s);
					
					
					}
				
			}else{
				objectiveStr=objectiveStr.concat("0 ");
			}
		}
		
		return objectiveStr;
	}

	
	/**
	 * sets upper and lower bounds on all variables
	 * 0<SOC<battery capacity
	 * 0 <t< parking timeS
	 * 
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @throws LpSolveException
	 */
	public void setLowerAndUpperBounds(double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
		solver.setLowbo(1,batterySize*batteryMin);
		solver.setUpbo(1,batterySize*batteryMax);
		
		for(int i=2; i<=numberOfVariables; i++){
			if(schedule.timesInSchedule.get(i-2).isParking()){
				solver.setLowbo(i, 0);
				solver.setUpbo(i, schedule.timesInSchedule.get(i-2).getIntervalLength());
			}else{
				// Driving times
				solver.setLowbo(i, 1);
				solver.setUpbo(i, 1);
			}
			
		}
	}
	
	
	
	
	/**
	 * adds constraint to LP problem
	 * (1  charging speed  -consumption charging speed 0 0 )* x  >= (1+buffer)* nextE consumption
	 * @param a
	 * @param buffer
	 * @throws LpSolveException
	 */
	
	public void setDrivingConsumptionSmallerSOC(int a, double buffer) throws LpSolveException{
		String objectiveStr="1 ";// first entry for SOC
		int pos=schedule.positionOfIthDrivingTime(a);
		
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			if(i<pos){
				
				if(schedule.timesInSchedule.get(i).isParking()){
					objectiveStr=objectiveStr.concat(Double.toString(((ParkingInterval)schedule.timesInSchedule.get(i)).getChargingSpeed()) 
							+ " ");
					
				}
				
				if(schedule.timesInSchedule.get(i).isDriving()){
					objectiveStr=objectiveStr.concat(Double.toString(
							((DrivingInterval)schedule.timesInSchedule.get(i)).getConsumption()*(-1))+ " ");
					
				}
				
			}else{
				objectiveStr=objectiveStr.concat("0 ");
			}
		}
		DrivingInterval d= (DrivingInterval)schedule.timesInSchedule.get(pos);
		solver.strAddConstraint(objectiveStr, 
				LpSolve.GE, 
				(1+buffer)*d.getConsumption());
		
		
	}
	
	
	
	public Schedule update() throws LpSolveException{
		double[] solution = solver.getPtrVariables();
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				((ParkingInterval)schedule.timesInSchedule.get(i)).setRequiredChargingDuration(solution[i+1]);
				
			}
		}
		return schedule;
	}
	
	
	
	/**
	 * results of LP summarized
	 * prints out Total Charging in Sub/Optimal Time
	 * 
	 * @throws LpSolveException
	 */
	public void	printLPSolution() throws LpSolveException{
		double[] solution = solver.getPtrVariables();
		System.out.println("Charging times from LP:");
		System.out.println("Starting SOC: "+ solution[0]);
		
		double optimalChargingParking=0;
		double suboptimalChargingParking=0;
		
		for (int i = 1; i < solution.length; i++) {
			
			if (schedule.timesInSchedule.get(i-1).isDriving()){
				
			}
			else {
				ParkingInterval p = (ParkingInterval)schedule.timesInSchedule.get(i-1);
				if(p.isInSystemOptimalChargingTime()){
					optimalChargingParking+=p.getRequiredChargingDuration();
				}else{
					suboptimalChargingParking+=p.getRequiredChargingDuration();
				}
			}
		}
		
		System.out.println("Total Charging in Optimal Time: " + optimalChargingParking);
		System.out.println("Total Charging in Suboptimal Time: " + suboptimalChargingParking);
		
		//TODO STORE RESULTS and feed into utility?
	}
	
	
}
