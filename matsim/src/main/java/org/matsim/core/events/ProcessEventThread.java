/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import java.lang.InterruptedException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.Gbl;

/**
 * The wrapper around the Events class for allowing parallelization.
 *
 * @author rashid_waraich
 */
/*package*/ class ProcessEventThread implements Runnable {
	private final List<Event> preInputBuffer;
	private final BlockingQueue<Event> eventQueue;
	private final EventsManager events;
	private final int preInputBufferMaxLength;

	public ProcessEventThread(
			final EventsManager events,
			final int preInputBufferMaxLength) {
		this.events = events;
		this.preInputBufferMaxLength = preInputBufferMaxLength;
		eventQueue = new LinkedBlockingQueue<Event>();
		preInputBuffer = new ArrayList<Event>( preInputBufferMaxLength + 1);
	}

	public synchronized void processEvent(final Event event) {
		// first approach (quick on office computer, but not on satawal)
		// eventQueue.add(event);

		// second approach, lesser locking => faster on Satawal
		preInputBuffer.add(event);
		if (preInputBuffer.size() > preInputBufferMaxLength) {
			emptyPreBuffer();
		}
	}

	private void emptyPreBuffer() {
		eventQueue.addAll( preInputBuffer );
		preInputBuffer.clear();
	}

	@Override
	public void run() {
		try {
			// process events, until LastEventOfIteration arrives
			while (true) {
				// take waits for an element to exist before returning:
				//  - thread sleeps until there is an event to process
				//  - we do not have to bother checking if the element exists
				Event nextEvent = eventQueue.take();
				if (nextEvent instanceof LastEventOfIteration) {
					Gbl.printCurrentThreadCpuTime();
					
					// if there are more events generated after end of simulation 
					// (generated in events handler), process them before stopping events handling.
					// in order to do this, LastEventOfIteration is moved to the back of the queue.
					if (eventQueue.size()>0){
						processEvent(nextEvent);
						emptyPreBuffer();
						nextEvent = eventQueue.take();
					} else {
						return;
					}
				}
				getEvents().processEvent(nextEvent);
			}
		}
		catch ( InterruptedException e ) {
			throw new RuntimeException( e );
		}
	}

	// schedule LastEventOfIteration and flush buffered events
	// the LastEventOfIteration lets the event handler threads know,
	// that there is no more work, as soon as they have processed this,
	// they are allowed to go to sleep
	public synchronized void close() {
		processEvent(new LastEventOfIteration(0.0));
		emptyPreBuffer();
	} 

	public EventsManager getEvents() {
		return events;
	}

}
