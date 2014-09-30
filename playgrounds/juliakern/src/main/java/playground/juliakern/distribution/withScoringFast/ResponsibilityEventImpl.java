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

package playground.juliakern.distribution.withScoringFast;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

import playground.juliakern.distribution.ResponsibilityEvent;

public class ResponsibilityEventImpl extends Event implements ResponsibilityEvent {

	private Id responsiblePersonId;
	private Id receivingPersonId;
	private Double exposureStartTime;
	private Double exposureEndTime;
	private Double concentration;
	private String exposureLocation;
	
	@Override
	public Double getExposureValue() {
		return this.getDuration()*this.concentration;
	}

	public ResponsibilityEventImpl(Id responsiblePersonId, Id receivingPersonId, Double exposureStartTime, Double exposureEndTime,
			Double concentration, String exposureLocation) {
		super(exposureStartTime);
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
	public String getResponsibilityInformation() {
		
		String info = "Exposure for person " + receivingPersonId.toString(); 
		info += " exposure time: " + exposureStartTime + " - " + exposureEndTime;
		info += " with exposure value " + this.getExposureValue();
		info += " at/during " + exposureLocation + ". ";
		return info;
	}
	
	public String getExposureInformation(){
		String info = "Responsible person " + responsiblePersonId.toString();
		info += " exposure time: " + exposureStartTime + " - " + exposureEndTime;
		info += " with exposure value " + this.getExposureValue();
		info += " at/during " + exposureLocation + ". ";
		return info;
	}

	@Override
	public Map<String, String> getInformationMap() {
		Map<String, String> info = new HashMap<String, String>();
		info.put(RECEIVING_PERSON_ID, receivingPersonId.toString());
		info.put(RESPONSIBLE_PERSON_ID, responsiblePersonId.toString());
		info.put(EXPOSURE_STARTTIME, exposureStartTime.toString());
		info.put(EXPOSURE_ENDTIME, exposureEndTime.toString());
		info.put(LOCATION, exposureLocation);
		info.put(CONCENTRATION, concentration.toString());

		return info;
	}

}
