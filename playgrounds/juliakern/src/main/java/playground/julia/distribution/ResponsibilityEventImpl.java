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

public class ResponsibilityEventImpl extends Event implements ResponsibilityEvent {

	private Id personId;
	private Double startTime;
	private Double endTime;
	private Double concentration;
	private String location;
	
	@Override
	public Double getExposureValue() {
		return this.getDuration()*this.concentration;
	}

	public ResponsibilityEventImpl(Id personId, Double startTime, Double endTime,
			Double concentration, String location) {
		super(startTime);
		this.personId = personId;
		this.startTime = startTime;
		this.endTime = endTime;
		this.concentration = concentration;
		this.location = location;
	}

	public Double getDuration() {
		return(this.endTime-this.startTime);
	}

	@Override
	public Id getPersonId() {
		return personId;
	}

	@Override
	public String getEventType() {
		return ResponsibilityEvent.EVENT_TYPE;
	}

	@Override
	public String getInformation() {
		
		String info = "Start time = " + startTime.toString();
		info+= ", end time = " + endTime.toString();
		info+= ", exposure value = " + this.getExposureValue();
		info+= ", location = " + location;
		return info;
	}

}
