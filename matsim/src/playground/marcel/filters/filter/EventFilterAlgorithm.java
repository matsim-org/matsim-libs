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

package playground.marcel.filters.filter;

import org.matsim.events.BasicEvent;
import org.matsim.events.handler.BasicEventHandlerI;

/**
 * @author  ychen
 */
public class EventFilterAlgorithm implements BasicEventHandlerI, EventFilterI {
	private EventFilterI nextFilter = null;

	private int count = 0;

	/*
	 * ----------------------IMPLEMENTS METHODS----------------------
	 * (non-Javadoc)
	 *
	 * @see org.matsim.playground.filters.EventFilterI#setNextFilter(org.matsim.playground.filters.EventFilterI)
	 */
	public void setNextFilter(EventFilterI nextFilter) {
		this.nextFilter = nextFilter;
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.filters.filter.FilterI#count()
	 */
	public void count() {
		this.count++;
	}
	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.filters.filter.FilterI#getCount()
	 */
	public int getCount() {
		return this.count;
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.filters.filter.EventFilterI#judge(org.matsim.demandmodeling.events.BasicEvent)
	 */
	public boolean judge(BasicEvent event) {
		return true;
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.filters.filter.EventFilterI#handleEvent(org.matsim.demandmodeling.events.BasicEvent)
	 */
	public void handleEvent(BasicEvent event) {
		count();
		this.nextFilter.handleEvent(event);
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.events.handler.EventHandlerI#reset(int)
	 */
	public void reset(int iteration) {
	}
}
