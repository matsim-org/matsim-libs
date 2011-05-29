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
package playground.wrashid.sschieffer.DSC;


import java.util.ArrayList;

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
	 * @throws Exception 
	 */
	public Schedule distribute(Id agentId, Schedule schedule) throws Exception{
		Schedule chargingScheduleAllIntervalsAgent = new Schedule();
		
		for (int i=0; i<schedule.getNumberOfEntries(); i++){
			
			TimeInterval t= schedule.timesInSchedule.get(i);
			if(t.isParking() ){
				ParkingInterval p= (ParkingInterval) t;
				if(p.getRequiredChargingDuration()>0.0){//standard charging case
					
					double chargingTime=p.getRequiredChargingDuration();
					if(chargingTime>0){
						
					}
					ArrayList <LoadDistributionInterval> loadList= DecentralizedSmartCharger.myHubLoadReader.getDeterministicLoadDistributionIntervalsAtLinkAndTime(
							agentId, //agentId
							p.getLocation(),
							p);
					
					// in this case loadList can only be size=1
					if(loadList.size()>1){
						System.out.println("check distribute.. loadList should not be possible to be larger than 1");
					}
						PolynomialFunction func= loadList.get(0).getPolynomialFunction();
						
						if(p.getIntervalLength()*0.65 <chargingTime){
							if(chargingTime>p.getIntervalLength()){
								if(DecentralizedSmartCharger.debug){
									System.out.println("rounding error - correction");
								}
								
								chargingTime=p.getIntervalLength();
								p.setRequiredChargingDuration(p.getIntervalLength());
							}
							double diff= p.getIntervalLength()-chargingTime;
							double startRand= Math.random()*diff;
							
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
						
					}else{// if charging durtion<0 meaning it is discharging
						p.setChargingSchedule(null);
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
			double chargingTime) throws Exception{
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
			
			// if too many iterations - reduce mincharging length or attribute anything
			
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
	 * @throws Exception 
	 */
	public Schedule assignRandomChargingSlotInChargingInterval(PolynomialFunction func, 
			double startTime, 
			double endTime, 
			double joulesInInterval, 
			double bit, 
			Schedule chargingInParkingInterval) throws Exception{
		// random and test
		
		boolean notFound=true;
		boolean run=true;
		if(DecentralizedSmartCharger.debug){
			System.out.println("assign random charging slot in interval");
			System.out.println(bit+ "seconds between "+ startTime + " - "+ endTime);
			System.out.println("Schedule already:");
			chargingInParkingInterval.printSchedule();
		}
		
		double upper=endTime;
		double lower=startTime;
		double trial=(upper+lower)/2;
		
		int countNotFoundInARow=0;
		
		while(notFound){
			
			run=true;			
			
			upper=endTime;
			lower=startTime;
			trial=(upper+lower)/2;
			
			double rand=Math.random();//double rand=Math.random()*joulesInInterval;
			
			double integral;
			PolynomialFunction funcSubOpt=null;
			double fullSubOptIntegral=0;
			while(run){
				
				
				if(joulesInInterval>=0){
					double err=Math.max(joulesInInterval/100.0, 1.0); // accuracy 1%
					//double err=joulesInInterval/100.0; // accuracy 1%
					if (DecentralizedSmartCharger.debug){
						System.out.println("integrate stat:"+ startTime+ " upto "+ trial+ " Function"+ func.toString());
						if(startTime==trial){
							System.out.println("TROUBLEt:");
							System.out.println("error:"+ err+ " joules in interval"+ joulesInInterval+ " Function"+ func.toString());
						}
					}
					integral=DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(func, startTime, trial);
					
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
					
					// negative suboptimal interval will only be calculated first time when func is stil null
					if (funcSubOpt==null){
						funcSubOpt= turnSubOptimalSlotDistributionIntoProbDensityOfFindingAvailableSlot(func, startTime, endTime);
						
					}
					
					integral=DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(
							funcSubOpt, 
							startTime, 
							trial);
					// limit integration only to first run through when value still 0
					if (fullSubOptIntegral==0){
						fullSubOptIntegral= DecentralizedSmartCharger.functionSimpsonIntegrator.integrate(
								funcSubOpt, startTime, endTime);
					}
					
					double err=Math.max(Math.abs(fullSubOptIntegral)/100.0, 1.0); // accuracy 0.1%
					
					if(Math.abs(integral)<Math.abs(rand*fullSubOptIntegral)){
						lower=trial;					
						trial=(upper+lower)/2;
						
					}else{
						upper=trial;
						trial=(upper+lower)/2;
					}
					//System.out.println(Math.abs(Math.abs(integral)-Math.abs(rand*fullSubOptIntegral)) +"<="+ err);
					if(Math.abs(Math.abs(integral)-Math.abs(rand*fullSubOptIntegral))<=err){
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
			
			if(c1!=null && chargingInParkingInterval.overlapWithTimeInterval(c1)==false){
				//no overlap--> exit loop
				
				notFound=false;
				countNotFoundInARow=0;
				chargingInParkingInterval.addTimeInterval(c1);
				
			}else{
				countNotFoundInARow++;
				
				if(countNotFoundInARow>100){
					chargingInParkingInterval=exitDistributionIfTooConstrained(startTime, endTime, bit, chargingInParkingInterval);
					notFound=false;
				}
			}
		}
		chargingInParkingInterval.sort();
		return chargingInParkingInterval;
		
	}
	
	
	
	public Schedule exitDistributionIfTooConstrained(double startTime, 
			double endTime, 
			double bit,
			Schedule chargingInParkingInterval){
		
		double timeAlready=chargingInParkingInterval.getTotalTimeOfIntervalsInSchedule();
		
		double diff= (endTime-startTime)-(timeAlready+bit);
		double startRand= Math.random()*diff;
		
		ChargingInterval c= new ChargingInterval(startTime+startRand, startTime+startRand+(timeAlready+bit));
		chargingInParkingInterval=new Schedule();
		chargingInParkingInterval.addTimeInterval(c);
		
		return chargingInParkingInterval;
		
	}
	
	
	public PolynomialFunction turnSubOptimalSlotDistributionIntoProbDensityOfFindingAvailableSlot(PolynomialFunction func, double start1, double end1) throws Exception{
		
		//-1/f(x)
		/*func= func.negate();
		PolynomialFunction modFunc= new PolynomialFunction(new double[]{1});
		*/
		DomainFinder d= new DomainFinder(start1, end1, func);
		
		double minDomain= d.getDomainMin();
		int start= (int)Math.floor(start1);
		int end= (int)Math.ceil(end1);
		int steps= (int)Math.floor((( end-start)/(60.0*5.0)));
		
		double [][] newFunc= new double[steps][2];
		
		for(int i=0; i<steps; i++){
			newFunc[i][0]=start+(i*(60.0*5.0));
			newFunc[i][1]=func.value(start+(i*(60.0*5.0)))-minDomain;
			
		}
				
		return DecentralizedSmartCharger.fitCurve(newFunc);
		
		
	}
	
	
}
