/* *********************************************************************** *
 * project: org.matsim.*
 * DecentralizedChargerV1Beta.java
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

package playground.wrashid.sschieffer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import lpsolve.*;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.optimization.OptimizationException;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.junit.runner.notification.Failure;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.vehicles.Vehicles;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.PSF2.vehicle.vehicleFleet.PlugInHybridElectricVehicle;
import playground.wrashid.PSF2.vehicle.vehicleFleet.Vehicle;
import playground.wrashid.lib.obj.LinkedListValueHashMap;

public class DecentralizedChargerV1Beta {

	final DrawingSupplier supplier = new DefaultDrawingSupplier();
	private LinkedListValueHashMap<Id, Double> energyConsumptionOfLegs;
	private LinkedListValueHashMap<Id, ParkingIntervalInfo> parkingTimeIntervals;
		
	private double [][] parkingTimesCurrentAgent;
	private double [][] peakAndOffPeakParkingTimesCurrentAgent;
	private double [][] drivingPeakAndOffPeakTimesCurrentAgent;
	
	private double [] parkingLengthCurrentAgent;
	
	private double [][] chargingTimesCurrentAgent;
	
	private double [][] drivingTimesCurrentAgent;
	private double []  drivingConsumptionCurrentAgent;
	
	private DecentralizedChargerInfo myChargerInfo;
	
	private double populationTotal;
	private double noOfPHEVs;
	private double averagePHEVConsumptionInWatts;
	private double averagePHEVConsumptionInJoules;
	final Controler controler;
	private double sumPeakTimeCharging;
	
	
	private XYSeriesCollection allChargingTimesSet= new XYSeriesCollection();
	
	
	/**
	 * Public constructor
	 */
	public DecentralizedChargerV1Beta(Controler controler, EnergyConsumptionPlugin energyConsumptionPlugin, ParkingTimesPlugin parkingTimesPlugin){
		sumPeakTimeCharging=0;
		
		//ArrayList<Integer> failureList=
		this.controler=controler;
		energyConsumptionOfLegs = energyConsumptionPlugin.getEnergyConsumptionOfLegs();
		parkingTimeIntervals = parkingTimesPlugin.getParkingTimeIntervals();
		
	}
	
	/**
	 * Output call of the Decentralized Charger!
	 * @param startChargingTime
	 * @param endChargingTime
	 * @param agentId
	 * @param linkId
	 */
	public void getElectricityFromGrid(double startChargingTime, double endChargingTime, Id agentId){
		System.out.println("Electricity for Agent:"+ agentId.toString() +" from "+startChargingTime +" to " +endChargingTime);
	}
	
	public double calcNumberOfPHEVs(Controler controler){
		populationTotal=controler.getPopulation().getPersons().size();
		return noOfPHEVs = populationTotal*Main.penetrationPercent;
	}
	
	
	public void performChargingAlgorithm() throws OptimizationException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException, LpSolveException {
		
		noOfPHEVs = calcNumberOfPHEVs(controler);
		averagePHEVConsumptionInJoules= getAveragePHEVConsumptionInJoules();
		averagePHEVConsumptionInWatts= getAveragePHEVConsumptionInWatt();
		myChargerInfo = new DecentralizedChargerInfo(noOfPHEVs, averagePHEVConsumptionInJoules); 
		
		myChargerInfo.loadBaseLoadCurveFromTextFile();
		
		myChargerInfo.getFittedLoadCurve();
		
		myChargerInfo.findHighLowIntervals();
		myChargerInfo.findProbabilityDensityFunctions();
		myChargerInfo.findProbabilityRanges();
		myChargerInfo.writeSummary();
		
		/*Loop over all agents
		 * for each agent -make personal list of links and times
		 */
			
		assignSlotsToAllAgents();
		
		System.out.println("sumPeakTimeCharging total : "+ sumPeakTimeCharging);
		System.out.println("sumPeakTimeCharging average per person : "+ sumPeakTimeCharging/100);
		printGraphChargingTimesAllAgents();
	
	}//end performChargingAlgorithm
	
	
	/**
	 * Loops over all agents
	 * - gets their daily schedule, off-peak parking, peak-parking and driving times and consumptions
	 * - calls linear programming to find required charging durations for all parking times
	 * - assigns slots
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 * @throws LpSolveException 
	 * @throws IOException 
	 */
	public void assignSlotsToAllAgents() throws LpSolveException, MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, IOException{

		for (Id personId: controler.getPopulation().getPersons().keySet()){
			
			System.out.println("Start Check Agent:" + personId.toString());
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			// get double[] with parking lengths AND fill double [][] parkingTimesCurrentAgent	
			double [] parkingLengthsCurrentAgent=getParkingLengthsCurrentAgent(parkingIntervals);
											
			// get double[] with driving Consumptions		
			drivingConsumptionCurrentAgent=getdrivingConsumptionCurrentAgent(personId);
			//get double [][] drivingTimes		
			drivingTimesCurrentAgent = getDrivingTimesCurrentAgent(parkingTimesCurrentAgent,parkingIntervals);
			
			peakAndOffPeakParkingTimesCurrentAgent=getPeakAndOffPeakParkingTimes();
			
			drivingPeakAndOffPeakTimesCurrentAgent=getDrivingPeakAndOffPeakTimes(peakAndOffPeakParkingTimesCurrentAgent, drivingTimesCurrentAgent, drivingConsumptionCurrentAgent);
			//order times			
			drivingPeakAndOffPeakTimesCurrentAgent=getActualOrder(drivingPeakAndOffPeakTimesCurrentAgent, 4);
		
			System.out.println("Done Ordering sequence of parking and driving times");
			
			
			/*
			 * START LINEAR PROGRAMMING
			 * set number of variables
			 * 	   number of constraints
			 */
			
			// number of Variables is number of rows in drivingPeakAndOffPeakTimesCurrentAgent PLUS 1 from initial SOC
			int numberOfVariables=drivingPeakAndOffPeakTimesCurrentAgent.length+1;
			
			/*
			 * CONSTRAINTS
			 * 1 equality constraint: SUM in = SUM out
			 * 2*n inequality constraints to keep battery level within capacity
			 * 2*n -2 upper and lower bounds formulated as inequalities (excluding extra for SOC
			 */
			
			//int numberOfConstraints=1+4*numberOfVariables;
			
			//LpSolve solver = LpSolve.makeLp(numberOfConstraints, numberOfVariables);
			LpSolve solver = LpSolve.makeLp(0, numberOfVariables);
			
			// OBJECTIVE FUNCTION is to minimize the time charging in peak hours (0 entry in [][2])
			double[] objective=getObjectiveFunction(drivingPeakAndOffPeakTimesCurrentAgent);
			
			String objectiveStr =turnDoubleRowInString(objective);
			//solver.setObjFn(objective);
			solver.strSetObjFn(objectiveStr);
			
			solver.setMinim(); //minimize the objective
			//solver.setMaxim();
			/*
			 * addConstraint(double[] row, int constrType, double rh) 
				types:
				LE - lower equal
				GE - greater or equal
				EQ - equal
				OF ?
				GR  ?
				
			 */
			
			//EQUALITY constraint 
			double[] equality=getEqualityConstraints(drivingPeakAndOffPeakTimesCurrentAgent);
			
			String equalityStr=turnDoubleRowInString(equality);
			//solver.addConstraint(equality, LpSolve.EQ, 0.0); 
			solver.strAddConstraint(equalityStr, LpSolve.EQ, 0.0); 
			
			
			// INEQUALITY CONSTRAINTS
			// Physical feasibility in sequence
			// POSSIBLE TO CHANGE INEQUALITY TO Main.batteryCapacity * Main.maxCharge
			for (int i=0; i<numberOfVariables;i++){
				double[] inequalityBelowBatteryCapacity=getInequalityBelowBatteryCapacity(drivingPeakAndOffPeakTimesCurrentAgent, i);
				
				//solver.addConstraint(inequalityBelowBatteryCapacity, LpSolve.LE, Main.batteryCapacity*Main.maxCharge);
				String inequalityBelowBatteryCapacityStr=turnDoubleRowInString(inequalityBelowBatteryCapacity);
				solver.strAddConstraint(inequalityBelowBatteryCapacityStr, LpSolve.LE, Main.batteryCapacity*Main.maxCharge);
			}
			
			// POSSIBLE TO CHANGE INEQUALITY TO Main.batteryCapacity * Main.minCharge
			for (int i=0; i<numberOfVariables;i++){
				double[] inequalityBatteryGreaterZero=getInequalityBatteryGreaterZero(drivingPeakAndOffPeakTimesCurrentAgent, i);
				
				String inequalityBatteryGreaterZeroStr=turnDoubleRowInString(inequalityBatteryGreaterZero);
				solver.strAddConstraint(inequalityBatteryGreaterZeroStr, LpSolve.GE, Main.batteryCapacity*Main.minCharge);
			
				//solver.addConstraint(inequalityBatteryGreaterZero, LpSolve.GE, 0.0);
			}
			
			
			
			
			//LOWER BOUNDS as INEQUALITIES
			
			// upper bound: 1*charging time <=maxparking time  --> LE
			// lower bound: 1*charging time >=0 time  --> GE
			for (int i=0; i<drivingPeakAndOffPeakTimesCurrentAgent.length;i++){
				double lowerBound= getLowerBound(drivingPeakAndOffPeakTimesCurrentAgent, i);
				
				/*double[] lowerBoundInequality=getInequalityConstraintsWithOneAtTimeI(drivingPeakAndOffPeakTimesCurrentAgent, i);
				
				String lowerBoundInequalityStr=turnDoubleRowInString(lowerBoundInequality);
				solver.strAddConstraint(lowerBoundInequalityStr, LpSolve.GE, lowerBound);
					*/		
				//solver.addConstraint(lowerBoundInequality, LpSolve.GE, lowerBound); 
				solver.setLowbo(i+2, lowerBound);
				
				
			}
			solver.setLowbo(1, Main.batteryCapacity*Main.minCharge);
		
			
			
			//UPPER BOUNDS
			
			for (int i=0; i<drivingPeakAndOffPeakTimesCurrentAgent.length;i++){
				double upperBound= getUpperBound(drivingPeakAndOffPeakTimesCurrentAgent, i);
				
				/*double[] upperBoundInequality=getInequalityConstraintsWithOneAtTimeI(drivingPeakAndOffPeakTimesCurrentAgent, i);
				String upperBoundInequalityStr=turnDoubleRowInString(upperBoundInequality);
				
				solver.strAddConstraint(upperBoundInequalityStr, LpSolve.LE, upperBound); 
				*/
				solver.setUpbo(i+2, upperBound);
			}
			
			solver.setUpbo(1, Main.batteryCapacity*Main.maxCharge);
					
			//SOLUTION			
			solver.solve();
			
			double[] solution = solver.getPtrVariables();
			
			try {
				
				sumPeakTimeCharging+=solver.getObjective();
				
				solver.setOutputfile(Main.outputPath+"\\LP\\LpOut_agent"+ personId.toString()+"printLp.txt");
				solver.printLp();
				
				solver.setOutputfile(Main.outputPath+"\\LP\\LpOut_agent"+ personId.toString()+"objective.txt");
				solver.printObjective();
				
				solver.setOutputfile(Main.outputPath+"\\LP\\LpOut_agent"+ personId.toString()+"tableau.txt");
				solver.printTableau();
			} catch (Exception e) {	    
			}
			
		
			
			// check
			System.out.println("solution vector:");
			for (int i=0; i<solution.length; i++){
				System.out.println(solution[i]);
			}
			
			double totalChargingTimeInPeakTime=solver.getObjective();
			printLPSolution(solution, totalChargingTimeInPeakTime);
			
	      	// Get CHARGING TIMES from LP results
	      	chargingTimesCurrentAgent=getChargingTimes(drivingPeakAndOffPeakTimesCurrentAgent,solution );
	      	
	      	saveChargingTimesCurrentAgent(personId, chargingTimesCurrentAgent);
	      	
	      	printGraphOfDailyPlanForAgent(personId, chargingTimesCurrentAgent, drivingPeakAndOffPeakTimesCurrentAgent);
	      	
	      	solver.deleteLp();
	      	
			for (int i=0; i< chargingTimesCurrentAgent.length; i++){
				getElectricityFromGrid(chargingTimesCurrentAgent[i][0], chargingTimesCurrentAgent[i][1], personId);
					
			}
		}
		
	}
	
	
	public double [] getDoubleArraySizeNWithOneAtPositionM(int n, int m){
		double [] d= new double[n];
		for (int i=0; i<n;i++){
			d[i]=0.0;
			if (i==m){
				d[i]=1.0;
			}
		}
		return d;
	}
	
	
	
	
	public void printGraphChargingTimesAllAgents() throws IOException{
		//for (XYSeries x :myChargerInfo.probDensityXYSeries){
		//	allChargingTimesSet.addSeries(x);
		//}
		// just for scale purposes zeroLine - easy fix --> could be more elegant
		allChargingTimesSet.addSeries(myChargerInfo.zeroLineData);
		JFreeChart chart = ChartFactory.createXYLineChart("Distribution of charging times for all agents by agent Id number", 
				"time of day [s]", 
				"agent [Id]", 
				allChargingTimesSet, 
				PlotOrientation.VERTICAL, false, true, false);
		
		int seriesCount= allChargingTimesSet.getSeries().size();
		int totalC=0;
		for (int u=0; u<seriesCount; u++){
			
			totalC+=allChargingTimesSet.getSeries(u).getItemCount();
		}
				
		
		final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(supplier);
        for(int j=0; j<totalC; j++){
        	plot.getRenderer().setSeriesPaint(j, Color.black);
        	
        	plot.getRenderer().setSeriesStroke(
    	            j, 
    	            new BasicStroke(
    	                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
    	                1.0f, new float[] {10.0f, 0.0f}, 5.0f
    	            )
    	        );
        }
       chart.setTitle(new TextTitle("Distribution of charging times for all agents by agent Id number", 
    		   new Font("Arial", Font.BOLD, 20)));
        
        ChartUtilities.saveChartAsPNG(new File(Main.outputPath + "_allChargingTimes.png"), chart, 800, 600);
	}
	
	
	
	public void saveChargingTimesCurrentAgent(Id personId, double [][]chargingTimes){
				
		for (int i=0; i<chargingTimes.length;i++){
			String agent= personId.toString();
			int agentNo=Integer.parseInt (agent);
			
			XYSeries chargingTimesSet= new XYSeries("agent_"+agent);
			chargingTimesSet.add(chargingTimes[i][0],agentNo);
			chargingTimesSet.add(chargingTimes[i][1],agentNo);
			allChargingTimesSet.addSeries(chargingTimesSet);
		}
	}
	
	
	public void printGraphOfDailyPlanForAgent(Id personId, double [][] chargingTimes, double [][] drivingPeakAndOffPeakTimes) throws IOException{
		
		XYSeriesCollection allTimesSet= new XYSeriesCollection();
		
		for (int i=0; i<chargingTimes.length;i++){
			XYSeries chargingTimesSet= new XYSeries("charging times");
			chargingTimesSet.add(chargingTimes[i][0],1);
			chargingTimesSet.add(chargingTimes[i][1],1);
			allTimesSet.addSeries(chargingTimesSet);
		}
		
		for (int i=0; i<drivingPeakAndOffPeakTimes.length;i++){
			if(drivingPeakAndOffPeakTimes[i][2]==2){
				//driving
				XYSeries drivingTimesSet= new XYSeries("driving times");
				drivingTimesSet.add(drivingPeakAndOffPeakTimes[i][0],4);
				drivingTimesSet.add(drivingPeakAndOffPeakTimes[i][1],4);
				allTimesSet.addSeries(drivingTimesSet);
			}
			else if (drivingPeakAndOffPeakTimes[i][2]==0){
				XYSeries peakTimesSet= new XYSeries("peak times");
				peakTimesSet.add(drivingPeakAndOffPeakTimes[i][0],3);
				peakTimesSet.add(drivingPeakAndOffPeakTimes[i][1],3);
				allTimesSet.addSeries(peakTimesSet);
			}
			else{
				XYSeries offPeakTimesSet= new XYSeries("off peak times");
				offPeakTimesSet.add(drivingPeakAndOffPeakTimes[i][0],2);
				offPeakTimesSet.add(drivingPeakAndOffPeakTimes[i][1],2);
				allTimesSet.addSeries(offPeakTimesSet);
			}
						
		}
		
		JFreeChart chart = ChartFactory.createXYLineChart("Travel pattern agent : "+personId.toString(), "time [s]", "charging, off-peak parking, peak-parking, driving times", allTimesSet, PlotOrientation.VERTICAL, false, true, false);
		chart.setBackgroundPaint(Color.white);
		
		final XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray); 
        //TextAnnotation offPeak= new TextAnnotation("Off Peak parking time");
        XYTextAnnotation txt1= new XYTextAnnotation("Charging time", 20000,1.1);
        XYTextAnnotation txt2= new XYTextAnnotation("Driving time", 20000,4.1);
        XYTextAnnotation txt3= new XYTextAnnotation("Peak parking time", 20000,3.1);
        XYTextAnnotation txt4= new XYTextAnnotation("Off Peak parking time", 20000,2.1);
        txt1.setFont(new Font("Arial", Font.PLAIN, 14));
        txt2.setFont(new Font("Arial", Font.PLAIN, 14));
        txt3.setFont(new Font("Arial", Font.PLAIN, 14));
        txt4.setFont(new Font("Arial", Font.PLAIN, 14));
        //public Font(String name,int style,int size)
            
        plot.addAnnotation(txt1);
        plot.addAnnotation(txt2);
        plot.addAnnotation(txt3);
        plot.addAnnotation(txt4);
        
        int t=chargingTimes.length+drivingPeakAndOffPeakTimes.length;
        
        for(int j=0; j<t; j++){
        	plot.getRenderer().setSeriesPaint(j, Color.black);
        	plot.getRenderer().setSeriesStroke(
    	            j, //indicate series number
    	          
    	            new BasicStroke(
    	                5.0f,  //float width
    	                BasicStroke.CAP_ROUND, //int cap
    	                BasicStroke.JOIN_ROUND, //int join
    	                1.0f, //float miterlimit
    	                new float[] {6.0f, 0.0f}, //float[] dash
    	                0.0f //float dash_phase
    	            )
    	        );
            
        }
        
        
        /*ChartFrame frame1=new ChartFrame("XYLine Chart",chart);
        frame1.setVisible(true);
        frame1.setSize(300,300);  */ 
        ChartUtilities.saveChartAsPNG(new File(Main.outputPath+ "agent "+personId.toString()+"_dayPlan.png") , chart, 800, 600);
		  
		
	}
	

	
	public double[] getInequalityConstraintsWithOneAtTimeI(double [][]drivingPeakAndOffPeakTimesCurrentAgent,int fill){
		double[] objective=new double [drivingPeakAndOffPeakTimesCurrentAgent.length+1];
		//SOC
		objective[0]=0;
		// loop over all parking and driving times
		for (int i=1; i<objective.length;i++){
			if (i==fill+1){
				
				objective[i]=1.0;
			}
			
			else{
				objective[i]=0.0;
			}
		}
		
		return objective;
	}
	
	
	
	public double[][] getChargingTimes(double [][] drivingPeakAndOffPeakTimesCurrentAgent, double [] solution ) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		ArrayList <double[][]> tempChargingTimes=new ArrayList<double[][]>(0);
		
		// loop solution vector, but skip first entry for starting SOC
		for (int i=1; i<solution.length;i++){
			if (solution[i]>0 && drivingPeakAndOffPeakTimesCurrentAgent[i-1][2]!=2){
				
				// FULL Time SLOT
				double[][] temp = new double [1][2];
				//FULLTIME
				if (drivingPeakAndOffPeakTimesCurrentAgent[i-1][3]==solution[i]){
					temp[0][0]=drivingPeakAndOffPeakTimesCurrentAgent[i-1][0];
					temp[0][1]=temp[0][0]+solution[i];
					tempChargingTimes.add(temp);
				}
				
				//PART OF PEAK TIME
				else if (drivingPeakAndOffPeakTimesCurrentAgent[i-1][2]==0){
										
					//HOMOGENOUSLY DISTRIBUTED PART OF THE TIME
					double startIntervalLength=(drivingPeakAndOffPeakTimesCurrentAgent[i-1][3]-solution[i]);
					
					double start= Math.random()*startIntervalLength;
					temp[0][0]=drivingPeakAndOffPeakTimesCurrentAgent[i-1][0]+Math.round(start);
					temp[0][1]=temp[0][0]+solution[i];
					tempChargingTimes.add(temp);
				}
				else{
					//PART OF OFF-PEAK TIME - probability density functions
					double [][] allSlots=myChargerInfo.getXChargingSecondsInGivenOffPeakInterval(drivingPeakAndOffPeakTimesCurrentAgent[i-1][0], drivingPeakAndOffPeakTimesCurrentAgent[i-1][1], solution[i] );
					for (int a=0; a<allSlots.length; a++){
						double[][] tempSlot= new double [1][2];
						tempSlot[0][0]=allSlots[a][0];
						tempSlot[0][1]=allSlots[a][1];
						tempChargingTimes.add(tempSlot);
					}
				}
						
					
			}			
				
		}//for
		double [][] chargingTimes= new double [tempChargingTimes.size()][2];
		for(int i=0; i<tempChargingTimes.size(); i++){
			double[][] temp =tempChargingTimes.get(i);
			chargingTimes[i][0]=temp[0][0];
			chargingTimes[i][1]=temp[0][1];
		}
		return chargingTimes;
		
		}
		
	
	
	public void	printLPSolution(double [] solution, double totalChargingTimeInPeakTime){
		System.out.println("Charging times from LP:");
		System.out.println("Starting SOC: "+ solution[0]);
		for (int i = 1; i < solution.length; i++) {
			if (drivingPeakAndOffPeakTimesCurrentAgent[i-1][2]==0){
				System.out.println("charging in Peak Time: " + solution[i]);
			}
			else if (drivingPeakAndOffPeakTimesCurrentAgent[i-1][2]==1){
				System.out.println("charging in Off-Peak Time: " + solution[i]);
			}
			else{
				System.out.println("driving");
			}
		}
		
		System.out.println("Total Charging in Peak Time: " + totalChargingTimeInPeakTime);
	}

	/**
	 * Lower bound will be 1 if the time period is driving
	 * the lower bound will be 0(seconds) of charging for parking times
	 * @param drivingPeakAndOffPeakTimesCurrentAgent
	 * @param i
	 * @return zero or 1
	 */
	public double getLowerBound(double [][] drivingPeakAndOffPeakTimesCurrentAgent, int i){
		if (drivingPeakAndOffPeakTimesCurrentAgent[i][2]==2){
			//if driving lower bound is 1
			return 1.0;
		}
		else{
			return 0.0;
		}
	}
	
	/**
	 * Upper bound will be 1 if the time period is driving --> drivingPeakAndOffPeakTimesCurrentAgent[i][2]==2
	 * for parking times, the the parking duration is max charging time --> drivingPeakAndOffPeakTimesCurrentAgent[i][3];
	 * @param drivingPeakAndOffPeakTimesCurrentAgent
	 * @param i
	 * @return 
	 */
	public double getUpperBound(double [][] drivingPeakAndOffPeakTimesCurrentAgent, int i){
		if (drivingPeakAndOffPeakTimesCurrentAgent[i][2]==2){
			//if driving lower bound is 1
			return 1.0;
		}
		else{
			return drivingPeakAndOffPeakTimesCurrentAgent[i][3];
		}
	}
	
	
	public double[] getInequalityBatteryGreaterZero(double [][] drivingPeakAndOffPeakTimesCurrentAgent, int fill){
		double[] objective=new double [drivingPeakAndOffPeakTimesCurrentAgent.length+1];
		for (int i=0; i<objective.length;i++){
			if (i<=fill){
				if(i==0){
					objective[i]=1.0; //SOCstart [J] = 1* SOC
				}
				else{
					// SOC =+ chargingSpeed*times
					// or SOC =+ energyConsumption*1
					objective[i]=getChargingSpeedOrConsumptionForTimeInterval(i-1, drivingPeakAndOffPeakTimesCurrentAgent);
				}
				
			}
			
			
			else{
				objective[i]=0.0;
			}
		}
		
		return objective;
	}
	
	
	
	public double[] getInequalityBelowBatteryCapacity(double [][] drivingPeakAndOffPeakTimesCurrentAgent, int fill){
		double[] objective=new double [drivingPeakAndOffPeakTimesCurrentAgent.length+1];
		for (int i=0; i<objective.length;i++){
			if (i<=fill){
				if(i==0){
					objective[i]=1.0; //SOCstart [J] = 1* SOC
				}
				else{
					// SOC =+ chargingSpeed*times
					// or SOC =+ energyConsumption*1
					objective[i]=getChargingSpeedOrConsumptionForTimeInterval(i-1, drivingPeakAndOffPeakTimesCurrentAgent);
				}
				
			}
			else{
				// all other entries are 0
				objective[i]=0.0;
			}
		}
		
		return objective;
	}
	
	public String turnDoubleRowInString(double[] d){
		String dString="";
		for (int i=0; i<d.length; i++){
			double t=d[i];
			dString=dString + t + " ";
		}
		return dString;
	}
	
	public double getChargingSpeedOrConsumptionForTimeInterval(int i, double [][] drivingPeakAndOffPeakTimesCurrentAgent){
		if(drivingPeakAndOffPeakTimesCurrentAgent[i][2]==2){
			// driving --> enter consumption
			return drivingPeakAndOffPeakTimesCurrentAgent[i][3];
		}
		else{
			// parking in peak or off-peak --> charging Speed
			return Main.chargingSpeedPerSecond;
		}
	}
	
	
	/**
	 * formulates objective function for LP Problem
	 * sets all entries corresponding to parkingTimes in peak Hours to 1 so that their sum can be minimized
	 * @param drivingPeakAndOffPeakTimesCurrentAgent
	 * @return
	 */
	public double[] getObjectiveFunction(double[][] drivingPeakAndOffPeakTimesCurrentAgent){
		
		double[] objective=new double [drivingPeakAndOffPeakTimesCurrentAgent.length+1];
		objective[0]=0;//SOC entry
		for (int i=0; i<drivingPeakAndOffPeakTimesCurrentAgent.length;i++){
			if(drivingPeakAndOffPeakTimesCurrentAgent[i][2]==0.0){
				// minimize the respective variable
				objective[i+1]=1.0;
			}
			else{
				objective[i+1]=0.0;
			}
		}
		return objective;
	}
	
	
	/**
	 * formulates the equality constraint as row in constraint matrix with
	 * - charging speed in parking times
	 * - energyConsumption during driving times
	 * @param drivingPeakAndOffPeakTimesCurrentAgent
	 * @return double[] equalityConstraint
	 */
	public double[] getEqualityConstraints(double[][] drivingPeakAndOffPeakTimesCurrentAgent){
			
			double[] objective=new double [drivingPeakAndOffPeakTimesCurrentAgent.length+1];
			objective[0]=0;//SOC entry
			for (int i=0; i<drivingPeakAndOffPeakTimesCurrentAgent.length;i++){
				objective[i+1]= getChargingSpeedOrConsumptionForTimeInterval(i,drivingPeakAndOffPeakTimesCurrentAgent);
				
			}
			return objective;
		}
	
	
	
	
	public double[][] getDrivingPeakAndOffPeakTimes(double [][] peakAndOffPeakParkingTimesCurrentAgent, double [][] drivingTimesCurrentAgent, double [] drivingConsumptionCurrentAgent){
		
		double [][] combinedTimes=new double[peakAndOffPeakParkingTimesCurrentAgent.length+drivingTimesCurrentAgent.length][4];
		
		for (int i=0;  i< peakAndOffPeakParkingTimesCurrentAgent.length; i++){
			combinedTimes[i][0]=peakAndOffPeakParkingTimesCurrentAgent[i][0];
			combinedTimes[i][1]=peakAndOffPeakParkingTimesCurrentAgent[i][1];
			combinedTimes[i][2]=peakAndOffPeakParkingTimesCurrentAgent[i][2];
			combinedTimes[i][3]=peakAndOffPeakParkingTimesCurrentAgent[i][3];
		}
		for (int i=0;  i< drivingTimesCurrentAgent.length; i++){
			combinedTimes[i+peakAndOffPeakParkingTimesCurrentAgent.length][0]=drivingTimesCurrentAgent[i][0];
			combinedTimes[i+peakAndOffPeakParkingTimesCurrentAgent.length][1]=drivingTimesCurrentAgent[i][1];
			combinedTimes[i+peakAndOffPeakParkingTimesCurrentAgent.length][2]=2;
			// instead of time put driving consumption
			combinedTimes[i+peakAndOffPeakParkingTimesCurrentAgent.length][3]=-1*drivingConsumptionCurrentAgent[i];
		}
		return combinedTimes;
		
	}
	
	
	
	
	
	public double[][] getPeakAndOffPeakParkingTimes(){
		int countOfParkingEntries=0;
		ArrayList<double[][]> tempPeakAndOffPeakParkingTimes=new ArrayList<double[][]>(0);
		for (int i =0; i<parkingTimesCurrentAgent.length;i++){
			//realParkingLengthCurrentAgent[i]=myChargerInfo.getFeasibleChargingTimeInInterval(parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1], myChargerInfo.getValleyTimes());
			double [][] newArrayListEntry=myChargerInfo.getPeakAndOffPeakTimesInInterval(parkingTimesCurrentAgent[i][0], parkingTimesCurrentAgent[i][1], myChargerInfo.getValleyTimes());
			tempPeakAndOffPeakParkingTimes.add(newArrayListEntry);
			countOfParkingEntries+=newArrayListEntry.length;
		}
		
		//initialize return double[][]
		double [][] returnPeakAndOffPeakParkingTimes=new double[countOfParkingEntries][4];
		
		int row=0;
		// loop the ArrayList
		for(int i=0; i< tempPeakAndOffPeakParkingTimes.size();i++){
			double [][] temp=tempPeakAndOffPeakParkingTimes.get(i).clone();
			//loop elements in ArrayListEntry and copy to returnDoubleArray
			for (int j=0; j<temp.length; j++){
				returnPeakAndOffPeakParkingTimes[row][0]=temp[j][0];
				returnPeakAndOffPeakParkingTimes[row][1]=temp[j][1];
				returnPeakAndOffPeakParkingTimes[row][2]=temp[j][2];
				returnPeakAndOffPeakParkingTimes[row][3]=temp[j][3];
				row++;
			}
		}
		return   returnPeakAndOffPeakParkingTimes;
		
	}
	
	
	/**
	 * reads the agent specific legEnergyConsumptionList and saves entries in the double driving COnsumptionCurrentAgent
	 * @param personId
	 * @return
	 */
	public double [] getdrivingConsumptionCurrentAgent(Id personId){
			LinkedList<Double> legEnergyConsumptionList = energyConsumptionOfLegs.get(personId);
			int noOfTrips=legEnergyConsumptionList.size();
			drivingConsumptionCurrentAgent = new double[noOfTrips];
			
			for (int i=0;i<legEnergyConsumptionList.size();i++) {
				drivingConsumptionCurrentAgent[i]=legEnergyConsumptionList.get(i);
			}
			return drivingConsumptionCurrentAgent;
	}
	
	/**
	 * fills the agent specific double [][] parkingTimesCuurrentAgent with arrival and departure times
	 * and returns the respective parking lengths of all parking intervals as double []
	 * @param parkingIntervals
	 * @return returns the respective parking lengths of all parking intervals as double []
	 */
	public double[] getParkingLengthsCurrentAgent(LinkedList<ParkingIntervalInfo> parkingIntervals){
		
		// arrival- departure
		parkingTimesCurrentAgent = new double[parkingIntervals.size()][2];
		for (int i=0;i<parkingIntervals.size();i++) {
			ParkingIntervalInfo parkingIntervalInfo = parkingIntervals.get(i);
			parkingTimesCurrentAgent[i][1]=parkingIntervalInfo.getDepartureTime();
			parkingTimesCurrentAgent[i][0]=parkingIntervalInfo.getArrivalTime();
		}
		// parking lengths
		parkingLengthCurrentAgent = new double[parkingIntervals.size()];
		for (int i=0;i<parkingLengthCurrentAgent.length;i++){
			parkingLengthCurrentAgent[i]=parkingTimesCurrentAgent[i][1]-parkingTimesCurrentAgent[i][0];
			
		}
		// correction of first entry
		if (parkingLengthCurrentAgent[0]<0){
			parkingLengthCurrentAgent[0]=parkingTimesCurrentAgent[0][1]+Main.secondsPerDay-parkingTimesCurrentAgent[0][0];
		}
		return parkingLengthCurrentAgent;
}
	
	public double [][] getActualOrder(double [][] timesToOrder, int numOfColumnsPerRow){
		double [][] actualOrder = new double [timesToOrder.length][numOfColumnsPerRow];
		
		// sort actual order by time
		actualOrder= minAtStartMaxAtEnd(timesToOrder, numOfColumnsPerRow);
		return actualOrder;
		
	} 
	
	/**
	 * Recursive method to sort double array by first entry in row
	 * 
	 * @param d
	 * @param elementsPerRow
	 * @return
	 */
	public double[][] minAtStartMaxAtEnd(double [][]d, int elementsPerRow){
		int min=0;
		int max=0;
		for (int i=0; i<d.length; i++){
			if (d[i][0]<d[min][0]){
				min=i;
			}
			if(d[i][0]>d[max][0]){
				max=i;
			}
		}
		
		double[][] clone=d.clone();
		clone[0]=d[min];
		clone[d.length-1]=d[max];
		
		
		double [][] leftInMiddle=removeEntryIFromDoubleArray( d, min, elementsPerRow);
		if (min<max)
			{leftInMiddle=removeEntryIFromDoubleArray(leftInMiddle, max-1, elementsPerRow);}
		else
			{leftInMiddle=removeEntryIFromDoubleArray(leftInMiddle, max, elementsPerRow);}
		
		if(leftInMiddle.length>1){
			leftInMiddle=minAtStartMaxAtEnd(leftInMiddle, 3);
		}
		
		// insert middle back into clone
		//System.out.println(leftInMiddle.length);
		for (int i=0; i<leftInMiddle.length; i++){
			clone[i+1]=leftInMiddle[i];
		}
		return clone;
	}
	
	
	public double [][] removeEntryIFromDoubleArray(double [][] d, int i, int elementsPerRow){
		double [][] newD=new double [d.length-1][elementsPerRow];
		int count=0;
		for (int c=0; c<d.length; c++){
			if (c==i){}
			else{
				newD[count]=d[c];
				count++;
			}
		}
		return newD;
	}
	
		
		
	/**
	 * sums up all values of a 1D double array
	 * @param array
	 * @return sum
	 */
	public double sumUpEntriesOf1DDoubleArray(double[] array){
		double sum=0;
		for (int i=0; i<array.length; i++){
			sum+=array[i];
		}
		return sum;
	}
	
	/**
	 * from parking times of agent, method deducts driving times
	 * the assumption is made, that the first entry of the parkingInterval is always overnight parking, e.g. 
	 * @param parkingTimesCurrentAgent
	 * @param parkingIntervals
	 * @return double with [i][0] start Driving and [i][1] stop Driving 
	 */
	public double [][] getDrivingTimesCurrentAgent(double [][] parkingTimesCurrentAgent,LinkedList<ParkingIntervalInfo> parkingIntervals){
		double [][]drivingTimesCurrentAgent = new double[parkingIntervals.size()][2];
		
		for (int i=1; i<parkingIntervals.size();i++){
			drivingTimesCurrentAgent[i-1][0]= parkingTimesCurrentAgent[i-1][1];
			drivingTimesCurrentAgent[i-1][1]= parkingTimesCurrentAgent[i][0];
		}
		// seperate: first entry corrected for overnight parking
		drivingTimesCurrentAgent[parkingIntervals.size()-1][0]= parkingTimesCurrentAgent[parkingIntervals.size()-1][1];
		drivingTimesCurrentAgent[parkingIntervals.size()-1][1]= parkingTimesCurrentAgent[0][0];
		return drivingTimesCurrentAgent;
	}
	
	

	
	/**
	 * adds up the Consumptions of all PHEV owners and divides results by Penetration
	 * @return
	 */
		public double getAveragePHEVConsumptionInWatt(){
			double sumOfAllConsumptionsInWatt=0;
			for (Id personId : Main.vehicles.getKeySet()){
				double totalTripLengthInSecondsCurrentAgent=getTotalTripLengthOfPerson(personId);
				
				Vehicle one= Main.vehicles.getValue(personId);
				PlugInHybridElectricVehicle two= new PlugInHybridElectricVehicle(new IdImpl(1));
				
				if(areVehiclesSameClass(one, two)){ 
					// add persons consumption to totalPHEVConsumption - is a PHEV vehicle!
					double sumOfTotalAgentConsumptionInJoule=sumUpLinkedListEntries(energyConsumptionOfLegs.get(personId));
					//Joule/second=Watt
					sumOfAllConsumptionsInWatt+=sumOfTotalAgentConsumptionInJoule/totalTripLengthInSecondsCurrentAgent;
						
				}
			};
			// AverageWatt = SUm of All WattConsumptions/number of people with PHEV
			return sumOfAllConsumptionsInWatt/(populationTotal*Main.penetrationPercent);
		}
		
		/**
		 * adds up Consumption of all agents and divides by number of agents
		 * @return
		 */
			public double getAveragePHEVConsumptionInJoules(){
				double sumOfAllConsumptionsInJoules=0;
				for (Id personId : Main.vehicles.getKeySet()){
					
					Vehicle one= Main.vehicles.getValue(personId);
					PlugInHybridElectricVehicle two= new PlugInHybridElectricVehicle(new IdImpl(1));
					
					if(areVehiclesSameClass(one, two)){ 
						// add persons consumption to totalPHEVConsumption - is a PHEV vehicle!
						sumOfAllConsumptionsInJoules=sumUpLinkedListEntries(energyConsumptionOfLegs.get(personId));
							
					}
				};
				// AverageWatt = SUm of All WattConsumptions/number of people with PHEV
				return sumOfAllConsumptionsInJoules/(populationTotal*Main.penetrationPercent);
			}
		
		
		/**
		 * returns total driving time by adding up all parking times and substracting them from seconds in a day
		 * @param personId
		 * @return total driving time in seconds
		 */
		public double getTotalTripLengthOfPerson(Id personId){
			LinkedList<ParkingIntervalInfo> parkingIntervals = parkingTimeIntervals.get(personId);
			double [] parkingLengthsCurrentAgent=getParkingLengthsCurrentAgent(parkingIntervals);
			double totalParkingTime=sumUpEntriesOf1DDoubleArray(parkingLengthsCurrentAgent);
			return Main.secondsPerDay-totalParkingTime;
		}
		
		
		public double sumUpLinkedListEntries(LinkedList<Double> l){
			double sum=0;
			for (int i=0;i<l.size();i++) {
				sum += l.get(i);
			}
			return sum;
		}
		
		/**
		 * 
		 * @param one one Vehicle object
		 * @param two second Vehicle object
		 * @return boolean indicating whether objects have same class
		 */
		public boolean areVehiclesSameClass(Vehicle one, Vehicle two){
			return one.getClass().equals(two.getClass());
		}
		
	
		
		
	}

