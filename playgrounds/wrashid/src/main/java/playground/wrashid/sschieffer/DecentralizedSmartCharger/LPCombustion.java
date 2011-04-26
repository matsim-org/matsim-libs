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

package playground.wrashid.sschieffer.DecentralizedSmartCharger;

import lpsolve.LpSolveException;

/**
 * this class is called if the agent has a conventional vehicle
 * 
 * An actual LP is not required, since the agent can only use the combustion engine.
 * So his required charging times are set to 0.
 * 
 * @author Stella
 *
 */
public class LPCombustion {

	
	private double energyFromCombustionEngine;
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
		energyFromCombustionEngine= getDrivingConsumption();
		return schedule;
	}
	
	
	
	public double getEnergyFromCombustionEngine(){
		return energyFromCombustionEngine;
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