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
public class LPEV {
	
	private Schedule schedule;
	private LpSolve solver; 
	private int numberOfVariables;
	
	private double buffer;
	
	private double batterySize;
	private double  batteryMin;
	private double  batteryMax;
	
	private boolean output;
	
	public LPEV(double buffer, boolean output){
		this.buffer=buffer;
		this.output=output;
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
		
		this.batteryMax=batteryMax;
		this.batteryMin=batteryMin;
		this.batterySize=batterySize;
		
		if(DecentralizedSmartCharger.debug){
			System.out.println("LP EV for Agent: "+ id.toString()); 
		}
		
		setUpLP(schedule, batterySize, batteryMin, batteryMax);
		int status = solver.solve();
        
        if(status!=0){
        	
        	if(DecentralizedSmartCharger.debug){
        		String text = solver.getStatustext(status);
        		System.out.println("Status text: "+ text); 
    		}
        	// status=0--> OPTIMAL  2 --> INFEASIBLE
        	return null; 
        }
        
      
		
		try {
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"DecentralizedCharger\\LP\\EV\\LP_agent"+ id.toString()+"printLp.txt");
			solver.printLp();
			
			} catch (Exception e) {	    
		}
		
		schedule= update();
	
		if(output){
			visualizeSOCAgent(solver.getPtrVariables(), vehicleType, id);
			
		}
		
		solver.deleteLp();
		
		return schedule;
		
		
	}
	
	
	
	
	
	
	
	
	
	public Schedule solveLPReschedule(Schedule schedule, 
			Id id, 
			double batterySize, double batteryMin, double batteryMax, 
			String vehicleType, 
			double startingSOC) throws LpSolveException, IOException{
		
		if(DecentralizedSmartCharger.debug){
			System.out.println("LP EV Resolve for Agent: "+ id.toString()); 
			/*schedule.printSchedule();*/
		}
		
		
		this.batteryMax=batteryMax;
		this.batteryMin=batteryMin;
		this.batterySize=batterySize;
		
		
		setUpLP(schedule, batterySize, batteryMin, batteryMax, startingSOC);
		int status = solver.solve();
        
        if(status!=0){
        	if(DecentralizedSmartCharger.debug){
        		String text = solver.getStatustext(status);
            	System.out.println("Status text: "+ text); 
            	// status=0--> OPTIMAL
            	// 2 --> INFEASIBLE
    		}
        	
        	return null; 
        }
        
      
		
		try {
			
			
			solver.setOutputfile(DecentralizedSmartCharger.outputPath+"V2G\\LP\\EV\\LP_agent_reschedule"+ id.toString()+"printLp.txt");
			solver.printLp();
		
		} catch (Exception e) {	    
		}
		
		schedule= update();

		
		solver.deleteLp();
		
		return schedule;
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	/**
	 * sets objective, inequalities and bounds on solution
	 * @param schedule
	 * @param id
	 * @throws LpSolveException
	 */
	public void setUpLP(Schedule schedule, double batterySize, double batteryMin, double batteryMax) throws LpSolveException{
		this.schedule=schedule;
		
		
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
	
	
	
	public LpSolve getSolver(){
		return solver;
	}
	
	
	private void setUpLP(Schedule schedule, double batterySize, double batteryMin, double batteryMax, double startingSOC) throws LpSolveException{
		this.schedule=schedule;
	
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
	 * weight is (-1 )*free Joules in interval/total free Joules for agent
	 * @param thisParkingInterval
	 * @return
	 */
	private double calculateWeightOptimal(ParkingInterval thisParkingInterval){
		// want to maximize charging in this time. thus need to minimize the negative of the weight
		
		return (-1 )* thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes;
		//return (-1 )*Math.pow(1.1, (10*thisParkingInterval.getJoulesInInterval()/schedule.totalJoulesInOptimalParkingTimes));
	}
	
	
	
	/*
	 * weight is Joules in interval/total suboptimal Joules for agent
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
	
	
	
	/**
	 * sets lower and upper bounds on all variables: 
	 * SOC= given Starting SOC,
	 * 0 <t< parking timeS
	 * 
	 * @param batterySize
	 * @param batteryMin
	 * @param batteryMax
	 * @throws LpSolveException
	 */
	private void setLowerAndUpperBoundsWithStartingSOC(double batterySize, double batteryMin, double batteryMax, double startingSOC) throws LpSolveException{
		solver.setLowbo(1, startingSOC);
		solver.setUpbo(1, startingSOC);
		
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
	
	private void setDrivingConsumptionSmallerSOC(int a, double buffer) throws LpSolveException{
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
	
	
	/**
	 * update agent schedule with results of LP
	 * @return
	 * @throws LpSolveException
	 */
	public Schedule update() throws LpSolveException{
		double[] solution = solver.getPtrVariables();
		
		schedule.setStartingSOC(solution[0]);
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisParking= (ParkingInterval) schedule.timesInSchedule.get(i);
				
				if(solution[i+1]>0.0){
					 thisParking.setRequiredChargingDuration(solution[i+1]);
					
				}else{
					// 0 or negative 
					 thisParking.setRequiredChargingDuration(0);
					 thisParking.setChargingSchedule(null);
				}
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
//		
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
		
	}
	
	
	
public void visualizeSOCAgent(double [] solution, String type, Id id) throws LpSolveException, IOException{
		
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
		
		JFreeChart chart = ChartFactory.createXYLineChart("SOC for agent"+ id.toString(), 
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
     	
     	ChartUtilities.saveChartAsPNG(new File(DecentralizedSmartCharger.outputPath+ "DecentralizedCharger\\SOC_of_"+type+"afterLPEV_Agent" + id.toString()+".png") , chart, 800, 600);
	  	
	}
	
	
}
