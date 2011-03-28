/* *********************************************************************** *
 * project: org.matsim.*
 * LPCombustion.java
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

import lpsolve.LpSolveException;


public class LPCombustion {

	private Schedule schedule;
	LPCombustion(){
		
	}
	
	
	public Schedule updateSchedule(Schedule schedule) throws LpSolveException{
		this.schedule=schedule;
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			if(schedule.timesInSchedule.get(i).isParking()){
				((ParkingInterval)schedule.timesInSchedule.get(i)).setRequiredChargingDuration(0);
				
			}
		
		}
		
		return schedule;
	}
	
	
	
	public double getDrivingConsumption(){
		double consumption=0;
		
		for(int i=0; i<schedule.getNumberOfEntries(); i++){
			schedule.printSchedule();
			if(schedule.timesInSchedule.get(i).isDriving()){
				DrivingInterval d1= ((DrivingInterval)schedule.timesInSchedule.get(i));
				consumption+=d1.getConsumption();
			}
		
		}
		return consumption;
	}
	
	
	
}