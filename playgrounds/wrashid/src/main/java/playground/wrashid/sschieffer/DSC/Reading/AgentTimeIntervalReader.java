/* *********************************************************************** *
 * project: org.matsim.*
 * AgentChargingTimeReader.java
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
package playground.wrashid.sschieffer.DSC.Reading;

import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;
import playground.wrashid.sschieffer.DSC.DecentralizedSmartCharger;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.DrivingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.LoadDistributionInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.ParkingInterval;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.Schedule;
import playground.wrashid.sschieffer.SetUp.IntervalScheduleClasses.TimeInterval;


/**
 * reads and sorts schedules of agents
 * calls Linear Programming
 * 
 * @author Stella
 *
 */
public class AgentTimeIntervalReader {

	
	public ParkingTimesPlugin parkingTimesPlugin;
	public EnergyConsumptionPlugin energyConsumptionPlugin;
	
	
	public AgentTimeIntervalReader(
			ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin
			){
	
		this.parkingTimesPlugin=parkingTimesPlugin;
		this.energyConsumptionPlugin=energyConsumptionPlugin;
		
	}
	
	/**
	 * 
	 * @param id
	 * @return
	 * @throws IllegalArgumentException 
	 * @throws FunctionEvaluationException 
	 * @throws MaxIterationsExceededException 
	 */
	public Schedule readParkingAndDrivingTimes(Id id) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		Schedule schedule=new Schedule();
		
		schedule=readParkingTimes(id, schedule);	
		//schedule.printSchedule();
		schedule=addDrivingTimes(id, schedule);			
		//schedule.printSchedule();
		//System.out.println("controlling optimal vs nonOptimal charging times");
		schedule = checkTimesWithHubSubAndOptimalTimes(schedule, id);
		
		//System.out.println("calculating Joules per Interval");
		schedule.getJoulesForEachParkingInterval(id);
		//schedule.printSchedule();
		return schedule;
	}
	
	
	public Schedule readParkingTimes(Id id, Schedule schedule){
		LinkedList<ParkingIntervalInfo> parkingTimeIntervals = parkingTimesPlugin.getParkingTimeIntervals().get(id);
		
		for (int i=0;i<parkingTimeIntervals.size();i++) {
			double start=parkingTimeIntervals.get(i).getArrivalTime();
			double end=parkingTimeIntervals.get(i).getDepartureTime();
			if (end>start){
				schedule.addTimeInterval(new ParkingInterval(
						start,//double
						end,//double
						parkingTimeIntervals.get(i).getLinkId()//id
						));
			} else{
				schedule.addTimeInterval(new ParkingInterval(
						0,//double
						end,//double
						parkingTimeIntervals.get(i).getLinkId()//id
						));
				
				schedule.addTimeInterval(new ParkingInterval(
						start,//double
						DecentralizedSmartCharger.SECONDSPERDAY,
						
						parkingTimeIntervals.get(i).getLinkId()//id
						));
			}			
		}
			
		schedule.sort();
		
		return schedule;
	}
	
	
	public Schedule addDrivingTimes(Id id, Schedule schedule){
		LinkedList<Double> energyConsumptionOfLegs=
						energyConsumptionPlugin.getEnergyConsumptionOfLegs().get(id);
		Schedule driving= new Schedule();
		int count=0;
		
		for(int i=1; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).getStartTime()-schedule.timesInSchedule.get(i-1).getEndTime()>0){
				driving.addTimeInterval(new DrivingInterval(
						schedule.timesInSchedule.get(i-1).getEndTime(),//double
						schedule.timesInSchedule.get(i).getStartTime(),//double
						energyConsumptionOfLegs.get(count)//consumption
					)
				);
			}
			
			count++;
		}
		
		schedule.mergeSchedules(driving);
		
		return schedule;
	}
	
	
	public Schedule checkIntervalForSubAndOptimalIntervals(TimeInterval t, Id id){
		
		Schedule newSchedule= new Schedule();
		
		if(t.isDriving()){
			newSchedule.addTimeInterval(t);
			return newSchedule;
			
		}else{
			// t is Parking
			ParkingInterval thisParkingInterval= (ParkingInterval)t;
			
			Id idLink= thisParkingInterval.getLocation();
			
			Schedule loadDistributionSchedule;
		
			loadDistributionSchedule= DecentralizedSmartCharger.myHubLoadReader.getLoadDistributionScheduleForHubId(id, idLink);
		
			
			for(int i=0; i<loadDistributionSchedule.getNumberOfEntries(); i++){
				
				LoadDistributionInterval lHub= ((LoadDistributionInterval)loadDistributionSchedule.timesInSchedule.get(i));
				LoadDistributionInterval overlap= 
					t.ifOverlapWithLoadDistributionIntervalReturnOverlap(lHub);
				
				if(null!=overlap){
					ParkingInterval p= new ParkingInterval(overlap.getStartTime(),
							overlap.getEndTime(), 
							((ParkingInterval)t).getLocation());
					p.setParkingOptimalBoolean(lHub.isOptimal());
					newSchedule.addTimeInterval(p);
					
				}
			}
			return newSchedule;
			
		}
	}
	
	
	
	public Schedule checkTimesWithHubSubAndOptimalTimes( Schedule schedule, Id id){
		Schedule newSchedule= new Schedule();	
		
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			
			TimeInterval t= schedule.timesInSchedule.get(i);
			//System.out.println("current interval");
			//t.printInterval();
			Schedule checkedTimeIntervalSchedule= checkIntervalForSubAndOptimalIntervals(t, id);
			//System.out.println("current intervalafter check");
			//checkedTimeIntervalSchedule.printSchedule();
			
			for(int j=0; j<checkedTimeIntervalSchedule.getNumberOfEntries();j++){
				newSchedule.addTimeInterval(checkedTimeIntervalSchedule.timesInSchedule.get(j));
			}
		}
		
		return newSchedule;
	}
	
	
	
	
	
	
	
}
