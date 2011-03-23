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
package playground.wrashid.sschieffer.V1G;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;


public class ChargingSlotDistributor {
	/*
	 * will assign exact charging times from an agents schedule and required charging times
	 */
	ChargingSlotDistributor(){		
	}
	
	
	
	Schedule distribute(Schedule schedule) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule chargingScheduleAgent = new Schedule();
		
		for (int i=0; i<schedule.getNumberOfEntries(); i++){
			
			TimeInterval t= schedule.timesInSchedule.get(i);
			if(t.isParking()){
				ParkingInterval p= (ParkingInterval) t;
				double chargingTime=p.getRequiredChargingDuration();
				
				PolynomialFunction func= DecentralizedV1G.myHubLoadReader.getPolynomialFunctionAtLinkAndTime(p.getLocation(),
						p.getStartTime(),
						p.getEndTime());
				
				
				//System.out.println("assign charging schedule between "+ p.getStartTime() + " to"+ p.getEndTime()+ " for "+ chargingTime+ " seconds");
				
				// if too constricted the distribution will not work, overlaps!!!
				if(p.getIntervalLength()*0.7 <chargingTime){
					
					double diff= p.getIntervalLength()-chargingTime;
					double startRand= Math.random()*diff;
					//(System.out.println("assign charging time "+ startRand + " to "+  startRand+chargingTime);
					chargingScheduleAgent.addTimeInterval(new ChargingInterval(p.getStartTime()+startRand, p.getStartTime()+startRand+chargingTime));
					
				}else{
					chargingScheduleAgent.mergeSchedules(assignChargingScheduleForParkingInterval(func, 
							p.getJoulesInInterval(), 
							p.getStartTime(), 
							p.getEndTime(), 
							chargingTime));
				}
				
				
			}
			
			//System.out.println("added charging time: "); 
			//chargingScheduleAgent.printSchedule();
		}
	
		return chargingScheduleAgent;
	}
	
	
	
	/*
	 * make schedule for single time interval
	 */
	public Schedule assignChargingScheduleForParkingInterval(PolynomialFunction func, 
			double joulesInInterval, 
			double startTime, 
			double endTime, 
			double chargingTime) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		//
		Schedule chargingInParkingInterval = new Schedule();
		int intervals=(int) Math.ceil(chargingTime/Main.MINCHARGINGLENGTH);
				
		
		for(int i=0; i<intervals; i++){
			
			double bit=0;
			
			if(i<intervals-1){
				bit=Main.MINCHARGINGLENGTH;
				
			}else{// i=intervals-1
				bit=chargingTime- (intervals-1)*Main.MINCHARGINGLENGTH;
				
			}
			
			chargingInParkingInterval=assignRandomChargingSlotForMinChargingInterval(func, 
					startTime, 
					endTime, 
					joulesInInterval, 
					bit, 
					chargingInParkingInterval);
			
		}
		
		return chargingInParkingInterval;
	}
	
	
	
	
	public Schedule assignRandomChargingSlotForMinChargingInterval(PolynomialFunction func, 
			double startTime, 
			double endTime, 
			double joulesInInterval, 
			double bit, 
			Schedule chargingInParkingInterval) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		// random and test
		
		boolean notFound=true;
		boolean run=true;
		
		double err=joulesInInterval/1000; // accuracy 0.1%
		
		double upper=endTime;
		double lower=startTime;
		double trial=(upper+lower)/2;
		
		//System.out.println("Find Slot of "+ bit +" in Interval "+ startTime+ " to "+ endTime);
		
		while(notFound){
			
			run=true;			
			
			upper=endTime;
			lower=startTime;
			trial=(upper+lower)/2;
			
			double rand=Math.random()*joulesInInterval;
			
			while(run){
				
				double integral=playground.wrashid.sschieffer.V1G.Main.functionIntegrator.integrate(func, startTime, trial);
				
				if(integral<rand){
					lower=trial;					
					trial=(upper+lower)/2;
					
				}else{
					upper=trial;
					trial=(upper+lower)/2;
				}
				
				if(Math.abs(integral-rand)<=err){
					run=false;
				}
			}
			
			ChargingInterval c1;
			if(trial+bit>endTime){
				c1= new ChargingInterval(endTime-bit, endTime );
			}else{
				c1= new ChargingInterval(trial, trial+bit);
			}
			//System.out.println("Assigned Slot of "+ c1.getStartTime() + " to "+ c1.getEndTime());
			
			if(chargingInParkingInterval.overlapWithTimeInterval(c1)==false){
				//no overlap--> exit loop
				
				notFound=false;
				chargingInParkingInterval.addTimeInterval(c1);
				//System.out.println("assign next ");
			}else{
				System.out.println("overlap agent .. redo");
				
			}
			//otherwise notFOund remains true and it runs again
			
		}
		
		return chargingInParkingInterval;
		
	}
	
	
}
