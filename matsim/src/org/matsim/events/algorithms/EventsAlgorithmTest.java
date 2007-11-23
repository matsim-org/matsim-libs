/* *********************************************************************** *
 * project: org.matsim.*
 * EventsAlgorithmTest.java
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

package org.matsim.events.algorithms;

import org.matsim.events.BasicEvent;
import org.matsim.events.handler.BasicEventHandlerI;

/**
 * A very simple algorithm. all it does is output some messages about handled events.
 * 
 */
public class EventsAlgorithmTest implements BasicEventHandlerI {
	public void handleEvent(BasicEvent event) {
		System.out.println("EventsAlgorithmTest.handeEvent() called for event " + event.getClass() + " data : " + event);
	}

	/**
	 * 
	 */
	public EventsAlgorithmTest() {
		super();
		System.out.println("EventsAlgorithmTest instanciated.");
	}
	public void reset(int iteration) {
		System.out.println("EventsAlgorithmTest.reset() called for iteration " + iteration);
	}

}
