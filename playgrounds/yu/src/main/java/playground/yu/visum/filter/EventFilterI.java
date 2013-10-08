/* *********************************************************************** *
 * project: org.matsim.*
 * EventFilterI.java
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
 * This interface extends interface:
 * org.matsim.playground.filters.filter.FilterI, and offers some important
 * functions for org.matsim.playground.filters.filter.EventFilter.
 * 
 * @author ychen
 * 
 */
public interface EventFilterI extends FilterI {
	/**
	 * judges whether the Event (org.matsim.events.Event) will be selected or
	 * not
	 * 
	 * @param event
	 *            - which is being judged
	 * @return true if the Person meets the criterion of the EventFilterA
	 */
	boolean judge(Event event);

	/**
	 * sends the person to the next EventFilterA
	 * (org.matsim.playground.filters.filter.EventFilter) or other behavior
	 * 
	 * @param event
	 *            - an event being handled
	 */
	void handleEvent(Event event);

	/**
	 * sets the next Filter, who will handle Event-object.
	 * 
	 * @param nextFilter
	 *            - the next Filter, who will handle Event-object.
	 */
	void setNextFilter(EventFilterI nextFilter);
}
