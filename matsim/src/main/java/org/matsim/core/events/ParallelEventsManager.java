/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelEventsManager.java
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

package org.matsim.core.events;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.hermes.Hermes;
import org.matsim.core.mobsim.hermes.WorldDumper;
import javax.inject.Inject;

/**
 * @author cdobler
 */
public final class ParallelEventsManager implements EventsManager {

	private final static Logger log = Logger.getLogger(ParallelEventsManager.class);
	
	private Distributor distributor;
	private EventsManager[] eventsManagers;
	private final List<EventHandler> eventsHandlers;
	private final boolean oneThreadPerHandler;
	private final boolean syncOnTimeSteps;
	private final int numOfThreads;
	private final AtomicBoolean hadException;
	private final ExceptionHandler uncaughtExceptionHandler;
	private int iteration = 0;

	private final int eventsQueueSize = 1048576 * 32;
	private final int eventsArraySize;

	@Inject
	ParallelEventsManager(Config config) {
		this(config.parallelEventHandling().getSynchronizeOnSimSteps() != null ? config.parallelEventHandling().getSynchronizeOnSimSteps() : true);
	}

	public ParallelEventsManager(final boolean syncOnTimeSteps) {
		this(syncOnTimeSteps, true, -1);
	}
	
	public ParallelEventsManager(final boolean syncOnTimeSteps, final int numOfThreads) {
		this(syncOnTimeSteps, false, numOfThreads);
	}
	
	/*package*/ ParallelEventsManager(final boolean syncOnTimeSteps, final boolean oneThreadPerHandler, final int numOfThreads) {
		this.syncOnTimeSteps = syncOnTimeSteps;
		this.oneThreadPerHandler = oneThreadPerHandler;
		this.numOfThreads = numOfThreads;
		this.hadException = new AtomicBoolean(false);
		this.eventsHandlers = new ArrayList<EventHandler>();
		this.eventsArraySize = syncOnTimeSteps ? 512 : 32768;
		this.uncaughtExceptionHandler = new ExceptionHandler(this.hadException);		
	}
	
	private void initialize() {	
		int numHandlers = oneThreadPerHandler ? this.eventsHandlers.size() : Math.min(this.numOfThreads, this.eventsHandlers.size());
		this.distributor = new Distributor(new ProcessEventsRunnable[numHandlers]);
		this.eventsManagers = new EventsManager[numHandlers];
		
		// create event managers
		if (this.oneThreadPerHandler) {
			for (int i = 0; i < this.eventsHandlers.size(); i++) {
				this.eventsManagers[i] = new SingleHandlerEventsManager(this.eventsHandlers.get(i));
			}
		} else {
			// TODO - check if this slow path is correct
			for (int i = 0; i < this.numOfThreads; i++) {
				this.eventsManagers[i] = new EventsManagerImpl();
			}
			for (int i = 0; i < this.eventsHandlers.size(); i++) {
				this.eventsManagers[this.eventsHandlers.size() % numOfThreads].addHandler(this.eventsHandlers.get(i));
			}
		}	

		// initialize runnables (threads that will execute the event managers)
		for (int i = 0; i < this.eventsManagers.length; i++) {
			EventsManager eventsManager = this.eventsManagers[i];
			ProcessEventsRunnable processEventsRunnable = new ProcessEventsRunnable(eventsManager);
			distributor.runnables[i] = processEventsRunnable;
			processEventsRunnable.setDaemon(true);
			processEventsRunnable.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
			if (oneThreadPerHandler) {
				processEventsRunnable.setName("SingleHandlerEventsManager: " + ((SingleHandlerEventsManager) eventsManager).getEventHandlerClassName());
			}
			else {
				processEventsRunnable.setName(ProcessEventsRunnable.class.toString() + i);
			}
			processEventsRunnable.start();
		}
		
		// initialize the distributor
		this.distributor.setDaemon(true);
		this.distributor.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
		this.distributor.setName("EventsDistributor");
		this.distributor.start();
	}
	
	private void teardown() {
		try {
			for (ProcessEventsRunnable per : distributor.runnables) {
				per.interrupt();
				per.join();
			}
			distributor.interrupt();
			distributor.join();	
		} catch (InterruptedException e) {
			throw new RuntimeException("Exception while waiting on join..." + e.getMessage());
		}
		
	}
	
	@Override
	public void processEvent(final Event event) {
		EventArray array = new EventArray(1);
		array.add(event);
		this.distributor.processEvents(array);
	}

	public void processEvents(final EventArray events) {
		this.distributor.processEvents(events);		
	}

	@Override
	public void addHandler(final EventHandler handler) {
		// this will be used the next time we start an iteration
		this.eventsHandlers.add(handler);
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		// this will be used the next time we start an iteration
		this.eventsHandlers.remove(handler);
	}
	
	@Override
	public void resetHandlers(int iteration) {
		for (EventsManager eventsManager : this.eventsManagers) {
			eventsManager.resetHandlers(iteration);
		}
	}

	@Override
	public void initProcessing() {

		initialize();

		for (EventsManager eventsManager : this.eventsManagers) {
			eventsManager.initProcessing();
		}

		resetHandlers(iteration);
	}
		
