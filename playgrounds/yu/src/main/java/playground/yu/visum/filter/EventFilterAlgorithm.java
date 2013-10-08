/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterAlgorithm.java
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
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * @author ychen
 */
public class EventFilterAlgorithm implements BasicEventHandler, EventFilterI {
	private EventFilterI nextFilter = null;

	private int count = 0;

	/*----------------------IMPLEMENTS METHODS--------------------*/
	@Override
	public void setNextFilter(EventFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}

	@Override
	public void count() {
		count++;
	}

	@Override
	public int getCount() {
		return count;
	}

	@Override
	public boolean judge(Event event) {
		return true;
	}

	@Override
	public void handleEvent(Event event) {
		count();
		nextFilter.handleEvent(event);
	}

	@Override
	public void reset(int iteration) {
	}
}
