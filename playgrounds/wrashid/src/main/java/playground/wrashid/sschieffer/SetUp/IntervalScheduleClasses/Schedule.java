/* *********************************************************************** *
 * project: org.matsim.*
 * Schedule.java
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
package playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.matsim.api.core.v01.Id;

import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;


/**
 * A schedule is a LinkedList of timeIntervals.
 * It helps to store related time intervals or activties in one object
 * i.e. to keep the daily plans of an agent and to sort his activities.
 * or to store all charging intervals during one parking interval
 * 
 * @author Stella
 *
 */
public class Schedule {
	
	public double totalJoulesInOptimalParkingTimes=0;
	public double totalJoulesInSubOptimalParkingTimes=0;
	private double startingSOC;
	
	public LinkedList<TimeInterval> timesInSchedule= new LinkedList<TimeInterval>();
	
	
	public Schedule(){
		
	}
	
	
	public Schedule(TimeInterval t){
		timesInSchedule.add(t);
	}
	
	public void clearJoules(){
		totalJoulesInOptimalParkingTimes=0;
		totalJoulesInSubOptimalParkingTimes=0;
		clearJoulesInIntervals();
		
	}
	
	
	private void clearJoulesInIntervals(){
		for(int i=0; i<getNumberOfEntries(); i++){
			TimeInterval t = timesInSchedule.get(i);
			
			if(t.isParking()){
				((ParkingInterval) t).setJoulesInPotentialChargingInterval(0.0);
				
			}
		}
	}
	
	
	
	public int getNumberOfEntries(){
		return timesInSchedule.size();
	}


	public double getStartingSOC(){
		return startingSOC;
	}


	public double getTotalBatteryConsumption(){
		
		double total=0;
		for(int i=0; i<getNumberOfEntries();i++){
			if(timesInSchedule.get(i).isDriving()){
				total+=((DrivingInterval) timesInSchedule.get(i)).getBatteryConsumption();
			}
		}
		return total;
	}
	
	
	
	public double getTotalConsumptionFromEngine(){
		
		double total=0;
		for(int i=0; i<getNumberOfEntries();i++){
			if(timesInSchedule.get(i).isDriving()){
				total+=((DrivingInterval) timesInSchedule.get(i)).getExtraConsumption();
			}
		}
		return total;
	}
	
	
	
	public double getTotalDrivingTimeWithoutEnginePower(){
		
		double total=0;
		for(int i=0; i<getNumberOfEntries();i++){
			if(timesInSchedule.get(i).isDriving()){
				total+=((DrivingInterval) timesInSchedule.get(i)).getEngineTime();
			}
		}
		return total;
	}
	
	
	
	
	public double getTotalTimeOfIntervalsInSchedule(){
		double totalTime=0;
		
		for(int i=0; i<getNumberOfEntries();i++){
			totalTime=totalTime+timesInSchedule.get(i).getIntervalLength();
		} 
		return totalTime;
	}
	
	
	
	public void addTimeInterval(TimeInterval t){
		timesInSchedule.add(t);
	}
	
	public void sort(){
		Collections.sort(timesInSchedule);
	}
	
	public void printSchedule(){
		System.out.println("*************************");
		System.out.println("Starting SOC: "+ getStartingSOC());
		for(TimeInterval t: timesInSchedule){
			t.printInterval();
		}
		System.out.println("*************************");
		
	}
	
	public void mergeSchedules(Schedule schedule2){
		for(int i=0; i< schedule2.getNumberOfEntries(); i++){
			timesInSchedule.add(schedule2.timesInSchedule.get(i));
		}
		
		this.sort();
	}
	
	
	public void setStartingSOC(double SOC){
		startingSOC=SOC;
	}
	
	
	
	public XYSeries makeXYSeriesFromLoadSchedule(String name){
		XYSeries xy = new XYSeries(name);
		for(int entry=0; entry<getNumberOfEntries();entry++){
			TimeInterval t= timesInSchedule.get(entry);
			
			if(t.isLoadDistributionInterval()){
				LoadDistributionInterval t2=(LoadDistributionInterval) t;
				for(double a=t2.getStartTime(); a<=t2.getEndTime();){
					xy.add(a, t2.getPolynomialFunction().value(a));
					a+=60;//in one minute bins
				}
			}
		}
		return xy;
	}
	
	
	/**
	 * only meant for schedules with loaddistributionIntervals,
	 * 
	 * passed String is title String of diagram
	 * @param titleGraph
	 * @throws IOException
	 */
	
