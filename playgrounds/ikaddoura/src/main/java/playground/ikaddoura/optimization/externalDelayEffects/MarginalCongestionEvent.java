/* *********************************************************************** *
 * project: org.matsim.*
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

/**
 * 
 */
package playground.ikaddoura.optimization.externalDelayEffects;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.internal.HasPersonId;

/**
 * Event to indicate that an agent entering or leaving a link is delaying other agents on that link later on.
 * 
 * @author ikaddoura
 */
public final class MarginalCongestionEvent extends Event implements HasPersonId {
	
	public static final String EVENT_TYPE = "MarginalCongestionEffect";
	public static final String ATTRIBUTE_PERSON = "person";
	public static final String ATTRIBUTE_DELAY = "delay";
	
	private final Id personId;
	private final double delay;

	public MarginalCongestionEvent(double time, Id personId, double externalDelay) {
		super(time);
		this.personId = personId;
		this.delay = externalDelay;
	}

	@Override
	public Id getPersonId() {
		return this.personId;
	}
	
	public double getDelay() {
		return delay;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}
	
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attrs = super.getAttributes();
		attrs.put(ATTRIBUTE_PERSON, this.personId.toString());
		attrs.put(ATTRIBUTE_DELAY, Double.toString(this.delay));
		return attrs;
	}

}
