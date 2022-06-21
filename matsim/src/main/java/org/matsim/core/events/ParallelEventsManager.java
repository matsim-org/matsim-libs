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
import javax.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author cdobler
 */
public final class ParallelEventsManager implements EventsManager {

	private final static Logger log = Logger.getLogger(ParallelEventsManager.class);

	private Distributor distributor;
	private ArrayList<EventsManager> eventsManagers = new ArrayList<>();
	private final List<EventHandler> eventsHandlers;
	private final boolean oneThreadPerHandler;
	private final boolean syncOnTimeSteps;
	private final int numOfThreads;
	private final ExceptionHandler uncaughtExceptionHandler;
	private int iteration = 0;
	private boolean init = false;
	private final BlockingQueue<EventArray> eventQueue;

	private final int eventsQueueSize;
	//private final int eventsQueueSize = 1048576 * 32;
	private final int eventsArraySize;

	@Inject
	ParallelEventsManager(Config config) {
		this(config.parallelEventHandling().getSynchronizeOnSimSteps() != null ? config.parallelEventHandling().getSynchronizeOnSimSteps() : true, config.parallelEventHandling().getEventsQueueSize());

	}

	public ParallelEventsManager(final boolean syncOnTimeSteps) {
		this(syncOnTimeSteps, 65536);
	}

	public ParallelEventsManager(final boolean syncOnTimeSteps, int eventsQueueSize) {
		this(syncOnTimeSteps, true, -1,eventsQueueSize);

	}

	public ParallelEventsManager(final boolean syncOnTimeSteps, final int numOfThreads, int eventsQueueSize) {
		this(syncOnTimeSteps, false, numOfThreads, eventsQueueSize);
	}

	/*package*/ ParallelEventsManager(final boolean syncOnTimeSteps, final boolean oneThreadPerHandler, final int numOfThreads,final int eventsQueueSize) {
		this.syncOnTimeSteps = syncOnTimeSteps;
		this.oneThreadPerHandler = oneThreadPerHandler;
		this.numOfThreads = numOfThreads;
		this.eventsHandlers = new ArrayList<>();
		this.eventsArraySize = syncOnTimeSteps ? 512 : 32768;
		this.eventsQueueSize = eventsQueueSize;
		this.eventQueue = new ArrayBlockingQueue<>(eventsQueueSize);
		this.uncaughtExceptionHandler = new ExceptionHandler();
	}

	private void initialize() {
		int numHandlers = oneThreadPerHandler ? this.eventsHandlers.size() : Math.min(this.numOfThreads, this.eventsHandlers.size());
		this.distributor = new Distributor(new ArrayList<ProcessEventsRunnable>(), eventQueue);
		this.eventsManagers = new ArrayList<>(numHandlers);

		// create event managers
		if (this.oneThreadPerHandler) {
			for (int i = 0; i < this.eventsHandlers.size(); i++) {
				this.eventsManagers.add(new SingleHandlerEventsManager(this.eventsHandlers.get(i)));
			}
		} else {
			// TODO - check if this slow path is correct
			for (int i = 0; i < this.numOfThreads; i++) {
				this.eventsManagers.add(new EventsManagerImpl());
			}
			for (int i = 0; i < this.eventsHandlers.size(); i++) {
				this.eventsManagers.get(i % numOfThreads).addHandler(this.eventsHandlers.get(i));
			}
		}

		// initialize runnables (threads that will execute the event managers)
		for (int i = 0; i < this.eventsManagers.size(); i++) {
			EventsManager eventsManager = this.eventsManagers.get(i);
			ProcessEventsRunnable processEventsRunnable = new ProcessEventsRunnable(eventsManager, distributor);
			distributor.runnables.add(processEventsRunnable);
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
		this.init = true;
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
			throw new RuntimeException("Exception while waiting on join...", e);
		}
		this.init = false;

	}

	@Override
	public void processEvent(final Event event) {
		if (!init) throw new IllegalStateException(".initProcessing() has to be called before processing events!");

		EventArray array = new EventArray(1);
		array.add(event);
		try {
			this.eventQueue.put(array);
		} catch (InterruptedException e) {
			throw new RuntimeException("Exception while adding event.", e);
		}
	}

