/* *********************************************************************** *
 * project: org.matsim.*
 * AgentDepartureEvent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;

public class AgentDepartureEvent extends AgentEvent implements BasicAgentDepartureEvent {

	public static final String EVENT_TYPE = "departure";

	public AgentDepartureEvent(final double time, final PersonImpl agent, final LinkImpl link, final LegImpl leg) {
		super(time, agent, link, leg);
	}

	public AgentDepartureEvent(final double time, final Id agentId, final Id linkId) {
		super(time, agentId, linkId);
	}

	@Override
	public String getEventType() {
		return EVENT_TYPE;
	}

}
