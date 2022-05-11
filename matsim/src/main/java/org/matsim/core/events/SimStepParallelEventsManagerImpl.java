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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.ParallelEventHandlingConfigGroup;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;

/**
 * An EventsHandler that handles all occurring Events in separate Threads.
 * When a Time Step of the QSim ends, all Events that have been created
 * in that Time Step are processed before the simulation can go on.
 * This is necessary e.g. when using Within-day Replanning.
 *
 * @author cdobler
 */
class SimStepParallelEventsManagerImpl implements EventsManager {

	private final static Logger log = Logger.getLogger(SimStepParallelEventsManagerImpl.class);

	private final int numOfThreads;
	private CyclicBarrier simStepEndBarrier;
	private CyclicBarrier iterationEndBarrier;
    private ProcessEventsRunnable[] runnables;
	private EventsManagerImpl[] eventsManagers;
	private EventsManagerImpl delegate;
	private ProcessedEventsChecker processedEventsChecker;

	private boolean parallelMode = false;
	private int handlerCount = 0;

	private AtomicLong counter;
	private AtomicReference<Throwable> hadException = new AtomicReference<>();

	@Inject
	SimStepParallelEventsManagerImpl(ParallelEventHandlingConfigGroup config) {
		this(config.getNumberOfThreads() != null ? config.getNumberOfThreads() : 1);
	}

    public SimStepParallelEventsManagerImpl() {
		this(1);
	}

	public SimStepParallelEventsManagerImpl(int numOfThreads) {
		this.numOfThreads = numOfThreads;
		log.info("number of threads=" + numOfThreads );
		init();
	}

	private void init() {
		this.counter = new AtomicLong(0);

		this.simStepEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
		this.iterationEndBarrier = new CyclicBarrier(this.numOfThreads + 1);

		this.delegate = new EventsManagerImpl();

		this.eventsManagers = new EventsManagerImpl[this.numOfThreads];
		for (int i = 0; i < numOfThreads; i++) this.eventsManagers[i] = new EventsManagerImpl();
	}

	@Override
	public void processEvent(final Event event) {
		this.counter.incrementAndGet();

		if (parallelMode) {
			// pass it to the event queue of the first event processing thread, it will pass it further
			runnables[0].processEvent(event);
		} else {
			delegate.processEvent(event);
		}
	}

	@Override
	public void addHandler(final EventHandler handler) {
		delegate.addHandler(handler);

		eventsManagers[handlerCount % numOfThreads].addHandler(handler);
		handlerCount++;
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		delegate.removeHandler(handler);

		for (EventsManager eventsManager : eventsManagers) eventsManager.removeHandler(handler);
	}

	@Override
	public void resetHandlers(int iteration) {
		delegate.resetHandlers(iteration);
		counter.set(0);
	}

	@Override
	public void initProcessing() {
		delegate.initProcessing();
		for (EventsManager eventsManager : this.eventsManagers) eventsManager.initProcessing();

		Queue<Event>[] eventsQueuesArray = new Queue[this.numOfThreads];
		List<Queue<Event>> eventsQueues = new ArrayList<Queue<Event>>();
		for (int i = 0; i < numOfThreads; i++) {
			Queue<Event> eventsQueue = new LinkedBlockingQueue<>();
			eventsQueues.add(eventsQueue);
			eventsQueuesArray[i] = eventsQueue;
		}

		/*
		 * Add a null entry to the list which will be set as nextEventsQueue in the last ProcessEventsThread.
		 */
		eventsQueues.add(null);

		/*
		 * Create a ProcessedEventsChecker that checks whether all Events of
		 * a time step have been processed.
		 */
		processedEventsChecker = new ProcessedEventsChecker(this, eventsQueuesArray);

		/*
		 *  Create a Barrier that the threads use to synchronize.
		 */
        CyclicBarrier waitForEmptyQueuesBarrier = new CyclicBarrier(this.numOfThreads, processedEventsChecker);

		hadException = new AtomicReference<>();
        ExceptionHandler uncaughtExceptionHandler = new ExceptionHandler(hadException, waitForEmptyQueuesBarrier,
                simStepEndBarrier, iterationEndBarrier);

		runnables = new ProcessEventsRunnable[numOfThreads];
		for (int i = 0; i < numOfThreads; i++) {
			ProcessEventsRunnable processEventsRunnable = new ProcessEventsRunnable(eventsManagers[i], processedEventsChecker,
                    waitForEmptyQueuesBarrier, simStepEndBarrier, iterationEndBarrier, eventsQueues.get(i), eventsQueues.get(i + 1));
			runnables[i] = processEventsRunnable;
			Thread thread = new Thread(processEventsRunnable);
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
			thread.setName(ProcessEventsRunnable.class.toString() + i);
			thread.start();
		}

		/*
		 * During the simulation Events are processed in
		 * the EventsProcessingThreads.
		 */
		this.parallelMode = true;
	}

