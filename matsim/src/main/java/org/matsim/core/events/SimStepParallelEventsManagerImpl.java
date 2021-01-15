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

import com.google.common.util.concurrent.AtomicDouble;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.handler.EventHandler;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

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
	private final AtomicDouble currentTimestep = new AtomicDouble(Double.NEGATIVE_INFINITY);

	// this delegate is used to handle events synchronously during un-initialized state
	private final EventsManager delegate = new EventsManagerImpl();

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

		// only test for order, if we are initialized. Some code in some contribs emits unordered events in between iterations
		if (event.getTime() < currentTimestep.get() && isInitialized.get()) {
			throw new RuntimeException("Event with time step: " + event.getTime() + " was submitted. But current timestep was: " + currentTimestep + ". Events must be ordered chronologically");
		}

		setCurrentTimestep(event.getTime());

		// register event here, to make sure finishProcessing works correctly awaits all submitted events
		phaser.register();

		if (isInitialized.get()) {
			// taking last processor because of initialization logic
			var processor = eventsProcessors.get(eventsProcessors.size() - 1);
			processor.addEvent(event);
			executorService.execute(processor);
		} else {
			delegate.processEvent(event);
			phaser.arriveAndDeregister();
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
	public synchronized void resetHandlers(int iteration) {
		awaitProcessingOfEvents();
		delegate.resetHandlers(iteration);
	}

	@Override
	public synchronized void initProcessing() {

		// wait for processing of events which were emitted in between iterations
		awaitProcessingOfEvents();
		isInitialized.set(true);
		currentTimestep.set(Double.NEGATIVE_INFINITY);
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

		// setting isInitialized before waiting for finishing of all events
		isInitialized.set(false);

		log.info("finishProcessing: Before awaiting all event processes");
		phaser.arriveAndAwaitAdvance();
		log.info("finishProcessing: After waiting for all events processes.");

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

	private void setCurrentTimestep(double time) {

		while (time > currentTimestep.get()) {
			var previousTime = currentTimestep.get();
			// wait for event handlers to process all events from previous time step including events emitted after 'afterSimStep' was called
			awaitProcessingOfEvents();
			currentTimestep.compareAndSet(previousTime, time);
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
				phaser.register();
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
