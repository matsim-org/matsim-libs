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
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.misc.Time;

import javax.inject.Inject;

/**
 * @author cdobler
 */
public class ParallelEventsManager implements EventsManager {

	private final static Logger log = Logger.getLogger(ParallelEventsManager.class);
	
	private Phaser simStepEndBarrier;
	private Phaser iterationEndBarrier;
	private Phaser waitForEmptyQueuesBarrier;

	private Distributor distributor;
	
	private EventsManager singleThreadEventsHandler;
	private EventsManager[] eventsManagers;
	private final List<EventHandler> eventsHandlers;
	
	private ProcessedEventsChecker processedEventsChecker;
	
	private final boolean syncOnTimeSteps;
	private final boolean oneThreadPerHandler;
	private final int numOfThreads;
	
	private boolean parallelMode = false;
		
	private final AtomicBoolean hadException;
	private ExceptionHandler uncaughtExceptionHandler;
	
	private boolean locked = false;

	/*
	 * Processed events are collected in an ArrayBlockingQueue. The distributor retrieves them and collects
	 * them in arrays. If an array is full or the time step ends (in case syncOnTimeSteps is true) or
	 * the simulation ends, the array is given to the event handlers which use LinkedBlockingQueues to
	 * collect them. By using those arrays, the memory overhead is reduced drastically (one array is handed
	 * over to all handlers; one entry in the LinkedBlockingQueues for each array and not for each event).
	 * 
	 * Reasonable values for the array size depends on whether syncOnTimeSteps is true. If yes, the value
	 * should be smaller than the typical number of events produced in a time step (otherwise, the events would
	 * be collected during the time step and processing would be triggered by the last event of the time step).
	 * 
	 * Values found in a 25 pct scenario of Switzerland (2m agents, 2m links, 1m nodes):
	 * 	- 11k events at 08:00:00
	 * 	- 12k events at 12:00:00
	 */
	private final int eventsQueueSize = 1048576;
//	private final int eventsArraySize = 32768;	// syncOnTimeSteps = false
//	private final int eventsArraySize = 512;	// syncOnTimeSteps = true
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
		
		this.simStepEndBarrier = new Phaser(1);
		this.iterationEndBarrier = new Phaser(1);
		
		this.eventsHandlers = new ArrayList<EventHandler>();
		this.singleThreadEventsHandler = new EventsManagerImpl();
		
