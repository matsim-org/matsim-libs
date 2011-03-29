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

import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF.V2G.BatteryStatistics;
import playground.wrashid.PSF2.vehicle.vehicleFleet.ElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.sschieffer.Main;

import lpsolve.LpSolve;
import lpsolve.LpSolveException;

public class LPPHEV {
	
	private Schedule schedule;
	private LpSolve solver; 
	private int numberOfVariables;
	private Id personId;
	private double energyFromCombustionEngine;
	
	public LPPHEV(){
		
	}
	
	
	
	
	
	/**
	 * sets up the LP for EVs and solves the LP-Problem
	 * 
	 * @param schedule daily schedule of agent
	 * @param id  id of agent
	 * @return returns the updated schedule
	 * @throws LpSolveException
	 */
	public Schedule solveLP(Schedule schedule, Id id,double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
		
		setUpLP(schedule, id, batterySize, batteryMin, batteryMax);
		solver.solve();
		try {
			
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LPPHEV_V1G_agent"+ personId.toString()+"printLp.txt");
			solver.printLp();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LPPHEV_V1G_agent"+ personId.toString()+"objective.txt");
			solver.printObjective();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LPPHEV_V1G_agent"+ personId.toString()+"tableau.txt");
			solver.printTableau();
		} catch (Exception e) {	    
		}
		
		
		schedule= update();
		System.out.println("updated schedule with required charging times:");
		schedule.printSchedule();
		
		printLPSolution();
		
		energyFromCombustionEngine= calcEnergyUsageFromCombustionEngine();
		System.out.println("Energy from combustion Engine of PHEV: "+ energyFromCombustionEngine);
		
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
		
		energyFromCombustionEngine=0;
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
		
		
		
		numberOfVariables= schedule.getNumberOfEntries()+1;
		
		solver = LpSolve.makeLp(0, numberOfVariables);
		
		setObjectiveFunction();
		
		
		// at all points should be within battery limit
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			String inequality=setInEqualityBatteryConstraint(i);
			solver.strAddConstraint(inequality, LpSolve.LE, batterySize*batteryMax);
			//solver.strAddConstraint(inequality, LpSolve.GE, batterySize*batteryMin);
			
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
		double [] objective= new double[numberOfVariables];
		
		objective[0]= -1;
		
		
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			// if Parking interval
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
				
				if(thisParkingInterval.isInSystemOptimalChargingTime()){
					
					// want to maximize charging in this time. thus need to minimize the negative of the weight
					double weightOptimal=calculateWeightOptimal(thisParkingInterval);
					//double totalweight= weightOptimal;
					//double totalweight= weightOptimal-thisParkingInterval.getChargingSpeed();
					//double totalweight= weightOptimal-(100)*thisParkingInterval.getChargingSpeed();
					
					objective[ 1+i] = weightOptimal;
					
					}
				
				else{
					// want to minimize charging in this time. thus need to minimize the absolute value of the weight
					double weightSubOptimal=calculateWeightSubOptimal(thisParkingInterval);
					//double totalSubWeight= weightSubOptimal;
					//double totalSubWeight= weightSubOptimal-(100)*thisParkingInterval.getChargingSpeed();
					objective[ 1+i] = weightSubOptimal;
					
				}
			}else{
				// Driving
				DrivingInterval thisDrivingInterval= (DrivingInterval)schedule.timesInSchedule.get(i);
				double energyOut=thisDrivingInterval.getConsumption();
				
				//double energyOut=(100)*thisDrivingInterval.getConsumption();				
				objective[ 1+i] = 0;
				//objective[ 1+i] = energyOut;
				
				
			}
			}
		
		
		// now loop to add maximize SOC after consumption,
		
		// setDrivingConsumptionSmallerSOC Inequality
		for(int i=0; i<schedule.numberOfDrivingTimes();i++){
			objective=objectiveToMinimizeCombustionEngineUse(objective, i);
			
		}
		
		
		String objectiveStr= makeStringObjectiveFromDoubleObjective(objective);
		
	
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
	
	
	
	
	
	public double calcEnergyUsageFromCombustionEngine() throws LpSolveException{
		
		boolean status;
		
		double energyFromEngine=0;
		
		double statusJoule;
		
		double lastFalseJoule=0;
		
		double[] solution = solver.getPtrVariables();
		
		statusJoule=solution[0];
		
		if(statusJoule>=0){
			status=true;
			}else{
				status=false;
			}
		
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				
				ParkingInterval thisP= (ParkingInterval)schedule.timesInSchedule.get(i);
				
				statusJoule+=thisP.getChargingSpeed()
							*solution[1+i];// charging speed * time= joules
				
				if(statusJoule>=0){
					status=true;
					}else{
						status=false;
					}
				
			}else{//Driving
				DrivingInterval thisD = (DrivingInterval)schedule.timesInSchedule.get(i);
							
				statusJoule+=(-1)*(thisD).getConsumption();
				
				if(statusJoule<0 && status==true){
					
					energyFromEngine=Math.abs(statusJoule);
					status=false;
					lastFalseJoule=statusJoule;
					
					ParkingInterval precedingP= (ParkingInterval)schedule.timesInSchedule.get(i-1);				
					 
					
					
					updatePrecedingParkingAndDrivingInterval(schedule, i, energyFromEngine, thisD, precedingP);		 
					 
				}else{
					
					if(statusJoule<0 && status==false){
						energyFromEngine=Math.abs(statusJoule);
						energyFromEngine+=Math.abs(statusJoule-lastFalseJoule);
						
						ParkingInterval precedingP= (ParkingInterval)schedule.timesInSchedule.get(i-1);
						
						updatePrecedingParkingAndDrivingInterval(schedule, i, energyFromEngine, thisD, precedingP);
						
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
		
		reducePrecedingParkingBy(s , pos, engineTime);
		
		addExtraConsumptionDriving(s, pos, engineTime, energyFromEngine);
		
		
	}
	
	
	
	/**
	 * reduces the req charging times in preceding parking time(s) by given value
	 * @param s
	 * @param pos
	 * @param deduct
	 */
	public void reducePrecedingParkingBy(Schedule s, int pos, double deduct){
			
		for(int i=pos-1;i>0; i--){
			
			if(s.timesInSchedule.get(i).isParking()){
				
				ParkingInterval thisP = (ParkingInterval) s.timesInSchedule.get(i);
				
				if(thisP.getRequiredChargingDuration()>=deduct ){
					
					thisP.setRequiredChargingDuration(thisP.getRequiredChargingDuration()-deduct);
					i=0;
				}else{
					
					double stillLeft= deduct-thisP.getRequiredChargingDuration();
					thisP.setRequiredChargingDuration(0);
					reducePrecedingParkingBy( s, i, stillLeft);
					i=0;
				}
				
			}
		}
			
	}
	
	
	
	/**
	 * adds the given extra time and extra consumption to preceding driving times
	 * @param s
	 * @param pos
	 * @param extraTime
	 * @param extraC
	 */
	public void addExtraConsumptionDriving(Schedule s, int pos, double extraTime, double extraC){
		for(int i=pos;i>0; i--){
			
			if(s.timesInSchedule.get(i).isDriving()){
				
				DrivingInterval thisD = (DrivingInterval) s.timesInSchedule.get(i);
				if(thisD.getIntervalLength()>=extraTime ){
					
					thisD.setExtraConsumption(extraC, extraTime);
					i=0;
					
				}else{
					double consLeft= extraC- thisD.getConsumption();
					double timeLeft= extraTime-thisD.getIntervalLength();
					
					addExtraConsumptionDriving(s, i, timeLeft, consLeft);
					thisD.setExtraConsumption(thisD.getConsumption(), thisD.getIntervalLength());
					i=0;
				}
			}
		}
		
		
	}
	
	
	
	public double[] objectiveToMinimizeCombustionEngineUse(double [] objective, int a) throws LpSolveException{
		
		objective[0]+=-1;
		
		int pos=schedule.positionOfIthDrivingTime(a);
		
		for(int i=0; i<schedule.timesInSchedule.size(); i++){
			if(i<pos){
				
				if(schedule.timesInSchedule.get(i).isParking()){
					objective[1+i]+= (-1)* ((ParkingInterval)schedule.timesInSchedule.get(i)).getChargingSpeed();
					
					
				}
				
				if(schedule.timesInSchedule.get(i).isDriving()){
					objective[1+i]+= ((DrivingInterval)schedule.timesInSchedule.get(i)).getConsumption();
					
					
				}
				
			}else{
				//nothing
			}
		}
		
		return objective;
		
	}
	
	
	/**
	 * turns a double array into a string separated by spaces
	 * 
	 * @param objective
	 * @return
	 */
	public String makeStringObjectiveFromDoubleObjective(double[]  objective){
		String s="";
		
		for(int i=0; i<objective.length; i++){
			s= s.concat(Double.toString(objective[i]) + " ");
		}
		return s;
	}
	
	
	
	public double getEnergyFromCombustionEngine(){
		return energyFromCombustionEngine;
	}
	
}
