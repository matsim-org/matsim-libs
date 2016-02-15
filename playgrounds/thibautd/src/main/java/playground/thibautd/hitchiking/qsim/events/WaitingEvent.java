/* *********************************************************************** *
 * project: org.matsim.*
 * PassengerStartsWaitingEvent.java
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
package playground.thibautd.hitchiking.qsim.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;

import java.util.Map;

/**
 * @author thibautd
 */
public abstract class WaitingEvent extends Event {
	private final Id link;

	public static final String ATTRIBUTE_PERSON = "person";

	private final Id personId;

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_PERSON, this.personId.toString());
		attr.put( "link"  , ""+link );
		return attr;
	}

	public Id getPersonId() {
		return this.personId;
	}
	
	protected WaitingEvent(
			final double time,
			final Id agentId,
			final Id linkId) {
		super(time);
		this.personId = agentId;
		link = linkId;
	}

	public Id getLinkId() {
		return link;
	}

}

