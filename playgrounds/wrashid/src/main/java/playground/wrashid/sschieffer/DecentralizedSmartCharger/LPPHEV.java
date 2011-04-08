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

public class LPPHEV {
	
	private Schedule schedule;
	private LpSolve solver; 
	private int numberOfVariables;
	private Id personId;
	private double energyFromCombustionEngine;
	
	private double batterySize;
	private double  batteryMin;
	private double  batteryMax;
	
	
	public LPPHEV(){
		
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
	public Schedule solveLP(Schedule schedule, Id id,double batterySize, double batteryMin, double batteryMax, String vehicleType) throws LpSolveException, IOException{
		
		this.batteryMax=batteryMax;
		this.batteryMin=batteryMin;
		this.batterySize=batterySize;
		
		setUpLP(schedule, id, batterySize, batteryMin, batteryMax);
		solver.solve();
		
		try {
			
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LP_agent"+ personId.toString()+"printLp.txt");
			solver.printLp();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LP_agent"+ personId.toString()+"objective.txt");
			solver.printObjective();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LP_agent"+ personId.toString()+"tableau.txt");
			solver.printTableau();
		} catch (Exception e) {	    
		}
		
		
		schedule= update();
		System.out.println("updated schedule with required charging times:");
		schedule.printSchedule();
		
		printLPSolution();
		
		energyFromCombustionEngine= calcEnergyUsageFromCombustionEngine(solver.getPtrVariables());
		System.out.println("Energy from combustion Engine of PHEV: "+ energyFromCombustionEngine);
		
		visualizeSOCAgent(solver.getPtrVariables(),vehicleType);
		
		solver.deleteLp();
		
		return schedule;
		
		
	}
	
	
	
	
	public Schedule solveLPReschedule(Schedule schedule, Id id,double batterySize, double batteryMin, double batteryMax, String vehicleType, double startingSOC) throws LpSolveException, IOException{
		
		this.batteryMax=batteryMax;
		this.batteryMin=batteryMin;
		this.batterySize=batterySize;
		
		setUpLP(schedule, id, batterySize, batteryMin, batteryMax, startingSOC);
		solver.solve();
		
		try {
			
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LP_agent_reschedule"+ personId.toString()+"printLp.txt");
			solver.printLp();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LP_agent_reschedule"+ personId.toString()+"objective.txt");
			solver.printObjective();
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"\\LP\\PHEV\\LP_agent_reschedule"+ personId.toString()+"tableau.txt");
			solver.printTableau();
		} catch (Exception e) {	    
		}
		
		
		schedule= update();
		System.out.println("updated schedule with required charging times:");
		schedule.printSchedule();
		
		printLPSolution();
		
		energyFromCombustionEngine= calcEnergyUsageFromCombustionEngine(solver.getPtrVariables());
		System.out.println("Energy from combustion Engine of PHEV: "+ energyFromCombustionEngine);
		
		visualizeSOCAgent(solver.getPtrVariables(),vehicleType);
		
		solver.deleteLp();
		
		return schedule;
		
		
	}
	