	public void visualizeLoadDistribution(String titleGraph, String saveAs) throws IOException{
		XYSeriesCollection loadDistributionIntervals = new XYSeriesCollection();
			
		loadDistributionIntervals.addSeries(makeXYSeriesFromLoadSchedule(""));
			
        
        JFreeChart chart = ChartFactory.createXYLineChart(
        		titleGraph, "time of day", "free load [W]", loadDistributionIntervals, 
        		PlotOrientation.VERTICAL, false, true, false);
        
        final XYPlot plot = chart.getXYPlot();
        plot.setDrawingSupplier(DecentralizedSmartCharger.supplier);
    
        plot.getRenderer().setSeriesPaint(0, Color.black);
        plot.getRenderer().setSeriesStroke(
	            0, 
	            new BasicStroke(
	                2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 
	                1.0f, new float[] {10.0f, 6.0f}, 5.0f
	            )
	        );
	   
	    
        chart.setBackgroundPaint(Color.white);
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.gray); 
        plot.setRangeGridlinePaint(Color.gray); 
      
        ChartUtilities.saveChartAsPNG(
        		new File(saveAs) , //DecentralizedSmartCharger.outputPath+"Hub\\"+ name+".png"
        		chart, 800, 600);
       
	}
	
	
	/**
	 * checks if there is an overlap with any of the Timeintervals in the schedule and the specified TimeInterval
	 * 
	 * returns true if overlap
	 * false if no overlap
	 * 
	 * @param t
	 * @return
	 */
	public boolean overlapWithTimeInterval(TimeInterval t){
		boolean overlap=false;
		
		for(int i=0; i<timesInSchedule.size(); i++){
			TimeInterval thisT= timesInSchedule.get(i);
			
			if( thisT.overlap(t)){
				//if overlap
				overlap=true;
				i=timesInSchedule.size();
			}
		}
		
		return overlap;
	}
	
	
	
	public Schedule getOverlapWithLoadDistributionInterval(LoadDistributionInterval t){
		Schedule overlap= new Schedule();
		
		for(int i=0; i<timesInSchedule.size(); i++){
			TimeInterval thisT= timesInSchedule.get(i);
			
			if( thisT.overlap(t)){
				overlap.addTimeInterval(thisT.ifOverlapWithLoadDistributionIntervalReturnOverlap(t));
			}
		}
		overlap.sort();
		return overlap;
	}
	
	
	/**
	 * start <= time < end
	 * (unless last interval where time can be equal to end)
	 * @param time
	 * @return
	 */
	public int timeIsInWhichInterval(double time){
		
		int solution=-1;
		for (int i=0; i<timesInSchedule.size(); i++){
			if(time<timesInSchedule.get(i).getEndTime() && 
					time>=timesInSchedule.get(i).getStartTime()){
				solution =i;
			}				
		}
		if (getNumberOfEntries()==0){
			System.out.println("Find time in schedule fails because schedule length ==0: ");
			printSchedule();
			System.out.println("time: "+time);
		}else{
			if(time==timesInSchedule.get(timesInSchedule.size()-1).getEndTime()){
				solution =timesInSchedule.size()-1;
			}
		}
		
		if(solution ==-1){
			System.out.println("timeIsInWhichInterval should not be -1");
			System.out.println("time: "+time);
			printSchedule();
		}
		return solution;
	}
	
	
	
	public int timeIsInWhichParkingInterval(double time){
		int solution=-1;
		for (int i=0; i<timesInSchedule.size(); i++){
			TimeInterval thisInteral= timesInSchedule.get(i);
			if(thisInteral.isParking()){
				if(thisInteral.timeIsEqualOrGreaterThanStartAndEqualOrSmallerThanEnd(time)){
					solution =i;
				}
			}
						
		}
		return solution;
	}
	
	
	/**
	 * returns ArrayList of integers of all intervals in schedule 
	 * @param t
	 * @return
	 */
	public ArrayList <Integer> intervalIsInWhichTimeInterval(TimeInterval t){
		ArrayList <Integer> intervalList = new ArrayList<Integer>(0);
		
		for (int i=0; i<timesInSchedule.size(); i++){
			if(timesInSchedule.get(i).overlap(t)){
				intervalList.add(i);
			}
		}	
			
		return intervalList;
	}
	
	
	
	public void addJoulesToTotalSchedule(double joules){
		if(joules<=0){
			totalJoulesInSubOptimalParkingTimes+=joules;
		}
		else{
			totalJoulesInOptimalParkingTimes+=joules;
		}
	}
	
	
	public int numberOfDrivingTimes(){
		int count=0;
		for(int i=0; i<timesInSchedule.size(); i++){
			if(timesInSchedule.get(i).isDriving()){
				count++;
			}
		}
		return count;
	}
	
	
	public int positionOfIthDrivingTime(int ithTime){
		int sol=0;
		int count=0;
		//this.printSchedule();
		
		for(int i=0; i<timesInSchedule.size(); i++){
			
			if(timesInSchedule.get(i).isDriving()){
				if(count == ithTime){
					sol= i;
					i=timesInSchedule.size();
					break;
				}
				
				if(count<ithTime){
					count++;
				
				}
			}
		}
		return sol;
	}
	
	
	/**
	 * checks if second is within one interval of the schedule
	 * true if in interval
	 * 
	 * @param sec
	 * @return
	 */
	public boolean isSecondWithinOneInterval(double sec){
		boolean inInterval=false;
		
		for(int i=0; i<getNumberOfEntries(); i++){
			TimeInterval t= timesInSchedule.get(i);
			if(sec<=t.getEndTime() && sec>=t.getStartTime()){
				// in interval
				inInterval=true;
			}
		}
		return inInterval;
		
	}
	
	
	public boolean sameTimeIntervalsInThisSchedule(Schedule s){
		boolean isSame=false;
		
		if (this.getNumberOfEntries()!=s.getNumberOfEntries()){
			
			return  isSame;
		}else{
			
			for(int i=0; i<this.getNumberOfEntries();i++){
				if(this.timesInSchedule.get(i).getStartTime()==s.timesInSchedule.get(i).getStartTime()){
					isSame=true;
				}else{return false;}
				
				if(this.timesInSchedule.get(i).getEndTime()==s.timesInSchedule.get(i).getEndTime()){
					isSame=true;
				}else{return false;}
				
			}
		}
		
		return isSame;
		
		
	}
	
	
	
	public void insertChargingIntervalsIntoParkingIntervalSchedule(Schedule intervalsToPutIn){
		
		for(int i=0; i<intervalsToPutIn.getNumberOfEntries(); i++){
		/*	System.out.println("InsertChargingInterval: Schedule into which to insert:");
			printSchedule();
			System.out.println("InsertChargingInterval: ChargingSChedule to insert:");
			intervalsToPutIn.printSchedule();*/
			
			TimeInterval t= intervalsToPutIn.timesInSchedule.get(i);
			
			 ArrayList <Integer> intervals= intervalIsInWhichTimeInterval(t);
			 
			 //for each overlap
			 
			 Schedule newChargingAndParkingIntervals= new Schedule();
			 int correction=0;
			 // delete Interval from Schedule and add new Intervals into newChargingAndParkingIntervals
			 for(int currentInt=0; currentInt<intervals.size();currentInt++){
				 
				 	double start=timesInSchedule.get(intervals.get(currentInt)).getStartTime();//currently in schedule
					double end=timesInSchedule.get(intervals.get(currentInt)).getEndTime();
					
					Id linkId = ((ParkingInterval) timesInSchedule.get(intervals.get(currentInt))).getLocation();
					boolean type= ((ParkingInterval) timesInSchedule.get(intervals.get(currentInt))).isInSystemOptimalChargingTime();
						
					//before 
					if(t.getStartTime()>start){
						ParkingInterval p= new ParkingInterval(start, t.getStartTime(), linkId);
						p.setParkingOptimalBoolean(type);
						p.setRequiredChargingDuration(0);
						newChargingAndParkingIntervals.addTimeInterval(p);
					}
					
					//charging
					if(t.getStartTime()>start){
						ChargingInterval p= new ChargingInterval(t.getStartTime(),t.getEndTime());
						
						newChargingAndParkingIntervals.addTimeInterval(p);
					}
					
					//after
					if(end>t.getEndTime()){
						ParkingInterval p= new ParkingInterval(t.getEndTime(),end, linkId);
						p.setParkingOptimalBoolean(type);
						p.setRequiredChargingDuration(0);
						newChargingAndParkingIntervals.addTimeInterval(p);
					}
					
					deleteIntervalAtEntry(intervals.get(currentInt)-correction);
					 correction++;
			 }
			 
			mergeSchedules(newChargingAndParkingIntervals);
			/*System.out.println("after adding  charging Interval");
			printSchedule();*/
			
		}
		
	}
	
	
	public void addLoadDistributionIntervalToExistingLoadDistributionSchedule(LoadDistributionInterval loadToAdd) 
	throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule newSchedule= new Schedule();
		
		//get overlap by cutting
		try{
			Schedule before= this.cutScheduleAtTimeWithoutJouleReassignment(loadToAdd.getStartTime());
			Schedule after= this.cutScheduleAtTimeSecondHalfWithoutJouleReassignment(loadToAdd.getEndTime(), 0.0);
			Schedule overlap= this.cutScheduleAtTimeWithoutJouleReassignment(loadToAdd.getEndTime());
			overlap= overlap.cutScheduleAtTimeSecondHalfWithoutJouleReassignment(loadToAdd.getStartTime(), 0.0);
			
			LoadDistributionInterval loadLeftToAdd= loadToAdd.clone();
			
			for(int i=0; i<overlap.getNumberOfEntries();i++){
				LoadDistributionInterval t=  (LoadDistributionInterval)overlap.timesInSchedule.get(i);
				if( t.overlap(loadLeftToAdd) ){
					
					LoadDistributionInterval localOverlap=
						t.ifOverlapWithLoadDistributionIntervalReturnOverlap(loadLeftToAdd);
					
					PolynomialFunction combinedFunc= t.getPolynomialFunction().add(loadLeftToAdd.getPolynomialFunction());
					boolean newOptimal;
					if(DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(combinedFunc, 
							loadLeftToAdd.getStartTime(), 
							t.getEndTime())>=0){
						newOptimal=true;
					}else{
						newOptimal=false;
					}
					
					newSchedule.addTimeInterval(new LoadDistributionInterval(localOverlap.getStartTime(), 
							localOverlap.getEndTime(),
							combinedFunc, newOptimal)
							);
					loadLeftToAdd=new LoadDistributionInterval(localOverlap.getEndTime(), 
							loadLeftToAdd.getEndTime(),
							loadLeftToAdd.getPolynomialFunction(), loadLeftToAdd.isOptimal());
				}
			}
			before.mergeSchedules(newSchedule);
			before.mergeSchedules(after);
			timesInSchedule=before.timesInSchedule;
		}catch(Exception e){
			System.out.println("Problem in addLoadDistributionIntervalToExistingLoadDistributionSchedule");
			System.out.println("adding load interval");
			loadToAdd.printInterval();
			printSchedule();
		}
		
		
		
	}
	
	
	
	public Schedule cloneSchedule(){
		Schedule copy= new Schedule();
		for(int i=0; i<getNumberOfEntries(); i++){
			TimeInterval l= timesInSchedule.get(i);
			copy.addTimeInterval(l.clone());
		}
		return copy;
	}
	
	
	public void deleteIntervalAtEntry(int i){
		timesInSchedule.remove(i);
	}
	
	
	
	
	public Schedule cutScheduleAtTime(double time, Id agentId) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule firstHalf=new Schedule();
		
		firstHalf= cutScheduleAtTimeWithoutJouleReassignment(time);
		firstHalf.getJoulesForEachParkingInterval(agentId);
		return firstHalf;
	}
	
	
	
