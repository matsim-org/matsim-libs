/* *********************************************************************** *
 * project: org.matsim.*
 * ChargingSlotDistributor.java
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


import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math.optimization.OptimizationException;
import org.matsim.api.core.v01.Id;


/**
 * assigns charging slots to the required charging times (calculated in the LP) using 
 * a random number generator and the distribution of free charging slots over the day.
 * 
 * Charging Slots are of a certain given maximum length and are stored as a schedule 
 * within the parking interval
 * 
 * 
 * @author Stella
 * 
 */

public class ChargingSlotDistributor {
	/*
	 * will assign exact charging times from an agents schedule and required charging times
	 */
	
	public double minChargingLength;
	ChargingSlotDistributor(double minChargingLength){	
		this.minChargingLength=minChargingLength;
	}
	
	
	
	/**
	 * goes over every time interval in agents schedule
	 * if it is a parking interval it will assign charging slots for the interval 
	 * and returns the relevant charging schedule 
	 * 
	 * 
	 * @param schedule
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws OptimizationException
	 */
	public Schedule distribute(Id agentId, Schedule schedule) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException{
		Schedule chargingScheduleAllIntervalsAgent = new Schedule();
		
		for (int i=0; i<schedule.getNumberOfEntries(); i++){
			
			TimeInterval t= schedule.timesInSchedule.get(i);
			if(t.isParking()){
				ParkingInterval p= (ParkingInterval) t;
				double chargingTime=p.getRequiredChargingDuration();
				
				PolynomialFunction func= DecentralizedSmartCharger.myHubLoadReader.getDeterministicLoadPolynomialFunctionAtLinkAndTime(
						agentId, //agentId
						p.getLocation(),
						p);
				//Id agentId, Id idLink, TimeInterval t
				
				if(p.getIntervalLength()*0.7 <chargingTime){
					if(chargingTime>p.getIntervalLength()){
						System.out.println("trouble");
					}
					double diff= p.getIntervalLength()-chargingTime;
					double startRand= Math.random()*diff;
					//(System.out.println("assign charging time "+ startRand + " to "+  startRand+chargingTime);
					
					ChargingInterval c= new ChargingInterval(p.getStartTime()+startRand, p.getStartTime()+startRand+chargingTime);
					chargingScheduleAllIntervalsAgent.addTimeInterval(c);
					
					Schedule chargingScheduleForParkingInterval= new Schedule();
					chargingScheduleForParkingInterval.addTimeInterval(c);
					p.setChargingSchedule(chargingScheduleForParkingInterval);
					
				}else{
					Schedule chargingScheduleForParkingInterval= 
						assignChargingScheduleForParkingInterval(func, 
							p.getJoulesInInterval(), 
							p.getStartTime(), 
							p.getEndTime(), 
							chargingTime);
					
					p.setChargingSchedule(chargingScheduleForParkingInterval);
					
					chargingScheduleAllIntervalsAgent.mergeSchedules(chargingScheduleForParkingInterval);
					
				}
				
			}
		}
	
		return chargingScheduleAllIntervalsAgent;
	}
	
	
	
