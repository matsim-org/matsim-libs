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

import org.matsim.basic.v01.Id;
import org.matsim.population.Person;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This event specifies that an agent has gained some utility (or disutility).
 * Scoring functions should handle these Events and add the utilities specified
 * by such events to the agents' score.
 *
 * @author mrieser
 */
public final class AgentUtilityEvent extends BasicEvent {

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
	public Attributes getAttributes() {
		AttributesImpl attr = new AttributesImpl();
		attr.addAttribute("", "", "time", "", Double.toString(this.time));
		attr.addAttribute("", "", "agent", "", this.agentId);
		attr.addAttribute("", "", "type", "", "agentUtility");
		attr.addAttribute("", "", "amount", "", Double.toString(this.amount));
		return attr;
	}

	@Override
	public String toString() {
		return getTimeString(this.time) + this.agentId + "\t0\t\t0\t9\tagentUtility\t" + amount;
	}

}
