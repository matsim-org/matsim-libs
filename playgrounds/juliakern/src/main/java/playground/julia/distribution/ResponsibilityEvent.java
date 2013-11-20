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

public interface ResponsibilityEvent {
	
	public final static String EVENT_TYPE = "responsibilityEvent";
	
	public final static String ATTRIBUTE_RESPONSIBLE_PERSON_ID = "personId";

	public Double getExposureValue();

	public Id getResponsiblePersonId();

	public String getInformation();

	public Id getReceivingPersonId();

	public Double getDuration();

}