		if (syncOnTimeSteps) this.eventsArraySize = 512;
		else this.eventsArraySize = 32768;
	}
	
	@Override
	public void processEvent(final Event event) {
		if (this.parallelMode) this.distributor.processEvent(event);
		else this.singleThreadEventsHandler.processEvent(event);
	}

	@Override
	public void addHandler(final EventHandler handler) {
		
		if (this.locked) throw new RuntimeException("Cannot add an event handler at the moment!");
		
		this.eventsHandlers.add(handler);
		this.singleThreadEventsHandler.addHandler(handler);
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		
		// not sure whether this happens while the simulation is running :?
//		if (this.locked) throw new RuntimeException("Cannot remove an event handler at the moment!");
		
		if (this.parallelMode) log.warn("Removing EventHandler while ParallelEventsHandler is in 'parallel' mode. This is not expected to happen :?");
		
		this.eventsHandlers.remove(handler);
		this.singleThreadEventsHandler.removeHandler(handler);
		
		if (this.eventsManagers != null) {
			for (EventsManager eventsManager : this.eventsManagers) {
				if (eventsManager instanceof SingleHandlerEventsManager) ((SingleHandlerEventsManager) eventsManager).deactivate();
				else eventsManager.removeHandler(handler);
			}
		}
	}
	
	@Override
	public void resetHandlers(int iteration) {
		this.singleThreadEventsHandler.resetHandlers(iteration);
	}

	@Override
	public void initProcessing() {
		
		this.locked = true;
		
		int numHandlers;
		if (this.oneThreadPerHandler) numHandlers = this.eventsHandlers.size();
		else numHandlers = Math.min(this.numOfThreads, this.eventsHandlers.size());	// don't create more managers than we have handlers
		
		this.eventsManagers = new EventsManager[numHandlers];
		if (this.oneThreadPerHandler) {
			for (int i = 0; i < this.eventsHandlers.size(); i++) this.eventsManagers[i] = new SingleHandlerEventsManager(this.eventsHandlers.get(i));
		} else {
			for (int i = 0; i < this.numOfThreads; i++) this.eventsManagers[i] = new EventsManagerImpl();
			for (int i = 0; i < this.eventsHandlers.size(); i++) this.eventsManagers[this.eventsHandlers.size() % numOfThreads].addHandler(this.eventsHandlers.get(i));
		}
		
		for (EventsManager eventsManager : this.eventsManagers) eventsManager.initProcessing();
		
		ProcessEventsRunnable[] eventsRunnables = new ProcessEventsRunnable[numHandlers]; 
		this.distributor = new Distributor(eventsRunnables);
		
		this.simStepEndBarrier = new Phaser(numHandlers + 1);
		this.iterationEndBarrier = new Phaser(numHandlers + 1);
		
		if (this.syncOnTimeSteps) {
			// Create a ProcessedEventsChecker that checks whether all Events of a time step have been processed.
			this.processedEventsChecker = new ProcessedEventsChecker(this, this.distributor.inputQueue);
			
			// Create a Barrier that the threads use to synchronize.
			this.waitForEmptyQueuesBarrier = new Phaser(numHandlers) {
				@Override
				protected boolean onAdvance(int phase, int registeredParties) {
					processedEventsChecker.run();
					return super.onAdvance(phase, registeredParties);
				}
			};
		} else {
			this.waitForEmptyQueuesBarrier = null;
			this.processedEventsChecker = null;
		}
		
		this.hadException.set(false);
		this.uncaughtExceptionHandler = new ExceptionHandler(this.hadException, this.waitForEmptyQueuesBarrier, 
				this.simStepEndBarrier, this.iterationEndBarrier);
		
		for (int i = 0; i < this.eventsManagers.length; i++) {
			EventsManager eventsManager = this.eventsManagers[i];
			ProcessEventsRunnable processEventsRunnable = new ProcessEventsRunnable(eventsManager, this.processedEventsChecker,
					this.waitForEmptyQueuesBarrier, this.simStepEndBarrier, this.iterationEndBarrier);
			
			eventsRunnables[i] = processEventsRunnable;
			Thread thread = new Thread(processEventsRunnable);
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
			if (eventsManager instanceof SingleHandlerEventsManager) thread.setName("SingleHandlerEventsManager: " + ((SingleHandlerEventsManager) eventsManager).getEventHandlerClassName());
			else thread.setName(ProcessEventsRunnable.class.toString() + i);
			thread.start();
		}
		
		Thread distributorThread = new Thread(this.distributor);
		distributorThread.setDaemon(true);
		distributorThread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
		distributorThread.setName("EventsDistributor");
		distributorThread.start();
		
		/*
		 * Enable parallel mode while simulation is running. It is disabled after the simulation in case additional
		 * events are created afterwards, e.g. money events by the road pricing contrib.
		 */
		this.parallelMode = true;
	}
		
	/*
	 * In some chases Events are created after this method has been called. To ensure that they are processed in 
	 * real time, we process them not in the parallel thread. To do so, we replace the parallel events manager
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
		if (!this.hadException.get()) {
			this.processEvent(new LastEventOfIteration(Double.MAX_VALUE));
			iterationEndBarrier.arriveAndAwaitAdvance();
		}
		
		for (EventsManager eventsManager : this.eventsManagers) eventsManager.finishProcessing();
		this.singleThreadEventsHandler.finishProcessing();
		
		this.eventsManagers = null;
		this.distributor = null;
		
		/*
		 * Disable parallel mode after the simulation has ended  in case additional
		 * events are created afterwards, e.g. money events by the road pricing contrib.
		 */
		this.parallelMode = false;
		
		if (this.hadException.get()) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.");
		}
		
		this.locked = false;
	}

	@Override
	public void afterSimStep(double time) {
		/*
		 * If an exception occurred, at least one of the events processing threads
		 * has crashed. Therefore the remaining threads would get stuck at the Phaser.
		 */
		if (this.hadException.get()) {
			return;
		}
		if (syncOnTimeSteps) {
			this.processedEventsChecker.setTime(time);
			this.processEvent(new LastEventOfSimStep(time));
			this.simStepEndBarrier.arriveAndAwaitAdvance();
		}
	}
	
	private class Distributor implements Runnable {

		private final ProcessEventsRunnable[] runnables;
		private final BlockingQueue<Event> inputQueue;
		
		public Distributor(ProcessEventsRunnable[] runnables) {
			this.runnables = runnables;
			this.inputQueue = new ArrayBlockingQueue<>(eventsQueueSize);
		}
		
		public final void processEvent(Event event) {
			this.inputQueue.add(event);
		}
		
		@Override
		public final void run() {
			try {
				int arrayPos = 0;
				Event[] events = new Event[eventsArraySize];
				while (true) {
					Event event = this.inputQueue.take();
					
					events[arrayPos] = event;
					arrayPos++;
					
					/*
					 * Hand events array to consumer threads if...
					 * 	- it has reached its capacity
					 * 	- we want to synchronize after each time step AND the current time step has ended
					 * 	- the simulation has ended
					 */
					if (arrayPos == eventsArraySize || (syncOnTimeSteps && event instanceof LastEventOfSimStep) || event instanceof LastEventOfIteration) {
						for (ProcessEventsRunnable runnable : this.runnables) {
							runnable.eventsQueue.add(events);
						}
						events = new Event[eventsArraySize];
						arrayPos = 0;
						
						// Break while loop so that the thread can shutdown.
						if (event instanceof LastEventOfIteration) break;
					}	
				}
			} catch (InterruptedException e) {
				hadException.set(true);
			}
		}
	}
	
	private class ProcessEventsRunnable implements Runnable {
		
		private final EventsManager eventsManager;
		private final ProcessedEventsChecker processedEventsChecker;
		private final Phaser waitForEmptyQueuesBarrier;
		private final Phaser simStepEndBarrier;
		private final Phaser iterationEndBarrier;
		private final BlockingQueue<Event[]> eventsQueue;
		private double lastEventTime = Time.UNDEFINED_TIME;

		public ProcessEventsRunnable(EventsManager eventsManager, ProcessedEventsChecker processedEventsChecker, 
				Phaser waitForEmptyQueuesBarrier, Phaser simStepEndBarrier, Phaser iterationEndBarrier) {
			this.eventsManager = eventsManager;
			this.processedEventsChecker = processedEventsChecker;
			this.waitForEmptyQueuesBarrier = waitForEmptyQueuesBarrier;
			this.simStepEndBarrier = simStepEndBarrier;
			this.iterationEndBarrier = iterationEndBarrier;
			this.eventsQueue = new LinkedBlockingQueue<>();
		}

		@Override
		public void run() {
			/*
			 * If the Simulation has ended we may still have some Events left to process.
			 * So we continue until the eventsQueue is empty.
			 * 
			 * The loop is ended by a break command when a LastEventOfIteration
			 * event is found.
			 */
			try {
				boolean foundLastEventOfIteration = false; 
				while (true && !foundLastEventOfIteration) {
					Event[] events = this.eventsQueue.take();
										
					for (Event event : events) {
						// Check whether the events are ordered chronologically.
						if (event.getTime() < this.lastEventTime) {
							throw new RuntimeException("Events in the queue are not ordered chronologically. " +
									"This should never happen. Is the ParallelEventsManager registered " +
									"as a MobsimAfterSimStepListener?");
						} else this.lastEventTime = event.getTime();
						
						if (event instanceof LastEventOfSimStep) {
							/*
							 * At the moment, this thread's queue is empty. However, one of the other threads
							 * could create additional events for this time step. Therefore we have to wait
							 * until all threads reach this barrier. Afterwards we can check whether still
							 * all queues are empty. If this is true, the threads reach the sim step end barrier.
							 */
							this.waitForEmptyQueuesBarrier.arriveAndAwaitAdvance();
							if (!this.processedEventsChecker.allEventsProcessed()) continue;
							
							/*
							 * All event queues are empty, therefore finish current time step by
							 * reaching the sim step end barrier.
							 */
							this.simStepEndBarrier.arriveAndAwaitAdvance();
							
							// the remaining entries in the array are null, therefore skip them
//							continue;
							break;
						} else if (event instanceof LastEventOfIteration) {
							/*
							 * If it is the last Event of the iteration, break the while loop
							 * and end the parallel events processing.
							 * TODO: Check whether still some events could be left in the queues...
							 */
							// the remaining entries in the array are null, therefore skip them
							foundLastEventOfIteration = true;
							break;
						}
						
						this.eventsManager.processEvent(event);						
					}
				}
			} catch (InterruptedException e) {
				hadException.set(true);
			}
			this.iterationEndBarrier.arriveAndAwaitAdvance();

			Gbl.printCurrentThreadCpuTime();
		}	
	}	// ProcessEventsRunnable
	
	private static class ProcessedEventsChecker implements Runnable {

		private final EventsManager evenentsManger;
		private final Queue<Event> eventsQueue;
		private boolean allEventsProcessed;
		private double time;
		
		public ProcessedEventsChecker(EventsManager evenentsManger, Queue<Event> eventsQueue) {
			this.evenentsManger = evenentsManger;
			this.eventsQueue = eventsQueue;
			
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
			/*
			 * Some EventHandlers might have created additional Events [1] which could be located in the list AFTER 
			 * the LastEventOfSimStep, meaning that they would be processed while the simulation is already processing
			 * the next time step. Therefore we check whether the eventsQueues are really empty.
			 * 
			 * If at least one of the queues is not empty, this time steps events processing has to go on. This is 
			 * triggered by setting allEventsProcessed to false. Additionally a last event of sim step event is created.
			 * When all events processing threads process that event, it is again checked whether there are more events left.
			 * 
			 * [1] ... Such a behavior is NOT part of MATSim's default EventHandlers but it still might occur.
			 */
			if (!this.eventsQueue.isEmpty()) {
				this.allEventsProcessed = false;
				this.evenentsManger.processEvent(new LastEventOfSimStep(time));
				return;
			}
			
			// otherwise: clear events queue and go on
			this.eventsQueue.clear();
			allEventsProcessed = true;
		}
	}	// ProcessedEventsChecker
	
	/**
	 * @author mrieser
	 */
	private static class ExceptionHandler implements UncaughtExceptionHandler {

		private final AtomicBoolean hadException;
		private final Phaser simStepEndBarrier;
		private final Phaser iterationEndBarrier;
		private final Phaser waitForEmptyQueuesBarrier;

		public ExceptionHandler(final AtomicBoolean hadException, Phaser waitForEmptyQueuesBarrier,
				Phaser simStepEndBarrier, Phaser iterationEndBarrier) {
			this.hadException = hadException;
			this.waitForEmptyQueuesBarrier = waitForEmptyQueuesBarrier;
			this.simStepEndBarrier = simStepEndBarrier;
			this.iterationEndBarrier = iterationEndBarrier;
		}

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			this.hadException.set(true);
			log.error("Thread " + t.getName() + " died with exception while handling events.", e);

			/*
			 * By reseting the barriers, they will throw a BrokenBarrierException
			 * which again will stop the events processing threads.
			 */
			this.simStepEndBarrier.forceTermination();
			this.iterationEndBarrier.forceTermination();
			this.waitForEmptyQueuesBarrier.forceTermination();
		}
	}
}