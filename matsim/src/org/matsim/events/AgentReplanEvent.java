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

import org.matsim.population.routes.CarRoute;

/**
 * @author dgrether
 */
public class AgentReplanEvent extends PersonEvent {

	public static final String EVENT_TYPE = "replan";

	public CarRoute replannedRoute;

	public AgentReplanEvent(final double time, final String agentId, final CarRoute alternativeRoute) {
		super(time, agentId);
		this.replannedRoute = alternativeRoute;
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
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