	/**
	 * sets objective, inequalities and bounds on solution
	 * @param schedule
	 * @param id
	 * @throws LpSolveException
	 */
	private void setUpLP(Schedule schedule, Id id, double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
		
		energyFromCombustionEngine=0;
		this.schedule=schedule;
		personId=id;
		
		
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
	
	
	
	private void setUpLP(Schedule schedule, Id id, double batterySize, double batteryMin, double batteryMax, double startingSOC) throws LpSolveException{
		
		energyFromCombustionEngine=0;
		this.schedule=schedule;
		personId=id;
		
		
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
		setLowerAndUpperBoundsWithStartingSOC(batterySize, batteryMin, batteryMax, startingSOC);
		
		
	}
	
	
	/**
	 * sets objective function
	 * 
	 * minimizing time in peak hours
	 * minimizing (-)*charging in off peak hours
	 * @throws LpSolveException
	 */
	private void setObjectiveFunction() throws LpSolveException{
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
				//DrivingInterval thisDrivingInterval= (DrivingInterval)schedule.timesInSchedule.get(i);
				//double energyOut=thisDrivingInterval.getConsumption();
					
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
	 * weight is sum of free Joules in interval*(-1) /total free Joules for agent
	 * @param thisParkingInterval
	 * @return
	 */
	private double calculateWeightOptimal(ParkingInterval thisParkingInterval){
		// want to maximize charging in this time. thus need to minimize the negative of the weight
		
		return (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
		//return (-1 )*Math.pow(1.1, (10*thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes));
	}
	
	
	
	/**
	 * weight is sum of free Joules in interval /total free Joules for agent
	 * @param thisParkingInterval
	 * @return
	 */
	private double calculateWeightSubOptimal(ParkingInterval thisParkingInterval){
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
	private String setInEqualityBatteryConstraint(int threshold){
		
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
	private void setLowerAndUpperBounds(double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
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
	
	
	
	private void setLowerAndUpperBoundsWithStartingSOC(double batterySize, double batteryMin, double batteryMax, double startingSOC) throws LpSolveException{
		solver.setLowbo(1,startingSOC);
		solver.setUpbo(1,startingSOC);
		
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
	 * updates the required charging times in agentSchedules according to results of LP
	 * @return
	 * @throws LpSolveException
	 */
	private Schedule update() throws LpSolveException{
		double[] solution = solver.getPtrVariables();
		
		schedule.setStartingSOC(solution[0]);
		
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
	
	public double getEnergyFromCombustionEngine(){
		return energyFromCombustionEngine;
	}





	/**
	 * ONLY FOR DEBUGGING PURPOSES
	 */
	public void setSchedule(Schedule s){
		schedule=s;
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
	private void reducePrecedingParkingBy(Schedule s, int pos, double deduct){
			
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
	private void addExtraConsumptionDriving(Schedule s, int pos, double extraTime, double extraC){
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
	
	
	
	/**
	 * modifies objetive double array such that the SOC right after every driving trip is maximized
	 * the battery of the PHEV is not bounded by minBattery restrictions
	 * thus we have to ensure otherwise that energy is preferably charged from the battery
	 * 
	 * 
	 * @param objective array of coefficients from other objective restrictions so far
	 * @param a 
	 * @return
	 * @throws LpSolveException
	 */
	private double[] objectiveToMinimizeCombustionEngineUse(double [] objective, int a) throws LpSolveException{
		
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
	private String makeStringObjectiveFromDoubleObjective(double[]  objective){
		String s="";
		
		for(int i=0; i<objective.length; i++){
			s= s.concat(Double.toString(objective[i]) + " ");
		}
		return s;
	}
	
	
	
	
	
	public void visualizeSOCAgent(double [] solution, String type) throws LpSolveException, IOException{
		
		XYSeriesCollection SOCAgent= new XYSeriesCollection();
		
		XYSeries SOCAgentSeries= new XYSeries("SOC agent");
		
		double [] SOC= solution.clone();	
		SOCAgentSeries.add(0,SOC[0]);
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisP= (ParkingInterval)schedule.timesInSchedule.get(i);
				SOC[i+1]= SOC[i]+thisP.getChargingSpeed()	*solution[1+i];
				// add
				SOCAgentSeries.add(thisP.getEndTime(),SOC[i+1]);
			}else{
				//subtract
				DrivingInterval thisD = (DrivingInterval)schedule.timesInSchedule.get(i);
				
				SOC[i+1]=SOC[i] - (thisD).getConsumption();
				SOCAgentSeries.add(thisD.getEndTime(),SOC[i+1]);
			}
		}
		
		SOCAgent.addSeries(SOCAgentSeries);
		
		
		//********************************
		XYSeries SOCMax= new XYSeries("SOC Max");
		SOCMax.add(0, batterySize);
		SOCMax.add(DecentralizedSmartCharger.SECONDSPERDAY, batterySize);		
		
		XYSeries SOCMin= new XYSeries("SOC Min");
		SOCMin.add(0, 0);
		SOCMin.add(DecentralizedSmartCharger.SECONDSPERDAY, 0);
		//********************************
		XYSeries SOCMaxSuggested= new XYSeries("SOC Max Suggested");
		SOCMaxSuggested.add(0, batterySize*batteryMax);
		SOCMaxSuggested.add(DecentralizedSmartCharger.SECONDSPERDAY, batterySize*batteryMax);
		
		XYSeries SOCMinSuggested= new XYSeries("SOC MinSuggested");
		SOCMinSuggested.add(0,batterySize*batteryMin);
		SOCMinSuggested.add(DecentralizedSmartCharger.SECONDSPERDAY, batterySize*batteryMin);
		//********************************
		
		SOCAgent.addSeries(SOCMax);
		SOCAgent.addSeries(SOCMin);
		SOCAgent.addSeries(SOCMaxSuggested);
		SOCAgent.addSeries(SOCMinSuggested);
		
		JFreeChart chart = ChartFactory.createXYLineChart("SOC for agent"+ personId.toString(), 
				"time of day [s]", 
				"SOC[J]", 
				SOCAgent, 
				PlotOrientation.VERTICAL, 
				false, true, false);
		
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray);
        
        
    	//******************************** SOC
        plot.getRenderer().setSeriesPaint(0, Color.black);
     	plot.getRenderer().setSeriesStroke(
 	            0, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {4.0f, 1.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	
    	//******************************** SOCMAX
     	plot.getRenderer().setSeriesPaint(1, Color.red);
     	plot.getRenderer().setSeriesStroke(
 	            1, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {4.0f, 4.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
    	//********************************SOCMIN
     	plot.getRenderer().setSeriesPaint(2, Color.red);
     	plot.getRenderer().setSeriesStroke(
 	            2, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {4.0f, 4.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	//********************************SOCMaxsugg
     	plot.getRenderer().setSeriesPaint(3, Color.gray);
     	plot.getRenderer().setSeriesStroke(
 	            3, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {3.0f, 3.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	//********************************SOCMinsugg
     	plot.getRenderer().setSeriesPaint(4, Color.gray);
     	plot.getRenderer().setSeriesStroke(
 	            4, 
 	            new BasicStroke(
 	                2.0f,  //float width
 	                BasicStroke.CAP_ROUND, //int cap
 	                BasicStroke.JOIN_ROUND, //int join
 	                1.0f, //float miterlimit
 	                new float[] {3.0f, 3.0f}, //float[] dash
 	                0.0f //float dash_phase
 	            )
 	        );
     	
     	ChartUtilities.saveChartAsPNG(new File(DecentralizedSmartCharger.outputPath+ "SOC_PHEV_from_"+type+"Agent"+personId.toString()+".png") , chart, 800, 600);
	  	
	}
		
		
		
	
}