	@Override
	public void processEvents(final EventArray events) {
		if (!init) throw new IllegalStateException(".initProcessing() has to be called before processing events!");

		try {
			this.eventQueue.put(events);
		} catch (InterruptedException e) {
			throw new RuntimeException("Exception while adding event.", e);
		}
	}

	@Override
	public void addHandler(final EventHandler handler) {
		if (init)
			throw new IllegalStateException("Handlers can not be added after .initProcessing() was called!");

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
		flush();

		for (EventsManager eventsManager : this.eventsManagers) {
			eventsManager.finishProcessing();
		}

		teardown();

		if (this.uncaughtExceptionHandler.hadException()) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.", this.uncaughtExceptionHandler.exception);
		}

		iteration += 1;
	}

	@Override
	public void afterSimStep(double time) {
		if (this.syncOnTimeSteps) {
			flush();
		}

		if (this.uncaughtExceptionHandler.hadException()) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.", this.uncaughtExceptionHandler.exception);
		}

	}

	public void flush() {
		try {
			this.distributor.flush();
		} catch (InterruptedException e) {
			throw new RuntimeException("Exception while waiting on flush... " + e.getMessage(), this.uncaughtExceptionHandler.exception);
		}
	}

	private class Distributor extends Thread {

		private final ArrayList<ProcessEventsRunnable> runnables;
		private final BlockingQueue<EventArray> eventQueue;

		// When set to true, the distributor will process all events until all events in the event manager are processed.
		// This is used when the simulation needs to sync with event processing and make sure there are no unprocessed
		// events in the system.
		private volatile boolean shouldFlush = false;

		public Distributor(ArrayList<ProcessEventsRunnable> runnables, BlockingQueue<EventArray> eventQueue) {
			this.runnables = runnables;
			this.eventQueue = eventQueue;
		}

		public void flush() throws InterruptedException {
			synchronized (this) {
				shouldFlush = true;
				while (shouldFlush && this.isAlive()) {
					this.wait(1);
				}
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
					EventArray earray = this.eventQueue.poll(50, TimeUnit.MICROSECONDS);
					if (earray == null) {
						synchronized (this) {
							// check if we can finish the flush
							if (shouldFlush) {
								// distribute missing events
								if (events.size() > 0) {
									distribute(events);
									events = new EventArray(eventsArraySize);
								}

								// We'll ask the ProcessEventsRunnables to flush
								for (ProcessEventsRunnable runnable : this.runnables)
									runnable.flush();

								// Wait until all the ProcessEventsRunnables have finished flushing
								boolean stillFlushing = true;
								while (stillFlushing) {
									this.wait(1);

									stillFlushing = false;
									for (ProcessEventsRunnable runnable : this.runnables)
										stillFlushing |= runnable.isFlushing();
								}

								// termination criteria for the flush
								if (eventQueue.isEmpty()) {
									shouldFlush = false;
									this.notify();
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

		private final Distributor distributor;
		private final EventsManager eventsManager;
		private final BlockingQueue<EventArray> eventsQueue;
		private boolean flush = false;

		public ProcessEventsRunnable(EventsManager eventsManager, Distributor distributor) {
			this.eventsManager = eventsManager;
			this.eventsQueue = new LinkedBlockingQueue<>();
			this.distributor = distributor;
		}

		public synchronized void flush() {
			flush = true;
		}

		public boolean isFlushing() {
			return flush && this.isAlive();
		}

		@Override
		public void run() {
			try {
				while (true) {
					EventArray events = this.eventsQueue.poll(50, TimeUnit.MICROSECONDS);

					if (events != null) {
						for (int i = 0; i < events.size(); i++) {
							this.eventsManager.processEvent(events.get(i));
						}
					}

					// If flush is over, then try to wake up distributor
					if (flush && this.eventsQueue.isEmpty()) {
						synchronized (this) {
							flush = false;
						}
						synchronized (this.distributor) {
							this.distributor.notify();
						}
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

		private volatile boolean hadException = false;
		private volatile Throwable exception;


		@Override
		public void uncaughtException(Thread t, Throwable e) {
			this.hadException = true;
			this.exception = e;
			log.error("Thread " + t.getName() + " died with exception while handling events.", e);
		}

		public boolean hadException() {
			return this.hadException;
		}

		public Throwable exception() {
			return this.exception;
		}

	}
}
