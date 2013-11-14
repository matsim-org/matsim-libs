/* *********************************************************************** *
 * project: org.matsim.*
 * ColdEmissionEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.julia.distribution;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

public class ExposureEventImpl extends Event implements ExposureEvent {
	
	// ! exposure value = time x concentration
	Id personId; 
	Double startTime; 
	Double endTime;
	Double personalExposureValue; 
	String activitytype;
	

	public ExposureEventImpl(Id personId, double startTime, double endTime,
			Double personalExposureValue, String activitytype) {
		super(startTime);
		this.personId = personId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.personalExposureValue = personalExposureValue;
		this.activitytype = activitytype;
	}

	public Id getPersonId() {
		return personId;
	}

	public Double getAverageExposure() {
		return personalExposureValue/(endTime-startTime);
	}

	public Double getExposure() {
		return personalExposureValue;
	}

	public Double getDuration() {
		return endTime-startTime;
	}

	@Override
	public String getEventType() {
		return ExposureEvent.EVENT_TYPE;
	}

}
