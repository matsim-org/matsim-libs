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

package playground.wrashid.parallelEventsHandler;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import org.matsim.events.BasicEvent;
import org.matsim.events.Events;
import org.matsim.events.handler.EventHandler;

public class ParallelEvents extends Events {

	private int numberOfThreads;
	private Events[] events = null;
	private ProcessEventThread[] eventsProcessThread = null;
	private int numberOfAddedEventsHandler = 0;
	private CyclicBarrier barrier = null;
	// this number should be set in the following way:
	// if the number of events is estimated as x, than this number
	// could be set to x/10
	// the higher this parameter, the less locks are used, but
	// the more the time gab between the simulation and events handling
	// for small simulations, the default value is ok and it even works
	// quite well for larger simulations with 10 million events
	private int preInputBufferMaxLength = 100000;

	
	/**
	 * @param numberOfThreads -
	 *            specify the number of threads used for the events handler
	 */
	public ParallelEvents(int numberOfThreads) {
		init(numberOfThreads);
	}

	/**
	 * 
	 * @param numberOfThreads
	 * @param expectedNumberOfEvents
	 * Only use this constructor for larger simulations (20M+ events).
	 */
	public ParallelEvents(int numberOfThreads, int expectedNumberOfEvents) {
		preInputBufferMaxLength = expectedNumberOfEvents / 10;
		init(numberOfThreads);
	}
	
	public void processEvent(final BasicEvent event) {
		for (int i = 0; i < eventsProcessThread.length; i++) {
			eventsProcessThread[i].processEvent(event);
		}
	}

	
	public void addHandler(final EventHandler handler) {
		synchronized (this) {
			events[numberOfAddedEventsHandler].addHandler(handler);
			numberOfAddedEventsHandler = (numberOfAddedEventsHandler + 1)
					% numberOfThreads;
		}
	}
	
	
	public void resetHandlers(final int iteration) {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].resetHandlers(iteration);
			}
		}
	}
	
	
	public void resetCounter() {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].resetCounter();
			}
		}
	}
	
	
	public void removeHandler(final EventHandler handler) {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].removeHandler(handler);
			}
		}
	}
	
	
	public void clearHandlers() {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].clearHandlers();
			}
		}
	}
	
	
	public void printEventHandlers() {
		synchronized (this) {
			for (int i=0;i<events.length;i++){
				events[i].printEventHandlers();
			}
		}
	}

	private void init(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		this.events = new Events[numberOfThreads];
		this.eventsProcessThread = new ProcessEventThread[numberOfThreads];
		// the additional 1 is for the simulation barrier
		barrier = new CyclicBarrier(numberOfThreads + 1);
		for (int i = 0; i < numberOfThreads; i++) {
			events[i] = new Events();
			eventsProcessThread[i] = new ProcessEventThread(events[i],
					preInputBufferMaxLength, barrier);
		}
	}

	// When one simulation iteration is finish, it must call this method,
	// so that it can communicate to the threads, that the simulation is 
	// finished and so that it can await the event handler threads
	public void awaitHandlerThreads() {
		for (int i = 0; i < eventsProcessThread.length; i++) {
			eventsProcessThread[i].close();
		}

		try {
			barrier.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (BrokenBarrierException e) {
			e.printStackTrace();
		}

		// reset this class, so that it can be reused for the next iteration
		for (int i = 0; i < numberOfThreads; i++) {
			eventsProcessThread[i] = new ProcessEventThread(events[i],
					preInputBufferMaxLength, barrier);
		}
	}
}