	/*
	 * In some chases Events are created after this method has been called. To ensure that they are processed in 
	 * real time, we process them not in the parallel thread. To do so, we replace the parallel events manager
	 * with its EventsManager instance.
	 */
	@Override
	public synchronized void finishProcessing() {

		processEvent(new LastEventOfIteration(Double.MAX_VALUE));
		flush();
		
		for (EventsManager eventsManager : this.eventsManagers) {
			eventsManager.finishProcessing();
		}

		teardown();

		if (this.hadException.get()) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.");
		}

		iteration += 1;
	}

	@Override
	public void afterSimStep(double time) {
		
		if (this.syncOnTimeSteps) {
			processEvent(new LastEventOfSimStep(time));
			flush();	
		}

		if (this.hadException.get()) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.");
		}

	}
	
	private void flush() {
		try {
			this.distributor.flush();
		} catch (InterruptedException e) {
			throw new RuntimeException("Exception while waiting on flush... " + e.getMessage());
		}
	}
	
	private class Distributor extends Thread {

		private final ProcessEventsRunnable[] runnables;
		private final BlockingQueue<EventArray> inputQueue;

		// When set to true, the distributor will process all events until all events in the event manager are processed.
		// This is used when the simulation needs to sync with event processing and make sure there are no unprocessed
		// events in the system.
		private boolean shouldFlush = false;
		
		public Distributor(ProcessEventsRunnable[] runnables) {
			this.runnables = runnables;
			this.inputQueue = new ArrayBlockingQueue<>(eventsQueueSize);
		}

		public final void processEvents(EventArray events) {
			this.inputQueue.add(events);
		}
	
		// TODO - do we need to support stopping?
		public void flush() throws InterruptedException {
			
			synchronized (this) {
				shouldFlush = true;
				this.wait();	
			}

		}
		
		private void distribute(EventArray events) {
			for (ProcessEventsRunnable runnable : this.runnables) {
				runnable.eventsQueue.add(events);
			}
		}
		
		@SuppressWarnings("unused")
		@Override
		public final void run() {
			try {
				EventArray events = new EventArray(eventsArraySize);
				while (true) {
					EventArray earray = this.inputQueue.poll(100, TimeUnit.MILLISECONDS);
					
					// this is just a debug hook
					if (Hermes.DEBUG_EVENTS && earray != null) {
						for (int i = 0; i < earray.size(); i++) {
							Event event = earray.get(i);
							if (event != null && event.getEventType() != null && !event.getEventType().equals("simstepend")) {
								WorldDumper.dumpEvent(event);
							}
						}
					}
					
					if (earray == null) {
						synchronized (this) {
							// check if we can finish the flush
							if (shouldFlush) {
								// flush all runnables
								for (ProcessEventsRunnable runnable : this.runnables) {
									runnable.flush();
								}
								// termination criteria for the flush
								if (inputQueue.isEmpty()) {
									shouldFlush = false;
									this.notify();
									//Gbl.printCurrentThreadCpuTime();
								}
							}	
						}
						continue;
					}

					// this is an optimization, if we receive a large buffer, avoid copying it and send it directly.
					if (earray.size() >= eventsArraySize) {
						// make sure we don't miss events already buffered
						if (events.size() > 0) {
							distribute(events);
							events = new EventArray(eventsArraySize);
						}
						// send newly received events
						distribute(earray);
					}
					// this is the non-optimized path, where we receive small number of events at a time
					else {
						for (int i = 0; i < earray.size(); i++) {
							Event event = earray.get(i);
							events.add(event);
							// if the buffer is full or if we need to flush
							if (events.size() == eventsArraySize || shouldFlush) {
								distribute(events);
								events = new EventArray(eventsArraySize);
							}
						}	
					}
				}
			} catch (InterruptedException e) {
					return;
			}
		}
	}

	private class ProcessEventsRunnable extends Thread {
		
		private final EventsManager eventsManager;
		private final BlockingQueue<EventArray> eventsQueue;
		private boolean shouldFlush = false;
		
		public ProcessEventsRunnable(EventsManager eventsManager) {
			this.eventsManager = eventsManager;
			this.eventsQueue = new LinkedBlockingQueue<>();
		}

		public void flush() throws InterruptedException {
			synchronized (this) {
				shouldFlush = true;
				this.wait();	
			}
		}
		
		@Override
		public void run() {
			try {
				while (true) {
					EventArray events = this.eventsQueue.poll(100, TimeUnit.MILLISECONDS);
					
					if (events == null) {
						synchronized (this) {
							if (shouldFlush) {
								shouldFlush = false;
								this.notify();
								//Gbl.printCurrentThreadCpuTime();
							}
						}
						continue;
					}
					
					for (int i = 0; i < events.size(); i++) {
						this.eventsManager.processEvent(events.get(i));
					}
				}
			} catch (InterruptedException e) {
				return;
			}
		}	
	}
	
	/**
	 * @author mrieser
	 */
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicBoolean hadException;

		ExceptionHandler(final AtomicBoolean hadException) {
			this.hadException = hadException;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			this.hadException.set(true);
			log.error("Thread " + t.getName() + " died with exception while handling events.", e);
		}
	}
}
