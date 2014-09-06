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

package playground.christoph.events;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.LastEventOfIteration;
import org.matsim.core.events.LastEventOfSimStep;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;

/**
 * @author cdobler
 */
public class ParallelEventsManager implements EventsManager {

	private final static Logger log = Logger.getLogger(ParallelEventsManager.class);
	
	private Phaser simStepEndBarrier;
	private Phaser iterationEndBarrier;
	private Phaser waitForEmptyQueuesBarrier;

	private Distributor distributor;
	
	private Map<EventHandler, ProcessEventsRunnable> eventsRunnables;
	private Map<EventHandler, EventsManager> eventsManagers;
	
	private EventsManagerImpl delegate;
	
	private ProcessedEventsChecker processedEventsChecker;
	
	private boolean syncOnTimeSteps = true;
	private boolean parallelMode = false;
	private int handlerCount = 0;
	
	private AtomicBoolean hadException;
	private ExceptionHandler uncaughtExceptionHandler;
	
	public ParallelEventsManager() {
		init();
	}
	
	private void init() {
		this.hadException = new AtomicBoolean(false);
		
		this.simStepEndBarrier = new Phaser(1);
		this.iterationEndBarrier = new Phaser(1);
		
		this.delegate = new EventsManagerImpl();

		this.eventsManagers = new LinkedHashMap<EventHandler, EventsManager>();
	}

	@Override
	public void processEvent(final Event event) {
		if (parallelMode) this.distributor.inputQueue.add(event);
		else delegate.processEvent(event);
	}

	@Override
	public void addHandler(final EventHandler handler) {
		this.delegate.addHandler(handler);
		
//		EventsManager eventsManager = new EventsManagerImpl();
//		eventsManager.addHandler(handler);
		EventsManager eventsManager = new SingleHandlerEventsManager(handler);
		this.eventsManagers.put(handler, eventsManager);
		
		this.handlerCount++;
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		this.delegate.removeHandler(handler);
		this.eventsManagers.remove(handler);
	}
	
	@Override
	public void resetHandlers(int iteration) {
		this.delegate.resetHandlers(iteration);
	}

	@Override
	public void initProcessing() {
		this.delegate.initProcessing();
		for (EventsManager eventsManager : this.eventsManagers.values()) eventsManager.initProcessing();
	
		this.distributor = new Distributor();
		
		/*
		 * Create a ProcessedEventsChecker that checks whether all Events of
		 * a time step have been processed.
		 */
		this.processedEventsChecker = new ProcessedEventsChecker(this, this.distributor.inputQueue);
		
		this.simStepEndBarrier = new Phaser(this.handlerCount + 1);
		this.iterationEndBarrier = new Phaser(this.handlerCount + 1);
		
		// Create a Barrier that the threads use to synchronize.
		this.waitForEmptyQueuesBarrier = new Phaser(this.handlerCount) {
			@Override
			protected boolean onAdvance(int phase, int registeredParties) {
				processedEventsChecker.run();
				return super.onAdvance(phase, registeredParties);
			}
		};
		
		this.hadException.set(false);
		this.uncaughtExceptionHandler = new ExceptionHandler(this.hadException, this.waitForEmptyQueuesBarrier, 
				this.simStepEndBarrier, this.iterationEndBarrier);
		
		this.eventsRunnables = new LinkedHashMap<EventHandler, ProcessEventsRunnable>();
		for (Entry<EventHandler, EventsManager> entry : this.eventsManagers.entrySet()) {
			EventHandler eventHandler = entry.getKey();
			EventsManager eventsManager = entry.getValue();
			
			ProcessEventsRunnable processEventsRunnable = new ProcessEventsRunnable(eventsManager, processedEventsChecker,
					waitForEmptyQueuesBarrier, simStepEndBarrier, iterationEndBarrier);

			eventsRunnables.put(eventHandler, processEventsRunnable);
			Thread thread = new Thread(processEventsRunnable);
			thread.setDaemon(true);
			thread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
			thread.setName(eventHandler.getClass().toString());
			thread.start();
		}		
		
		Thread distributorThread = new Thread(distributor);
		distributorThread.setDaemon(true);
		distributorThread.setUncaughtExceptionHandler(this.uncaughtExceptionHandler);
		distributorThread.setName("EventsDistributor");
		distributorThread.start();
		
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
		if (!this.hadException.get()) {
			this.processEvent(new LastEventOfIteration(Double.MAX_VALUE));
			iterationEndBarrier.arriveAndAwaitAdvance();
		}
		
		delegate.finishProcessing();
		for (EventsManager eventsManager : this.eventsManagers.values()) eventsManager.finishProcessing();
		
		/*
		 * After the simulation Events are processed in
		 * the Main Thread.
		 */
		this.parallelMode = false;
		
		if (this.hadException.get()) {
			throw new RuntimeException("Exception while processing events. Cannot guarantee that all events have been fully processed.");
		}
	}

	@Override
	public void afterSimStep(double time) {
		/*
		 * If an exception occurred, at least one of the events processing threads
		 * has crashed. Therefore the remaining threads would get stuck at the Phaser.
		 */
		if (hadException.get()) {
			return;
		}
		if (syncOnTimeSteps) {
			this.processedEventsChecker.setTime(time);
			this.processEvent(new LastEventOfSimStep(time));
			this.simStepEndBarrier.arriveAndAwaitAdvance();			
		}
	}
	
	private class Distributor implements Runnable {

		
		public final BlockingQueue<Event> inputQueue = new LinkedBlockingQueue<>();
		
		@Override
		public void run() {
			try {
				ProcessEventsRunnable[] runnables = new ProcessEventsRunnable[eventsRunnables.size()];
				
				int i = 0;
				for (ProcessEventsRunnable runnable : eventsRunnables.values()) {
					runnables[i] = runnable;
					i++;
				}
				
				while (true) {
					Event event = inputQueue.take();
					
					for (ProcessEventsRunnable runnable : runnables) {
						runnable.eventsQueue.add(event);
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
		private final BlockingQueue<Event> eventsQueue;
		private double lastEventTime = 0.0;

		public ProcessEventsRunnable(EventsManager eventsManager, ProcessedEventsChecker processedEventsChecker, 
				Phaser waitForEmptyQueuesBarrier, Phaser simStepEndBarrier,
				Phaser iterationEndBarrier) {
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
			lastEventTime = 0.0;
			try {
				while (true) {					
					Event event = this.eventsQueue.take();
					
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
						waitForEmptyQueuesBarrier.arriveAndAwaitAdvance();
						if (!processedEventsChecker.allEventsProcessed()) continue;
						
						/*
						 * All event queues are empty, therefore finish current time step by
						 * reaching the sim step end barrier.
						 */
						simStepEndBarrier.arriveAndAwaitAdvance();
						continue;
					} else if (event instanceof LastEventOfIteration) {
						/*
						 * If it is the last Event of the iteration, break the while loop
						 * and end the parallel events processing.
						 * TODO: Check whether still some events could be left in the queues...
						 */
						break;
					}
					
					this.eventsManager.processEvent(event);
				}
			} catch (InterruptedException e) {
				hadException.set(true);
			}
			iterationEndBarrier.arriveAndAwaitAdvance();

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