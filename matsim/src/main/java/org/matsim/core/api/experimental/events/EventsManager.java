/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.events;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.EventArray;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Steppable;

/**
 * This should be split into two interfaces:
 * an API (add/removeHandler) and an SPI (Service Provider Interface)
 */
public interface EventsManager {

	public void processEvent(final Event event);

	/**
	 * Submit multiple events for processing at once.
	 */
	default void processEvents(final EventArray events) {
		for (int i = 0; i < events.size(); i++) {
			processEvent(events.get(i));
		}
	}

	public void addHandler(final EventHandler handler);
	
	public void removeHandler(final EventHandler handler);

	public void resetHandlers(int iteration);
	
	/**
	 * Called before the first event is sent for processing. Allows to initialize internal
	 * data structures used to process events.
	 */
	public void initProcessing();

	/**
	 * Called by a {@link Steppable} Mobsim after each {@link doSimStep} call. Parallel implementations
	 * of an EventsManager can then ensure that all events of the sim step have been processed.
	 */
	public void afterSimStep(double time);
	
	/**
	 * Called after the last event is sent for processing. The method must only return when all
	 * events are completely processing (in case they are not directly processed in
	 * {@link #processEvent(Event)}). Can be used to clean up internal data structures used
	 * to process events.
	 */
	public void finishProcessing();

}