public Schedule cutScheduleAtTimeWithoutJouleReassignment(double time) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		Schedule firstHalf=new Schedule();
		
		firstHalf.setStartingSOC(getStartingSOC());
		int interval= timeIsInWhichInterval(time); 
		
		for(int i=0; i<=interval-1; i++){
			firstHalf.addTimeInterval(timesInSchedule.get(i).clone());
			
		}
		
		//last interval To Be Cut
		TimeInterval lastInterval= timesInSchedule.get(interval);
		if(lastInterval.isCharging()){
			System.out.println("Exception in cutScheduleAtTimeWithoutJouleReassignment - should not have charging schedules");
		}else {
			if (lastInterval.isDriving()){
				
				DrivingInterval d= new DrivingInterval(lastInterval.getStartTime(), 
						time, 
						((DrivingInterval)lastInterval).getBatteryConsumption() * (time- lastInterval.getStartTime())/lastInterval.getIntervalLength()
						);
				if(d.getIntervalLength()>0){
					firstHalf.addTimeInterval(d);
				}
				
			}else{
				if(lastInterval.isLoadDistributionInterval()){
					LoadDistributionInterval l= new LoadDistributionInterval(lastInterval.getStartTime(), 
							time, 
							((LoadDistributionInterval)lastInterval).getPolynomialFunction(), 
							((LoadDistributionInterval)lastInterval).isOptimal()
							);
					if(l.getIntervalLength()>0){
						firstHalf.addTimeInterval(l);
					}
					
				}else{
					//PARKING
					ParkingInterval p= new ParkingInterval(lastInterval.getStartTime(), 
							time, 
							((ParkingInterval)lastInterval).getLocation() 
							);
					p.setParkingOptimalBoolean(((ParkingInterval)lastInterval).isInSystemOptimalChargingTime());
					
					if(((ParkingInterval)lastInterval).getChargingSchedule()!= null){
						p.setChargingSchedule(
								((ParkingInterval)lastInterval).getChargingSchedule().cutChargingScheduleAtTime(time));
						
						double totalTimeChargingInP= p.getChargingSchedule().getTotalTimeOfIntervalsInSchedule();
						p.setRequiredChargingDuration(totalTimeChargingInP);
					}else{
						p.setRequiredChargingDuration(0);
					}
						
					if(p.getIntervalLength()>0){
						firstHalf.addTimeInterval(p);
					}
				}
			}	
			
		}
		//firstHalf.printSchedule();
		return firstHalf;
	}

	/**
	 * cuts the schedule and reassigns joules to the parking intervals
	 * 
	 * @param time
	 * @param startingSOC
	 * @param agentId
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 */
	public Schedule cutScheduleAtTimeSecondHalf (double time, double startingSOC, Id agentId) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule secondHalf=new Schedule();
		secondHalf=cutScheduleAtTimeSecondHalfWithoutJouleReassignment(time,  startingSOC);
		secondHalf.getJoulesForEachParkingInterval(agentId);
		return secondHalf;
		
	}

	
	
	public Schedule cutScheduleAtTimeSecondHalfWithoutJouleReassignment(double time, double startingSOC) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule secondHalf=new Schedule();
		secondHalf.setStartingSOC(startingSOC);
		int interval= timeIsInWhichInterval(time); 
		if (interval==-1){
			System.out.println("interval== -1 error");
			System.out.println("Schedule:");
			printSchedule();
			System.out.println("Cutting at time: "+ time);
		}
		//add first
		TimeInterval firstInterval= timesInSchedule.get(interval);
		if (firstInterval.isDriving()){
			
			DrivingInterval d= new DrivingInterval(time, firstInterval.getEndTime(), 
					((DrivingInterval)firstInterval).getBatteryConsumption() * (firstInterval.getEndTime()-time )/firstInterval.getIntervalLength()
			);
			if(d.getIntervalLength()>0){// time=firstInterval.endtime
				secondHalf.addTimeInterval(d);
			}
			
		}else{
			if(firstInterval.isLoadDistributionInterval()){
				//LOADDISTRIBUTION
				LoadDistributionInterval l= new LoadDistributionInterval(time, firstInterval.getEndTime(),						
						((LoadDistributionInterval)firstInterval).getPolynomialFunction(), 
						((LoadDistributionInterval)firstInterval).isOptimal()
						);
				if(l.getIntervalLength()>0){
					secondHalf.addTimeInterval(l);
				}
				
			}else{
				//PARKING
				ParkingInterval p= new ParkingInterval(time, firstInterval.getEndTime(), 
						((ParkingInterval)firstInterval).getLocation() 
						);
				p.setParkingOptimalBoolean(((ParkingInterval)firstInterval).isInSystemOptimalChargingTime());
				
				if(((ParkingInterval)firstInterval).getChargingSchedule()!= null){
					p.setChargingSchedule(
							((ParkingInterval)firstInterval).getChargingSchedule().cutChargingScheduleAtTimeSecondHalf(time));
					p.setRequiredChargingDuration(p.getChargingSchedule().getTotalTimeOfIntervalsInSchedule());
				}else{
					p.setRequiredChargingDuration(0);
				}
				
				if(p.getIntervalLength()>0){
					secondHalf.addTimeInterval(p);
				}
			}	
		}
		
		for(int i=interval+1; i<getNumberOfEntries(); i++){
			secondHalf.addTimeInterval(timesInSchedule.get(i).clone());
			
		}
		secondHalf.sort();	
		//secondHalf.printSchedule();
		return secondHalf;
		
	}

	/**
	 * makes a clone of first half of charging schedule
	 * @param time
	 * @return
	 */
	public Schedule cutChargingScheduleAtTime(double time){
		Schedule newCharging= new Schedule();
		for(int i=0; i<getNumberOfEntries(); i++){
			if(time > timesInSchedule.get(i).getEndTime()){
				//add full interval
				
				newCharging.addTimeInterval(new ChargingInterval(timesInSchedule.get(i).getStartTime(), 
						timesInSchedule.get(i).getEndTime()));
				
				
			}
			if(time > timesInSchedule.get(i).getStartTime() && time <= timesInSchedule.get(i).getEndTime()){
				//only take first half
				newCharging.addTimeInterval(new ChargingInterval(timesInSchedule.get(i).getStartTime(), time));
			}
			
		}
		return newCharging;
		
	}
	
	
	/**
	 * returns a clone of the second half of the charging schedule
	 * @param time
	 * @return
	 */
	public Schedule cutChargingScheduleAtTimeSecondHalf(double time){
		Schedule newCharging= new Schedule();
		for(int i=0; i<getNumberOfEntries(); i++){
			if(time < timesInSchedule.get(i).getStartTime()){
				//add full interval
				newCharging.addTimeInterval(new ChargingInterval(timesInSchedule.get(i).getStartTime(), 
						timesInSchedule.get(i).getEndTime()));
			}
			if(time > timesInSchedule.get(i).getStartTime() && time <= timesInSchedule.get(i).getEndTime()){
				//only take 2nd half
				newCharging.addTimeInterval(new ChargingInterval(time, timesInSchedule.get(i).getEndTime()));
			}
			
		}
		return newCharging;
		
	}
	
	
	
	
	public Schedule fillNonExistentTimesInLoadScheduleWithZeroLoads(){
		Schedule filled= new Schedule();
		
		filled.mergeSchedules(this.cloneSchedule());
		
		// fill beginning 
		if(timesInSchedule.get(0).getStartTime()>0.0){
			filled.addTimeInterval(new LoadDistributionInterval(0.0, 
					timesInSchedule.get(0).getStartTime(), 
					new PolynomialFunction(new double[]{0.0}),false));
		}
				
		for(int i=1; i< getNumberOfEntries(); i++){
			// if there is a distante between consecutive intervals fill it
			if(timesInSchedule.get(i).getStartTime()- timesInSchedule.get(i-1).getEndTime()>0.0){
				filled.addTimeInterval(new LoadDistributionInterval(
						 timesInSchedule.get(i-1).getEndTime(), 
						 timesInSchedule.get(i).getStartTime(), 
						new PolynomialFunction(new double[]{0.0}),false));
			}
		}
		
		// up to end
		if(timesInSchedule.get(getNumberOfEntries()-1).getEndTime()<DecentralizedSmartCharger.SECONDSPERDAY){
			filled.addTimeInterval(new LoadDistributionInterval(
					timesInSchedule.get(getNumberOfEntries()-1).getEndTime(), 
					DecentralizedSmartCharger.SECONDSPERDAY, 
					new PolynomialFunction(new double[]{0.0}),false));
		}
		filled.sort();
		//filled.printSchedule();
		return filled;
	}
	
	
	
