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

package org.matsim.core.events;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.NetworkRoute;

/**
 * @author dgrether
 */
public class AgentReplanEvent extends PersonEvent {

	public static final String EVENT_TYPE = "replan";

	private final NetworkRoute replannedRoute;

	public AgentReplanEvent(final double time, final Id agentId, final NetworkRoute alternativeRoute) {
		super(time, agentId);
		this.replannedRoute = alternativeRoute;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

	public String getTextRepresentation() {
		StringBuilder builder = new StringBuilder();
		builder.append(Double.valueOf(this.getTime()));
		builder.append(' ');
		builder.append(this.getPersonId().toString());

		return builder.toString();
	}

	public NetworkRoute getReplannedRoute() {
		return this.replannedRoute;
	}

}
