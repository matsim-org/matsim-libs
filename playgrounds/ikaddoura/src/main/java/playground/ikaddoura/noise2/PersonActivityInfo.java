/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.noise2;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Time;

/**
 * @author ikaddoura
 *
 */
public class PersonActivityInfo {
	
	private Id<Person> personId; // not required in new implementation TODO	
	private String activityType;
	private double startTime;
	private double endTime;
	private double durationWithinInterval; // not required in new implementation TODO	
	
	public Id<Person> getPersonId() {
		return personId;
	}
	public void setPersonId(Id<Person> personId) {
		this.personId = personId;
	}
	public String getActivityType() {
		return activityType;
	}
	public void setActivityType(String activityType) {
		this.activityType = activityType;
	}
	public double getStartTime() {
		return startTime;
	}
	public void setStartTime(double startTime) {
		this.startTime = startTime;
	}
	public double getEndTime() {
		return endTime;
	}
	public void setEndTime(double endTime) {
		this.endTime = endTime;
	}	
	public double getDurationWithinInterval() {
		return durationWithinInterval;
	}
	public void setDurationWithinInterval(double durationWithinInterval) {
		this.durationWithinInterval = durationWithinInterval;
	}
	
	@Override
	public String toString() {
		return "PersonId: " + personId + " / activityType: " + activityType + " / startTime: " + Time.writeTime(startTime, Time.TIMEFORMAT_HHMMSS) + " / endTime: " + Time.writeTime(endTime, Time.TIMEFORMAT_HHMMSS);
	}
	
	public double getDurationWithinInterval(double timeIntervalEnd, double timeBinSize) {
		
		double durationInThisInterval = 0.;
		double timeIntervalStart = timeIntervalEnd - timeBinSize;
		
		if (( this.getStartTime() < timeIntervalEnd ) && ( this.getEndTime() >=  timeIntervalStart )) {
			if ((this.getStartTime() <= timeIntervalStart) && this.getEndTime() >= timeIntervalEnd ) {
				durationInThisInterval = timeBinSize;
			
			} else if (this.getStartTime() <= timeIntervalStart && this.getEndTime() <= timeIntervalEnd) {
				durationInThisInterval = this.getEndTime() - timeIntervalStart;
			
			} else if (this.getStartTime() >= timeIntervalStart && this.getEndTime() >= timeIntervalEnd) {
				durationInThisInterval = timeIntervalEnd - this.getStartTime();
			
			} else if (this.getStartTime() >= timeIntervalStart && this.getEndTime() <= timeIntervalEnd) {
				durationInThisInterval = this.getEndTime() - this.getStartTime();
		
			} else {
				throw new RuntimeException("Unknown case. Aborting...");
			}
		}
		
		return durationInThisInterval;
	}
}
