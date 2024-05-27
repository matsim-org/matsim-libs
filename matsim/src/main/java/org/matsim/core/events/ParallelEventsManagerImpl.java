/* *********************************************************************** *
 * project: org.matsim.*
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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;

/**
 *
 * ParallelEvents allows parallelization for events handling. Usage: First
 * create an object of this class. Before each iteration, call initProcessing.
 * After each iteration, call finishProcessing. This has already been
 * incorporated into the Controller.
 *
 * Usage via config.xml:
 *
 * <pre>
 * <module name="parallelEventHandling">
 *  <param name="numberOfThreads" value="2" />
 * </module>
 * </pre>
 *
 * optionally you can also specify the estimated number of events per iteration:
 *
 * <pre>
 *  <param name="estimatedNumberOfEvents" value="10000000" />
 * </pre>
 *
 * (not really needed, but can make performance slightly faster in larger
 * simulations).
 *
 * @see <a href="http://www.matsim.org/node/238">http://www.matsim.org/node/238</a>
 * @author rashid_waraich
 *
 */
public final class ParallelEventsManagerImpl implements EventsManager {

	private boolean parallelMode = true;
	private int numberOfThreads;
	private EventsManagerImpl[] events = null;
	private ProcessEventThread[] eventsProcessThread = null;
	private Thread[] threads = null;
	private int numberOfAddedEventsHandler = 0;
	private final AtomicReference<Throwable> hadException = new AtomicReference<>();
	private final ExceptionHandler uncaughtExceptionHandler = new ExceptionHandler(hadException);

	private final static Logger log = LogManager.getLogger(ParallelEventsManagerImpl.class);

	// this number should be set in the following way:
	// if the number of events is estimated as x, then this number
	// could be set to x/10
	// the higher this parameter, the less locks are used, but
	// the more the time buffer between the simulation and events handling
	// for small simulations, the default value is ok and it even works
	// quite well for larger simulations with 10 million events
	private int preInputBufferMaxLength = 100000;

	@Inject
	ParallelEventsManagerImpl(Config config) {
		if (config.eventsManager().getEstimatedNumberOfEvents() != null) {
			preInputBufferMaxLength = (int) (config.eventsManager().getEstimatedNumberOfEvents() / 10);
		}
		init(config.eventsManager().getNumberOfThreads());
	}

	/**
	 * @param numberOfThreads
	 *            - specify the number of threads used for the events handler
	 */
	public ParallelEventsManagerImpl(int numberOfThreads) {
		init(numberOfThreads);
	}

	/**
	 *
	 * @param numberOfThreads
	 * @param estimatedNumberOfEvents
	 *            Only use this constructor for larger simulations (20M+
	 *            events).
	 */
	public ParallelEventsManagerImpl(int numberOfThreads, long estimatedNumberOfEvents) {
		preInputBufferMaxLength = (int) (estimatedNumberOfEvents / 10 );
		init(numberOfThreads);
	}

	@Override
	public void processEvent(final Event event) {
		if (parallelMode) {
			for (int i = 0; i < eventsProcessThread.length; i++) {
				eventsProcessThread[i].processEvent(event);
			}
		} else {
			for (int i = 0; i < eventsProcessThread.length; i++) {
				eventsProcessThread[i].getEvents().processEvent(event);
			}
		}
	}

	@Override
	public void addHandler(final EventHandler handler) {
		synchronized (this) {
			log.info("adding Event-Handler " + handler.getClass().getName() + " to thread " + numberOfAddedEventsHandler);
			events[numberOfAddedEventsHandler].addHandler(handler);
			numberOfAddedEventsHandler = (numberOfAddedEventsHandler + 1) % numberOfThreads;
		}
	}

	@Override
	public void resetHandlers(final int iteration) {
		synchronized (this) {
			for (int i = 0; i < events.length; i++) {
				events[i].resetHandlers(iteration);
			}
		}
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		synchronized (this) {
			for (int i = 0; i < events.length; i++) {
				events[i].removeHandler(handler);
			}
		}
	}

	private void printEventHandlers() {
		synchronized (this) {
			for (int i = 0; i < events.length; i++) {
				log.info("registered event handlers for thread " + i + ":");
				events[i].printEventHandlers();
			}
		}
	}

	private void init(int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		this.events = new EventsManagerImpl[numberOfThreads];
		this.eventsProcessThread = new ProcessEventThread[numberOfThreads];
		this.threads = new Thread[numberOfThreads];
		// the additional 1 is for the simulation barrier
		for (int i = 0; i < numberOfThreads; i++) {
			events[i] = new EventsManagerImpl();
		}
	}

	// When one simulation iteration is finish, it must call this method,
	// so that it can communicate to the threads, that the simulation is
	// finished and that it can await the event handler threads.

	// after call to this method, all event processing is done not in parallel
	// anymore
	@Override
	public void finishProcessing() {
		for (int i = 0; i < eventsProcessThread.length; i++) {
			eventsProcessThread[i].close();
		}

		try {
			for (Thread t : this.threads) {
				t.join();
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// list which threads had which handlers to debug performance issues
		printEventHandlers();

		/*
		 * introduction of the parallel mode variable was required, because of
		 * the following reason: previously no event handling was possible after
		 * the end of the simulation. e.g. adding money events in the after
		 * mobsim controler listener would not be invoked by parallelEventHandling
		 */

		parallelMode = false;

		if (this.hadException.get() != null) {
			throw new RuntimeException(
					"Exception while processing events. Cannot guarantee that all events have been fully processed.",
					uncaughtExceptionHandler.hadException.get());
		}
	}

	// create event handler threads
	// prepare for next iteration
	@Override
	public void initProcessing() {
		// reset this class, so that it can be reused for the next iteration
		for (int i = 0; i < numberOfThreads; i++) {
			this.eventsProcessThread[i] = new ProcessEventThread(events[i], preInputBufferMaxLength);
			this.threads[i] = new Thread(eventsProcessThread[i], "Events-" + i);
			this.threads[i].setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
			this.threads[i].start();
		}

		// (re-)activate parallel mode while the mobsim is running
		this.parallelMode = true;
	}

	/**
	 * @author mrieser
	 */
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicReference<Throwable> hadException;

		public ExceptionHandler(final AtomicReference<Throwable> hadException) {
			this.hadException = hadException;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			log.error("Thread " + t.getName() + " died with exception while handling events.", e);
			this.hadException.set(e);
		}

	}

	@Override
	public void afterSimStep(double time) {
		// nothing to do in this implementation
	}

}
