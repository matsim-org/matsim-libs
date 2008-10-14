/* *********************************************************************** *
 * project: org.matsim.*
 * AgentUtilityEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.events;

import java.util.Map;

import org.matsim.basic.v01.Id;
import org.matsim.population.Person;

/**
 * This event specifies that an agent has gained some utility (or disutility).
 * Scoring functions should handle these Events and add the utilities specified
 * by such events to the agents' score.
 *
 * @author mrieser
 */
public final class AgentUtilityEvent extends PersonEvent {

	public final double amount;

	public AgentUtilityEvent(final double time, final Person agent, final double amount) {
		super(time, agent);
		this.amount = amount;
	}

	public AgentUtilityEvent(final double time, final Id agentId, final double amount) {
		super(time, agentId.toString());
		this.amount = amount;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(BasicEvent.ATTRIBUTE_TYPE, "agentUtility");
		attr.put("amount", Double.toString(this.amount));
		return attr;
	}

	@Override
	public String toString() {
		return getTimeString(this.time) + this.agentId + "\t0\t\t0\t9\tagentUtility\t" + amount;
	}

}