	/*
	 * In some chases Events are created after this method has been called.
	 * To ensure that they are processed in real time, we process them not
	 * in the parallel thread. To do so, we replace the parallel events manager
	 * with its EventsManager instance.
	 */
	@Override
	public synchronized void finishProcessing() {

		/*
		 * If an exception occurred, at least one of the events processing threads
		 * has crashed. As a result, also all other events processing threads have
		 * been stopped.
		 * If not, it is waited until all threads have ended processing events.
		 */
		if (hadException.get() == null) {
			try {
				this.processEvent(new LastEventOfIteration(Double.POSITIVE_INFINITY));
				iterationEndBarrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				// prefer storing the original exception and not the follow-up BrokenCyclicBarrier exceptions
				if (hadException.get() == null) {
					// let's log it as it may be superseded by exceptions thrown during finishProcessing() by
					// the delegate or one of the eventManagers
					log.error("Exception caught while finishing event processing at the iteration end.", e);
					this.hadException.set(e);
				}
			}
        }

		delegate.finishProcessing();
		for (EventsManager eventsManager : this.eventsManagers) eventsManager.finishProcessing();

		/*
		 * After the simulation Events are processed in
		 * the Main Thread.
		 */
		this.parallelMode = false;

		if (hadException.get() != null) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.", hadException.get());
		}
	}

	@Override
	public void afterSimStep(double time) {

		/*
		 * If an exception occurred, at least one of the events processing threads
		 * has crashed. Therefore the remaining threads would get stuck at the
		 * CyclicBarrier.
		 */
		if (hadException.get() != null) {
			throw new RuntimeException(hadException.get());
		}

		try {
			Gbl.assertNotNull( this.processedEventsChecker );
			this.processedEventsChecker.setTime(time);
			this.processEvent(new LastEventOfSimStep(time));
			simStepEndBarrier.await();
		} catch (InterruptedException | BrokenBarrierException e) {
			if (hadException.get() != null) {
				throw new RuntimeException(hadException.get());
			}
			throw new RuntimeException(e);
		}
    }

	private static class ProcessEventsRunnable implements Runnable {

		private final EventsManager eventsManager;
		private final ProcessedEventsChecker processedEventsChecker;
		private final CyclicBarrier waitForEmptyQueuesBarrier;
		private final CyclicBarrier simStepEndBarrier;
		private final CyclicBarrier iterationEndBarrier;
		private final Queue<Event> eventsQueue;
		private final Queue<Event> nextEventsQueue;
		private double lastEventTime = 0.0;

		public ProcessEventsRunnable(EventsManager eventsManager, ProcessedEventsChecker processedEventsChecker,
				CyclicBarrier waitForEmptyQueuesBarrier,CyclicBarrier simStepEndBarrier,
				CyclicBarrier iterationEndBarrier, Queue<Event> eventsQueue, Queue<Event> nextEventsQueue) {
			this.eventsManager = eventsManager;
			this.processedEventsChecker = processedEventsChecker;
			this.waitForEmptyQueuesBarrier = waitForEmptyQueuesBarrier;
			this.simStepEndBarrier = simStepEndBarrier;
			this.iterationEndBarrier = iterationEndBarrier;
			this.eventsQueue = eventsQueue;
			this.nextEventsQueue = nextEventsQueue;
		}

		@Override
		public void run() {
			try {
				/*
				 * If the Simulation has ended we may still have some
				 * Events left to process. So we continue until the
				 * eventsQueue is empty.
				 *
				 * The loop is ended by a break command when a LastEventOfIteration
				 * event is found.
				 */
				lastEventTime = 0.0;
				while (true) {
					Event event = ((LinkedBlockingQueue<Event>) eventsQueue).take();

					/*
					 * Check whether the events are ordered chronologically.
					 */
					if (event.getTime() < this.lastEventTime) {
						throw new RuntimeException("Events in the queue are not ordered chronologically. " +
								"This should never happen. Is the SimTimeStepParallelEventsManager registered " +
								"as a MobsimAfterSimStepListener?");
					} else {
						this.lastEventTime = event.getTime();
					}

					if (event instanceof LastEventOfSimStep) {
						/*
						 * Send the event to the next events processing thread, if this is not the last thread.
						 */
						if (nextEventsQueue != null) {
							nextEventsQueue.add(event);
						}

						/*
						 * At the moment, this thread's queue is empty. However, one of the other threads
						 * could create additional events for this time step. Therefore we have to wait
						 * until all threads reach this barrier. Afterwards we can check whether still
						 * all queues are empty. If this is true, the threads reach the sim step end barrier.
						 */
						waitForEmptyQueuesBarrier.await();
						if (!processedEventsChecker.allEventsProcessed()) continue;

						/*
						 * All event queues are empty, therefore finish current time step by
						 * reaching the sim step end barrier.
						 */
						simStepEndBarrier.await();
						continue;
					}
					else {
						/*
						 * Handing the event over to the next events processing thread.
						 *
						 * This could be added to the next EventHandlers queue here or in
						 * the processEvent(...) method. Doing it here might be a bit faster
						 * since it is not done in the main thread.
						 */
						if (nextEventsQueue != null) {
							nextEventsQueue.add(event);
						}

						/*
						 * If it is the last Event of the iteration, break the while loop
						 * and end the parallel events processing.
						 * TODO: Check whether still some events could be left in the queues...
						 */
						if (event instanceof LastEventOfIteration) {
							break;
						}
					}
					eventsManager.processEvent(event);
				}
				iterationEndBarrier.await();
			} catch (InterruptedException | BrokenBarrierException e) {
				throw new RuntimeException(e);
			}
            Gbl.printCurrentThreadCpuTime();
		}

		public void processEvent(Event event) {
			this.eventsQueue.add(event);
		}

	}	// ProcessEventsRunnable

	private static class ProcessedEventsChecker implements Runnable {

		private final EventsManager evenentsManger;
		private final Queue<Event>[] eventQueues;
		private boolean allEventsProcessed;
		private double time;

		public ProcessedEventsChecker(EventsManager evenentsManger, Queue<Event>[] eventQueues) {
			this.evenentsManger = evenentsManger;
			this.eventQueues = eventQueues;

			this.allEventsProcessed = true;
		}

		public void setTime(double time) {
			this.time = time;
		}

		public boolean allEventsProcessed() {
			return this.allEventsProcessed;
		}

		@Override
		public void run() {
			for (Queue<Event> eventsQueue : eventQueues) {
				/*
				 * Some EventHandlers might have created additional Events [1] which
				 * could be located in the list AFTER the LastEventOfSimStep, meaning
				 * that they would be processed while the simulation is already processing
				 * the next time step. Therefore we check whether the eventsQueues are really
				 * empty.
				 * If at least one of the queues is not empty, this time steps
				 * events processing has to go on. This is triggered by setting
				 * allEventsProcessed to false. Additionally a last event of sim
				 * step event is created. When all events processing threads
				 * process that event, it is again checked whether there are
				 * more events left.
				 *
				 * [1] ... Such a behavior is NOT part of MATSim's default EventHandlers but it
				 * still might occur.
				 */
				if (eventsQueue.size() > 0) {
					allEventsProcessed = false;
					evenentsManger.processEvent(new LastEventOfSimStep(time));
					return;
				}
			}

			// otherwise
			allEventsProcessed = true;
		}

	}	// ProcessedEventsChecker

	/**
	 * @author mrieser
	 */
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicReference<Throwable> hadException;
		private final CyclicBarrier simStepEndBarrier;
		private final CyclicBarrier iterationEndBarrier;
		private final CyclicBarrier waitForEmptyQueuesBarrier;

		public ExceptionHandler(final AtomicReference<Throwable> hadException, CyclicBarrier waitForEmptyQueuesBarrier,
				CyclicBarrier simStepEndBarrier, CyclicBarrier iterationEndBarrier) {
			this.hadException = hadException;
			this.waitForEmptyQueuesBarrier = waitForEmptyQueuesBarrier;
			this.simStepEndBarrier = simStepEndBarrier;
			this.iterationEndBarrier = iterationEndBarrier;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			if (hadException.get() == null) {
				// store the original exception and not the follow-up BrokenCyclicBarrier exceptions
				this.hadException.set(e);
			}
			if (!(e instanceof BrokenBarrierException)) {
				// do not log BrokenBarrierException -- they are triggered by resetting the barrier due to
				// another (original) exception
				log.error("Thread " + t.getName() + " died with exception while handling events.", e);
			}
			/*
			 * By reseting the barriers, they will throw a BrokenBarrierException
			 * which again will stop the events processing threads.
			 */
			this.simStepEndBarrier.reset();
			this.iterationEndBarrier.reset();
			this.waitForEmptyQueuesBarrier.reset();
		}

	}

}
