/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterA.java
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
package playground.yu.visum.filter;

import org.matsim.api.core.v01.events.Event;

/**
 * @author ychen
 */
public abstract class EventFilterA extends Filter implements EventFilterI {

	/*
	 * -------------------------MEMBER VARIABLES----------------
	 */
	private EventFilterI nextFilter = null;

	/*
	 * ------------------------SETTER------------------------------
	 */

	/**
	 * sets the next EventFilterA-Object
	 * 
	 * @param nextFilter
	 *            - The nextFilter to set.
	 */
	@Override
	public void setNextFilter(EventFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}

	/*
	 * ------------------------IMPLEMENTS METHODS-----------------------
	 */
	@Override
	public abstract boolean judge(Event event);

	@Override
	public void handleEvent(Event event) {
		if (judge(event)) {
			count();
			nextFilter.handleEvent(event);
		}
	}

	/**
	 * @return Returns the result.
	 */
	protected boolean isResult() {
		return false; // subclass may overwrite this to return true
	}
}
