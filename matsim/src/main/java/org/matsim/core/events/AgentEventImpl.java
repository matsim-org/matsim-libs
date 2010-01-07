/* *********************************************************************** *
 * project: org.matsim.*
 * AgentEvent.java
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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.AgentEvent;

public abstract class AgentEventImpl extends PersonEventImpl implements AgentEvent {

	public static final String ATTRIBUTE_LINK = "link";	
	public static final String ATTRIBUTE_LEGMODE = "legMode";	

	private Leg leg;

	private final Id linkId;
	private final TransportMode legMode;

	AgentEventImpl(final double time, final Id agentId, final Id linkId, final Leg leg) {
		super(time, agentId);
		this.linkId = linkId;
		this.leg = leg;
		this.legMode = leg == null ? null : leg.getMode();
	}

	AgentEventImpl(final double time, final Id agentId, final Id linkId, final TransportMode legMode) {
		super(time, agentId);
		this.linkId = linkId;
		this.legMode = legMode;
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> attr = super.getAttributes();
		attr.put(ATTRIBUTE_LINK, this.linkId.toString());
		attr.put(ATTRIBUTE_LEGMODE, (this.legMode == null ? null : this.legMode.toString()));
		return attr;
	}

	public Leg getLeg() {
		return this.leg;
	}

	public TransportMode getLegMode() {
		return this.legMode;
	}
	
	public Id getLinkId() {
		return this.linkId;
	}
	
}
