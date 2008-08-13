/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.population.Route;
import org.xml.sax.Attributes;


/**
 * @author dgrether
 *
 */
public class AgentReplanEvent extends BasicEvent {

	public Route replannedRoute;

	public AgentReplanEvent(double time, String agentId, Route alternativeRoute) {
		super(time, agentId);
		this.replannedRoute = alternativeRoute;
	}

	/**
	 * @see org.matsim.events.BasicEvent#getAttributes()
	 */
	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}

	/**
	 * @see org.matsim.events.BasicEvent#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(Double.valueOf(this.time));
		builder.append(" ".intern());
		builder.append(this.agentId);

		return builder.toString();
	}

}
