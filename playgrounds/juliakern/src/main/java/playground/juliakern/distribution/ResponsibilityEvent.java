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

package playground.juliakern.distribution;

import java.util.Map;

import org.matsim.api.core.v01.Id;

/**
 * 
 * @author julia
 *
 */
public interface ResponsibilityEvent {
	
	public final static String EVENT_TYPE = "responsibilityEvent";
	public final static String RESPONSIBLE_PERSON_ID = "responsiblePersonId";
	public final static String RECEIVING_PERSON_ID = "receivingPersonId";
	public final static String EMISSIONEVENT_STARTTIME = "emissionEventStartTime";
	public final static String EXPOSURE_STARTTIME = "exposureStartTime";
	public final static String EXPOSURE_ENDTIME = "exposureEndTime";
	public final static String LOCATION = "location";
	public final static String CONCENTRATION = "concentration";

	public Double getExposureValue();

	public Id getResponsiblePersonId();

	public String getResponsibilityInformation();
	
	public String getExposureInformation();

	public Id getReceivingPersonId();

	public Double getDuration();

	public Map<String, String> getInformationMap();

}