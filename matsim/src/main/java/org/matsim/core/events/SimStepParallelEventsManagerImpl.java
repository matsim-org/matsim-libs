/* *********************************************************************** *
 * project: org.matsim.*
 * SimStepParallelEventsManagerImpl.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;

import javax.inject.Inject;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An EventsHandler that handles all occurring Events in separate Threads.
 * When a Time Step of the QSim ends, all Events that have been created
 * in that Time Step are processed before the simulation can go on.
 * This is necessary e.g. when using Within-day Replanning.
 *
 * This events manager handles submitted events in-between initProcessing and finishProcessing
 * concurrently. After finishProcessing was called, e.g. in-between iterations, events are
 * handled on the calling thread. Many things seem to rely on this logic.
 * 
 * @author cdobler
 */
public final class SimStepParallelEventsManagerImpl implements EventsManager {

	private final static Logger log = Logger.getLogger(SimStepParallelEventsManagerImpl.class);

	private final ExecutorService executorService;
	private final Phaser phaser = new Phaser(1);
	private final List<EventsManager> managers = new ArrayList<>();
	private final List<ProcessEventsRunnable> eventsProcessors = new ArrayList<>();
	private final int numberOfThreads;
	private final AtomicBoolean hasThrown = new AtomicBoolean(false);
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);

	// this delegate is used to handle events synchronously during un-initialized state
	private final EventsManager delegate = new EventsManagerImpl();

	private double currentTimestep = Double.NEGATIVE_INFINITY;
	private int handlerCount;

	@Inject
	SimStepParallelEventsManagerImpl(ParallelEventHandlingConfigGroup config) {
		this(config.getNumberOfThreads() != null ? config.getNumberOfThreads() : 2);
	}

    public SimStepParallelEventsManagerImpl() {
		this(2);
	}
	
	public SimStepParallelEventsManagerImpl(int numOfThreads) {
		this.numberOfThreads = numOfThreads;
		this.executorService = Executors.newWorkStealingPool(numberOfThreads);

		ProcessEventsRunnable nextProcessor = null;

		for (int i = 0; i < numberOfThreads; i++){
			var manager = new EventsManagerImpl();
			managers.add(manager);
			var processor = new ProcessEventsRunnable(manager, phaser, nextProcessor, executorService);
			nextProcessor = processor;
			eventsProcessors.add(processor);
		}
	}

	@Override
	public void processEvent(final Event event) {

		// only test for order, if we are initialized. Some code in some contribs emmits unordered events in between iterations
		if (event.getTime() < currentTimestep && isInitialized.get()) {
			throw new RuntimeException("Event with time step: " + event.getTime() + " was submitted. But current timestep was: " + currentTimestep + ". Events must be ordered chronologically");
		}

		// testing the condition here already, to minimize the number of calls to synchronized method
		if (event.getTime() > currentTimestep)
			setCurrentTimestep(event.getTime());

		if (isInitialized.get()) {
			// taking last processor because of initialization logic
			var processor = eventsProcessors.get(eventsProcessors.size() - 1);
			processor.addEvent(event);
			executorService.execute(processor);
		} else {
			delegate.processEvent(event);
		}
	}

	@Override
	public void addHandler(final EventHandler handler) {
		delegate.addHandler(handler);

		managers.get(handlerCount % numberOfThreads).addHandler(handler);
		handlerCount++;
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		delegate.removeHandler(handler);

		for (var manager : managers)
			manager.removeHandler(handler);
	}
	
	@Override
	public void resetHandlers(int iteration) {
		awaitProcessingOfEvents();
		delegate.resetHandlers(iteration);
	}

	@Override
	public void initProcessing() {

		// wait for processing of events which were emitted in between iterations
		awaitProcessingOfEvents();
		isInitialized.set(true);
		currentTimestep = Double.NEGATIVE_INFINITY;
		delegate.initProcessing();

		for (var manager : managers) {
			manager.initProcessing();
		}
	}
		
	/*
	 * In some chases Events are created after this method has been called.
	 * To ensure that they are processed in real time, we process them not
	 * in the parallel thread. To do so, we replace the parallel events manager
	 * with its EventsManager instance.
	 */
	@Override
	public synchronized void finishProcessing() {

		log.info("finishProcessing: Before awaiting all event processes");
		phaser.arriveAndAwaitAdvance();
		log.info("finishProcessing: After waiting for all events processes.");

		isInitialized.set(false);

		if (!hasThrown.get())
			throwExceptionIfAnyThreadCrashed();

		delegate.finishProcessing();
		for (var manager : managers) {
			manager.finishProcessing();
		}
	}

	@Override
	public void afterSimStep(double time) {

		// await processing of events emitted during a simulation step
		awaitProcessingOfEvents();

		delegate.afterSimStep(time);
		for (var manager : managers) {
			manager.afterSimStep(time);
		}
    }

	private synchronized void setCurrentTimestep(double time) {

		// this test must be inside synchronized block to make sure await is only called once
		if (time > currentTimestep) {
			// wait for event handlers to process all events from previous time step including events emitted after 'afterSimStep' was called
			awaitProcessingOfEvents();
			currentTimestep = time;
		}
	}

	private void awaitProcessingOfEvents() {
		phaser.arriveAndAwaitAdvance();
		throwExceptionIfAnyThreadCrashed();
	}

	private void throwExceptionIfAnyThreadCrashed() {
		eventsProcessors.stream()
				.filter(ProcessEventsRunnable::hadException)
				.findAny()
				.ifPresent(process -> {
					hasThrown.set(true);
					throw new RuntimeException(process.getCaughtException());
				});
	}
	
	private static class ProcessEventsRunnable implements Runnable {

		private final EventsManager manager;
		private final Phaser phaser;
		private final ProcessEventsRunnable nextProcessor;
		private final ExecutorService executorService;
		private final Queue<Event> eventQueue = new ConcurrentLinkedQueue<>();

		private Exception caughtException;

		private ProcessEventsRunnable(EventsManager manager, Phaser phaser, ProcessEventsRunnable nextProcessor, ExecutorService executorService) {
			this.manager = manager;
			this.phaser = phaser;
			this.nextProcessor = nextProcessor;
			this.executorService = executorService;
		}

		private boolean hadException() {
			return caughtException != null;
		}

		private Exception getCaughtException() {
			return caughtException;
		}

		void addEvent(Event event) {
			phaser.register();
			eventQueue.add(event);
		}

		/**
		 * This method must be synchronized to make sure the events are passed to the events manager in the order we've
		 * received them.
		 */
		@Override
		public synchronized void run() {
			var event = eventQueue.poll();
			notifyNextProcess(event);
			tryProcessEvent(event);
		}

		private void notifyNextProcess(Event event) {
			if (nextProcessor != null) {
				nextProcessor.addEvent(event);
				executorService.execute(nextProcessor);
			}
		}

		private void tryProcessEvent(Event event) {
			try {
				manager.processEvent(event);
			} catch (Exception e) {
				caughtException = e;
			} finally {
				phaser.arriveAndDeregister();
			}
		}
	}
}
