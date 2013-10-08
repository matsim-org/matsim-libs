/* *********************************************************************** *
 * project: org.matsim.*
 * ParkingSearchEvent.java
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

package playground.christoph.parking.core.events;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

/**
 * @author cdobler
 */
public class ParkingSearchEvent extends Event {

	public static final String EVENT_TYPE = "parkingsearch";
	public static final String ATTRIBUTE_SEARCHSTRATEGYTYPE = "searchstrategy";
	
	private final String searchStrategy;
	
	public ParkingSearchEvent(final double time, final Id personId, final String searchStrategy) {
		super(time);
		this.personId = personId;
		this.searchStrategy = searchStrategy;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();

		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put(ATTRIBUTE_SEARCHSTRATEGYTYPE, this.searchStrategy);

		return attr;
	}

	public String getSearchStrategyType() {
		return this.searchStrategy;
	}
	
	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;


	public Id getPersonId() {
		return this.personId;
	}

}
