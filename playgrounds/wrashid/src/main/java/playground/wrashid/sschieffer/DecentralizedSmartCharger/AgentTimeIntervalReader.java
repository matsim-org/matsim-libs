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
package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import java.util.LinkedList;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.commons.math.analysis.polynomials.PolynomialFunction;
import org.matsim.api.core.v01.Id;

import playground.wrashid.PSF2.pluggable.energyConsumption.EnergyConsumptionPlugin;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingIntervalInfo;
import playground.wrashid.PSF2.pluggable.parkingTimes.ParkingTimesPlugin;


/**
 * reads and sorts schedules of agents
 * calls Linear Programming
 * @author Stella
 *
 */
public class AgentTimeIntervalReader {

	
	public ParkingTimesPlugin parkingTimesPlugin;
	public EnergyConsumptionPlugin energyConsumptionPlugin;
	
	
	AgentTimeIntervalReader(
			ParkingTimesPlugin parkingTimesPlugin,
			EnergyConsumptionPlugin energyConsumptionPlugin){
		
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
		
		
		schedule=addDrivingTimes(id, schedule);			
		System.out.println("with driving times");
		//schedule.printSchedule();
		
		
		System.out.println("controlling optimal vs nonOptimal charging times");
		schedule = checkTimesWithHubSubAndOptimalTimes(schedule);
		//schedule.printSchedule();
		
		System.out.println("calculating Joules per Interval");
		schedule = getJoulesForEachParkingInterval(id, schedule);
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
		
		//System.out.println("Schedule unsorted");
		//schedule.printSchedule();
		schedule.sort();
		//System.out.println("Schedule sorted");
		//schedule.printSchedule();
		
		
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
	
	
	public Schedule checkIntervalForSubAndOptimalIntervals(TimeInterval t){
		Schedule newSchedule= new Schedule();
		if(t.isDriving()){
			newSchedule.addTimeInterval(t);
			return newSchedule;
			
		}else{
			// t is Parking
			ParkingInterval thisParkingInterval= (ParkingInterval)t;
			
			Id idLink= thisParkingInterval.getLocation();
			Schedule loadDistributionSchedule= DecentralizedSmartCharger.myHubLoadReader.getLoadDistributionScheduleForHubId(idLink);
			
			double startParking= t.getStartTime();
			double endParking= t.getEndTime();
			
			int intervalStart=loadDistributionSchedule.timeIsInWhichInterval(startParking);
			int intervalEnd=loadDistributionSchedule.timeIsInWhichInterval(endParking);
			
			LoadDistributionInterval lstart= (LoadDistributionInterval)loadDistributionSchedule.timesInSchedule.get(intervalStart);
			LoadDistributionInterval lend= (LoadDistributionInterval)loadDistributionSchedule.timesInSchedule.get(intervalEnd);
			
			// if start and end are in same loadDistributionInterval
			if(intervalStart==intervalEnd){
				thisParkingInterval.setParkingOptimalBoolean(lstart.isOptimal());
				newSchedule.addTimeInterval(t);
				return newSchedule;
			}else{
				// if start and end are NOT in same loadDistributionInterval
				
				ParkingInterval p1= 
					new ParkingInterval(startParking, 
							loadDistributionSchedule.timesInSchedule.get(intervalStart).getEndTime(),
							idLink
							);
				p1.setParkingOptimalBoolean(lstart.isOptimal());
				newSchedule.addTimeInterval(p1);
				
				
				for(int j=intervalStart+1; j<intervalEnd; j++){
					LoadDistributionInterval lInBetween= (LoadDistributionInterval)loadDistributionSchedule.timesInSchedule.get(j);
					
					ParkingInterval p3= 
						new ParkingInterval(
								lInBetween.getStartTime(),//start
								lInBetween.getEndTime(),// end
								idLink
								);
					p3.setParkingOptimalBoolean(lInBetween.isOptimal());
					newSchedule.addTimeInterval(p3);
				}
				
									
				ParkingInterval p2= 
					new ParkingInterval(loadDistributionSchedule.timesInSchedule.get(intervalEnd).getStartTime(),
							endParking, 
							idLink
							);
				p2.setParkingOptimalBoolean(lend.isOptimal());
				newSchedule.addTimeInterval(p2);
				return newSchedule;
				
			}
			
		}
	}
	
	
	public Schedule checkTimesWithHubSubAndOptimalTimes( Schedule schedule){
		Schedule newSchedule= new Schedule();	
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			
			TimeInterval t= schedule.timesInSchedule.get(i);
			Schedule checkedTimeIntervalSchedule= checkIntervalForSubAndOptimalIntervals(t);
			
			for(int j=0; j<checkedTimeIntervalSchedule.getNumberOfEntries();j++){
				newSchedule.addTimeInterval(checkedTimeIntervalSchedule.timesInSchedule.get(j));
			}
		}
		
		return newSchedule;
	}
	
	
	
	public Schedule getJoulesForEachParkingInterval(Id id, Schedule schedule) throws MaxIterationsExceededException, FunctionEvaluationException, IllegalArgumentException{
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			
			if(schedule.timesInSchedule.get(i).isParking()){
				ParkingInterval thisParkingInterval= (ParkingInterval)schedule.timesInSchedule.get(i);
				// getFunctionFromHubReader
				Id idLink= thisParkingInterval.getLocation();
				
				PolynomialFunction p= 
					DecentralizedSmartCharger.myHubLoadReader.getDeterministicLoadPolynomialFunctionAtLinkAndTime(
							idLink, 
							thisParkingInterval);
				
							
				//Integrate from start to End
				double joulesInInterval=DecentralizedSmartCharger.functionIntegrator.integrate(p, 
						thisParkingInterval.getStartTime(), 
						thisParkingInterval.getEndTime());
				
				schedule.addJoulesToTotalSchedule(joulesInInterval);
				//save results in Parking Interval
				thisParkingInterval.setJoulesInPotentialChargingInterval(joulesInInterval);
				
			}
			
		}
		
		return schedule;
	}
	
	
	
	
}
