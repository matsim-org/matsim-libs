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

	private Id responsiblePersonId;
	private Id receivingPersonId;
	private Double emissionEventTime;
	private Double exposureStartTime;
	private Double exposureEndTime;
	private Double concentration;
	private String exposureLocation;
	
	@Override
	public Double getExposureValue() {
		return this.getDuration()*this.concentration;
	}

	public ResponsibilityEventImpl(Id responsiblePersonId, Id receivingPersonId, Double emissionEventTime, Double exposureStartTime, Double exposureEndTime,
			Double concentration, String exposureLocation) {
		super(exposureStartTime);
		this.emissionEventTime = emissionEventTime;
		this.responsiblePersonId = responsiblePersonId;
		this.receivingPersonId = receivingPersonId;
		this.exposureStartTime = exposureStartTime;
		this.exposureEndTime = exposureEndTime;
		this.concentration = concentration;
		this.exposureLocation = exposureLocation;
	}

	public Double getDuration() {
		return(this.exposureEndTime-this.exposureStartTime);
	}

	@Override
	public Id getResponsiblePersonId() {
		return responsiblePersonId;
	}

	public Id getReceivingPersonId(){
		return receivingPersonId;
	}
	
	@Override
	public String getEventType() {
		return ResponsibilityEvent.EVENT_TYPE;
	}

	@Override
	public String getInformation() {
		
		String info = "Exposure for Person " + responsiblePersonId.toString(); 
		info += " exposure time: " + exposureStartTime + " - " + exposureEndTime;
		info += " with exposure value " + this.getExposureValue();
		info += " at/during " + exposureLocation + ". ";
		info += "Caused by emission event at " + emissionEventTime + ".";
		return info;
	}

}
