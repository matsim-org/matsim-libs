/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.api.experimental.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;


/**
 * @author nagel
 *
 */
public class BoardingDeniedEvent extends Event 
{
	public static final String EVENT_TYPE="BoardingDeniedEvent" ;
	
	public static final String ATTRIBUTE_PERSON_ID = "person" ;
	private Id personId ;

	public static final String ATTRIBUTE_VEHICLE_ID = "vehicle" ;
	private Id vehicleId;
	
	public BoardingDeniedEvent(final double time, Id personId2, Id vehicleId2 ) {
		super(time) ;
		this.personId = personId2 ;
		this.vehicleId = vehicleId2 ;
	}
	
	@Override
	public Map<String,String> getAttributes() {
		Map<String,String> atts = super.getAttributes() ;
		atts.put(ATTRIBUTE_PERSON_ID, this.personId.toString() ) ;
		atts.put(ATTRIBUTE_VEHICLE_ID, this.vehicleId.toString() ) ;
		return atts ;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE ;
	}

	public Id getPersonId() {
		return personId;
	}

	public Id getVehicleId() {
		return vehicleId;
	}


}
