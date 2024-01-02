/* *********************************************************************** *
 * project: org.matsim.*
 * EventHandler.java
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

package org.matsim.core.events.handler;

import org.matsim.core.api.internal.MatsimExtensionPoint;

/**
 * Examples:<ul>
 * <li>{@link tutorial.programming.example06EventsHandling.RunEventsHandlingExample}
 * <li>{@link tutorial.programming.example06EventsHandling.RunEventsHandlingWithControlerExample}
 * <li>{@link tutorial.programming.example21tutorialTUBclass.events.RunEventsHandlingExample}
 * </ul>
 *
 * <br>
 * Design thoughts:<ul>
 * <li> This is deliberately without a handleEvent( Event ev ) so that derived interfaces and ultimately classes are not
 * forced to implement a handler that deals with <i>all</i> events.  kai, with input from dominik, nov'11
 * </ul>
 *
 */
public interface EventHandler extends MatsimExtensionPoint {
	/** Gives the event handler the possibility to clean up its internal state.
	 * Within a Controler-Simulation, this is called before the mobsim starts.
	 *
	 * @param iteration the up-coming iteration from which up-coming events will be from.
	 */
	default void reset(int iteration) {}

}
