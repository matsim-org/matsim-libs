/* *********************************************************************** *
 * project: org.matsim.*
 * SynchronizedEventsManagerImpl
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.events;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;


/**
 * This class can be used by parallel mobility simulations. 
 * If you are not programming parallel code that generates events better use EventsManagerImpl.
 * 
 * The method processEvent is synchronized in this
 * implementation to avoid thread interference errors.
 * This class is just a delegate that uses all the logic that can be found in another EventsManager instance
 * except the synchronization of processEvent().
 * 
 * @author dgrether
 *
 */
class SynchronizedEventsManagerImpl implements EventsManager {

	private final EventsManager delegate;

	public SynchronizedEventsManagerImpl(EventsManager eventsManager){
		this.delegate = eventsManager;
	}

	@Override
	public void addHandler(EventHandler handler) {
		this.delegate.addHandler(handler);
	}

	@Override
	public synchronized void processEvent(Event event) {
		this.delegate.processEvent(event);
	}
	
	@Override
	public void removeHandler(EventHandler handler) {
		this.delegate.removeHandler(handler);
	}

	@Override
	public void resetHandlers(int iteration) {
		delegate.resetHandlers(iteration);
	}

	@Override
	public void initProcessing() {
		delegate.initProcessing();
	}

	@Override
	public void afterSimStep(double time) {
		delegate.afterSimStep(time);
	}

	@Override
	public void finishProcessing() {
		delegate.finishProcessing();
	}

}
