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

package playground.christoph.events.parallelEventsHandler;

import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.parallelEventsHandler.LastEventOfIteration;
import org.matsim.core.events.parallelEventsHandler.LastEventOfSimStep;
import org.matsim.core.gbl.Gbl;

/* 
 * Alternative implementation. Now use a LinkedBlockingQueue instead
 * of busy waiting. 
 *
 * @author cdobler
 */
public class WithinDayProcessEventThread implements Runnable {
	private ArrayList<Event> preInputBuffer = null;
	private LinkedBlockingQueue<Event> eventQueue = null;
	private EventsManager events;
	private CyclicBarrier simStepBarrier = null;
	private CyclicBarrier iterationBarrier = null;
	private int preInputBufferMaxLength;

	public WithinDayProcessEventThread(EventsManager events, int preInputBufferMaxLength, CyclicBarrier simStepBarrier, CyclicBarrier iterationBarrier)
	{
		this.events = events;
		this.preInputBufferMaxLength = preInputBufferMaxLength;
		eventQueue = new LinkedBlockingQueue<Event>();
		preInputBuffer = new ArrayList<Event>();
		this.simStepBarrier = simStepBarrier;
		this.iterationBarrier = iterationBarrier;
	}

	public synchronized void processEvent(Event event) {
		// first approach (quick on office computer, but not on satawal)
		// eventQueue.add(event);

		// second approach, lesser locking => faster on Satawal
		preInputBuffer.add(event);
		if (preInputBuffer.size() > preInputBufferMaxLength)
		{
			eventQueue.addAll(preInputBuffer);
			preInputBuffer.clear();
		}
	}

	public void run()
	{
		try
		{
			// process events, until LastEventOfIteration arrives
			Event nextEvent = null;
			while (true) {
				nextEvent = eventQueue.take();
				if (nextEvent != null)
				{
					if (nextEvent instanceof LastEventOfIteration)
					{
						break;
					}
					else if (nextEvent instanceof LastEventOfSimStep)
					{
						this.simStepBarrier.await();
					}

					events.processEvent(nextEvent);
				}

			}
			// inform main thread, that processing finished
			iterationBarrier.await();
			Gbl.printCurrentThreadCpuTime();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		} 
		catch (BrokenBarrierException e)
		{
			e.printStackTrace();
		}
		
	}

	// schedule LastEventOfIteration and flush buffered events
	// the LastEventOfIteration lets the event handler threads know,
	// that there is no more work, as soon as they have processed this,
	// they are allowed to go to sleep
	public void closeIteration()
	{
		processEvent(new LastEventOfIteration(0.0));
		eventQueue.addAll(preInputBuffer);
		preInputBuffer.clear();
	}

	public void closeSimStep()
	{
		processEvent(new LastEventOfSimStep(0.0));
		eventQueue.addAll(preInputBuffer);
		preInputBuffer.clear();
	}
}
