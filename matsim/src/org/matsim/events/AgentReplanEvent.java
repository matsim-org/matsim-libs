/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.population.Route;

/**
 * @author dgrether
 */
public class AgentReplanEvent extends PersonEvent {

	public Route replannedRoute;

	public AgentReplanEvent(double time, String agentId, Route alternativeRoute) {
		super(time, agentId);
		this.replannedRoute = alternativeRoute;
	}

	/**
	 * @see org.matsim.events.BasicEvent#getAttributes()
	 */
	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(BasicEvent.ATTRIBUTE_TYPE, "replan");
		return attr;
	}

	/**
	 * @see org.matsim.events.BasicEvent#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Double.valueOf(this.time));
		builder.append(' ');
		builder.append(this.agentId);

		return builder.toString();
	}

}
