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

package org.matsim.core.events.parallelEventsHandler;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.Gbl;

/**
 * The wrapper around the Events class for allowing parallelization.
 *
 * @author rashid_waraich
 */
public class ProcessEventThread implements Runnable {
	ArrayList<Event> preInputBuffer = null;
	ConcurrentListSPSC<Event> eventQueue = null;
	EventsManagerImpl events;
	CyclicBarrier cb = null;
	private int preInputBufferMaxLength;

	public ProcessEventThread(EventsManagerImpl events, int preInputBufferMaxLength,
			CyclicBarrier cb) {
		this.events = events;
		this.preInputBufferMaxLength = preInputBufferMaxLength;
		eventQueue = new ConcurrentListSPSC<Event>();
		preInputBuffer = new ArrayList<Event>();
		this.cb = cb;
	}

	public synchronized void processEvent(Event event) {
		// first approach (quick on office computer, but not on satawal)
		// eventQueue.add(event);

		// second approach, lesser locking => faster on Satawal
		preInputBuffer.add(event);
		if (preInputBuffer.size() > preInputBufferMaxLength) {
			eventQueue.add(preInputBuffer);
			preInputBuffer.clear();
		}
	}

	public void run() {
		// process events, until LastEventOfIteration arrives
		Event nextEvent = null;
		while (true) {
			nextEvent = eventQueue.remove();
			if (nextEvent != null) {
				if (nextEvent instanceof LastEventOfIteration) {
					break;
				}
				events.processEvent(nextEvent);
			}
		}
		// inform main thread, that processing finished
		try {
			cb.await();
			Gbl.printCurrentThreadCpuTime();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}
	}

	// schedule LastEventOfIteration and flush buffered events
	// the LastEventOfIteration lets the event handler threads know,
	// that there is no more work, as soon as they have processed this,
	// they are allowed to go to sleep
	public void close() {
		processEvent(new LastEventOfIteration(0.0));
		eventQueue.add(preInputBuffer);
		preInputBuffer.clear();
	}

}