	/*
	 * function is called from within distribute() for all parkinIntervals where the required charging time shall be distributed
	 * using a Random Number generator
	 * make schedule for single time interval
	 */
	public Schedule assignChargingScheduleForParkingInterval(PolynomialFunction func, 
			double joulesInInterval, 
			double startTime, 
			double endTime, 
			double chargingTime) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException{
		//
		Schedule chargingInParkingInterval = new Schedule();
		int intervals=(int) Math.ceil(chargingTime/minChargingLength);
				
		
		for(int i=0; i<intervals; i++){
			
			double bit=0;
			
			if(i<intervals-1){
				bit=minChargingLength;
				
			}else{// i=intervals-1
				bit=chargingTime- (intervals-1)*minChargingLength;
				
			}
				
				chargingInParkingInterval=assignRandomChargingSlotInChargingInterval(func, 
						startTime, 
						endTime, 
						joulesInInterval, 
						bit, 
						chargingInParkingInterval);
			
			
			
		}
		
		return chargingInParkingInterval;
	}
	
	
	
	
	/**
	 * makes random number RN
	 * finds a time in interval that corresponds to RN according to available free slot distribution
	 * creates a charging slot and saves it in charging schedule, if it does not overlap with already existing charging times
	 * 
	 * @param func
	 * @param startTime
	 * @param endTime
	 * @param joulesInInterval
	 * @param bit
	 * @param chargingInParkingInterval
	 * @return
	 * @throws MaxIterationsExceededException
	 * @throws FunctionEvaluationException
	 * @throws IllegalArgumentException
	 * @throws OptimizationException
	 */
	public Schedule assignRandomChargingSlotInChargingInterval(PolynomialFunction func, 
			double startTime, 
			double endTime, 
			double joulesInInterval, 
			double bit, 
			Schedule chargingInParkingInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException, OptimizationException{
		// random and test
		
		boolean notFound=true;
		boolean run=true;
		
		
		double upper=endTime;
		double lower=startTime;
		double trial=(upper+lower)/2;
		
		//System.out.println("Find Slot of "+ bit +" in Interval "+ startTime+ " to "+ endTime);
		
		while(notFound){
			
			run=true;			
			
			upper=endTime;
			lower=startTime;
			trial=(upper+lower)/2;
			
			double rand=Math.random();
			//double rand=Math.random()*joulesInInterval;
			
			while(run){
				double integral;
				if(joulesInInterval>=0){
					double err=joulesInInterval/1000; // accuracy 0.1%
					
					integral=DecentralizedSmartCharger.functionIntegrator.integrate(func, startTime, trial);
					
					if(integral<rand*joulesInInterval){
						lower=trial;					
						trial=(upper+lower)/2;
						
					}else{
						upper=trial;
						trial=(upper+lower)/2;
					}
					
					if(Math.abs(integral-rand*joulesInInterval)<=err){
						run=false;
					}
					
				}else{
					
					// negative suboptimal interval
					PolynomialFunction funcSubOpt= turnSubOptimalSlotDistributionIntoProbDensityOfFindingAvailableSlot(func, startTime, endTime);
					
					integral=DecentralizedSmartCharger.functionIntegrator.integrate(
							funcSubOpt, 
							startTime, 
							trial);
					
					double fullSubOptIntegral= DecentralizedSmartCharger.functionIntegrator.integrate(
							funcSubOpt, startTime, endTime);
					
					double err=fullSubOptIntegral/1000; // accuracy 0.1%
					
					if(integral<rand*fullSubOptIntegral){
						lower=trial;					
						trial=(upper+lower)/2;
						
					}else{
						upper=trial;
						trial=(upper+lower)/2;
					}
					
					if(Math.abs(integral-rand*fullSubOptIntegral)<=err){
						run=false;
					}
					
				}
				
			}
			
			ChargingInterval c1;
			if(trial+bit>endTime){
				c1=null;
				//c1= new ChargingInterval(endTime-bit, endTime );
			}else{
				c1= new ChargingInterval(trial, trial+bit);
			}
			//System.out.println("Assigned Slot of "+ c1.getStartTime() + " to "+ c1.getEndTime());
			
			if(c1!=null && chargingInParkingInterval.overlapWithTimeInterval(c1)==false){
				//no overlap--> exit loop
				
				notFound=false;
				chargingInParkingInterval.addTimeInterval(c1);
				
			}else{
				
				
			}
			//otherwise notFound remains true and it runs again
			
		}
		
		return chargingInParkingInterval;
		
	}
	
	
	
	public PolynomialFunction turnSubOptimalSlotDistributionIntoProbDensityOfFindingAvailableSlot(PolynomialFunction func, double start1, double end1) throws OptimizationException{
		
		double start= Math.floor(start1);
		double end= Math.ceil(end1);
		int steps= ((int) end)-((int)start);
		
		double [][] newFunc= new double[steps][2];
		
		for(int i=0; i<steps; i++){
			newFunc[i][0]=start+i;
			newFunc[i][1]=(-1)/func.value(start+i);
		}
		
		return DecentralizedSmartCharger.fitCurve(newFunc);
		
		
	}
	
	
}
