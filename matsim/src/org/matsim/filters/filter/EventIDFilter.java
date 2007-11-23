/* *********************************************************************** *
 * project: org.matsim.*
 * EventIDFilter.java
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

package org.matsim.filters.filter;

import org.matsim.events.BasicEvent;

public class EventIDFilter extends EventFilterA {
	private static final int criterion = 38;

	@Override
	public boolean judge(BasicEvent event) {
		return (Integer.parseInt(event.agentId) % criterion == 0);
	}
}