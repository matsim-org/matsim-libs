/* *********************************************************************** *
 * project: org.matsim.*
 * AgentEventWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.analysis.possiblesharedrides;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.AgentEvent;
import org.matsim.core.api.experimental.events.LinkEvent;

/**
 * wrapper to allow manipulating AgentEvent as LinkEvent.
 * AgentEvent is "compatible with LinkEvent as it implements the
 * getLinkId() method, but it does not implements it, leading to
 * the need of all kind of ugly tests.
 * @author thibautd
 */
public class AgentEventWrapper implements AgentEvent, LinkEvent {
	private AgentEvent delegate;

	public AgentEventWrapper(final AgentEvent ev) {
		this.delegate = ev;
	}

	/**
	 * @see org.matsim.core.api.experimental.events.PersonEvent#getPersonId()
	 */
	public Id getPersonId() {
		return delegate.getPersonId();
	}

	/**
	 * @see org.matsim.core.api.experimental.events.AgentEvent#getLinkId()
	 */
	public Id getLinkId() {
		return delegate.getLinkId();
	}

	/**
	 * @see org.matsim.core.api.experimental.events.AgentEvent#getLegMode()
	 */
	public String getLegMode() {
		return delegate.getLegMode();
	}

	/**
	 * @see org.matsim.core.api.experimental.events.Event#getTime()
	 */
	public double getTime() {
		return delegate.getTime();
	}

	/**
	 * @see org.matsim.core.api.experimental.events.Event#getAttributes()
	 */
	public Map<String, String> getAttributes() {
		return delegate.getAttributes();
	}
}
