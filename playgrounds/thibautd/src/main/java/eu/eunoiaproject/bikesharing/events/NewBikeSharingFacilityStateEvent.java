/* *********************************************************************** *
 * project: org.matsim.*
 * NewBikeSharingFacilityStateEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package eu.eunoiaproject.bikesharing.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

/**
 * @author thibautd
 */
public class NewBikeSharingFacilityStateEvent extends Event {
	private final Id facilityId;
	private final int newAmountOfBikes;

	public NewBikeSharingFacilityStateEvent(
			final double time,
			final Id facilityId,
			final int newAmountOfBikes) {
		super( time );
		this.facilityId = facilityId;
		this.newAmountOfBikes = newAmountOfBikes;
	}

	@Override
	public Map<String, String> getAttributes() {
		final Map<String, String> atts = super.getAttributes();
		atts.put( "facilityId" , facilityId.toString() );
		atts.put( "newAmountOfBikes" , newAmountOfBikes+"" );
		return atts;
	}

	@Override
	public String getEventType() {
		return "newBikeSharingFacilityState";
	}
}