public void getJoulesForEachParkingInterval(Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
	clearJoules();
		for(int i=0; i<getNumberOfEntries(); i++){
			
			if(timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)timesInSchedule.get(i);
				// getFunctionFromHubReader
				Id idLink= thisParkingInterval.getLocation();
				
				ArrayList <PolynomialFunction> funcList= 
					DecentralizedSmartCharger.myHubLoadReader.getDeterministicLoadPolynomialFunctionAtLinkAndTime(
							id,
							idLink, 
							thisParkingInterval);
				
				
				PolynomialFunction p= funcList.get(0);
				// size can only be 1
				if( funcList.size()>1){
					System.out.println("in getJoulesForEachParkingInterval funcList was larger than 1 ERROR");
					printSchedule();
				}
				
				//Integrate from start to End
				double joulesInInterval=DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(p, 
						thisParkingInterval.getStartTime(), 
						thisParkingInterval.getEndTime());
				
				addJoulesToTotalSchedule(joulesInInterval);
				//save results in Parking Interval
				thisParkingInterval.setJoulesInPotentialChargingInterval(joulesInInterval);
				
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
	public void addExtraConsumptionDriving( int pos, double extraTime, double extraC){
		for(int i=pos;i>0; i--){
			
			if(timesInSchedule.get(i).isDriving()){
				
				DrivingInterval thisD = (DrivingInterval) timesInSchedule.get(i);
				if(thisD.getIntervalLength()>=extraTime ){
					
					thisD.setExtraConsumption(extraC, extraTime);
					i=0;
					
				}else{
					double consLeft= extraC- thisD.getBatteryConsumption();
					double timeLeft= extraTime-thisD.getIntervalLength();
					
					addExtraConsumptionDriving( i, timeLeft, consLeft);
					thisD.setExtraConsumption(thisD.getBatteryConsumption(), thisD.getIntervalLength());
					i=0;
				}
			}
		}
		
		
	}
	
	
	/**
	 * reduces the req charging times in preceding parking time(s) by given value
	 * @param s
	 * @param pos
	 * @param deduct
	 */
	public void reducePrecedingParkingBy( int pos, double deduct){
			
		for(int i=pos-1;i>0; i--){
			
			if(timesInSchedule.get(i).isParking()){
				
				ParkingInterval thisP = (ParkingInterval) timesInSchedule.get(i);
				
				if(thisP.getRequiredChargingDuration()>=deduct ){
					
					thisP.setRequiredChargingDuration(thisP.getRequiredChargingDuration()-deduct);
					i=0;
				}else{
					
					double stillLeft= deduct-thisP.getRequiredChargingDuration();
					thisP.setRequiredChargingDuration(0);
					reducePrecedingParkingBy( i, stillLeft);
					i=0;
				}
				
			}
		}
			
	}
	
	
	
	public void cleanUpLoadSchedule(){
		Schedule newSchedule= new Schedule();
		
		for(int i=0; i<getNumberOfEntries()-1; i++){
			LoadDistributionInterval l1=(LoadDistributionInterval)timesInSchedule.get(i);
			LoadDistributionInterval l2=(LoadDistributionInterval)timesInSchedule.get(i+1);
			
			if(l1.haveSamePolynomialFuncCoeffs(l2)){
				//merge them
				newSchedule.addTimeInterval(new LoadDistributionInterval(l1.getStartTime(), 
						l2.getEndTime(), 
						l1.getPolynomialFunction(),l1.isOptimal()));
				i++;
			}else{
				newSchedule.addTimeInterval(l1);
				if(i==getNumberOfEntries()-2){
					newSchedule.addTimeInterval(l2);
				}
			}
		}
		this.timesInSchedule=newSchedule.timesInSchedule;
	}
	
	
	
	public static Schedule makeScheduleFromArrayListLoadIntervals(ArrayList<LoadDistributionInterval> list){
		Schedule stochasticSchedule= new Schedule();
		for(int i=0; i< list.size(); i++){
			stochasticSchedule.addTimeInterval(list.get(i));
		}
		stochasticSchedule=stochasticSchedule.fillNonExistentTimesInLoadScheduleWithZeroLoads();
		return stochasticSchedule;
	}
	
}
