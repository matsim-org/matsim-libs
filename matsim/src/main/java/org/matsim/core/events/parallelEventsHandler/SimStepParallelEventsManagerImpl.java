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

package org.matsim.core.events.parallelEventsHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;

/**
 * An EventsHandler that handles all occurring Events in separate Threads.
 * When a Time Step of the QSim ends, all Events that have been created
 * in that Time Step are processed before the simulation can go on.
 * This is necessary e.g. when using Within Day Replanning.
 * 
 * @author cdobler
 */
public class SimStepParallelEventsManagerImpl extends EventsManagerImpl implements MobsimAfterSimStepListener {
	
	private final int numOfThreads;
	private CyclicBarrier simStepEndBarrier;
	private CyclicBarrier iterationEndBarrier;
	CyclicBarrier waitForEmptyQueuesBarrier;
	private Thread[] threads;
	private ProcessEventsRunnable[] runnables;
	private EventsManager[] eventsManagers;
	private ProcessedEventsChecker processedEventsChecker;
	
	private boolean parallelMode = false;
	private int handlerCount = 0;
	
	public SimStepParallelEventsManagerImpl() {
		this(1);
	}
	
	public SimStepParallelEventsManagerImpl(int numOfThreads) {
		this.numOfThreads = numOfThreads;
		init();
	}
	
	private void init() {
		
		this.simStepEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
		this.iterationEndBarrier = new CyclicBarrier(this.numOfThreads + 1);
		
		this.eventsManagers = new EventsManager[this.numOfThreads];
		for (int i = 0; i < numOfThreads; i++) this.eventsManagers[i] = EventsUtils.createEventsManager();
	}

	@Override
	public void processEvent(final Event event) {
		if (parallelMode) {
			runnables[0].processEvent(event);
		} else super.processEvent(event);
	}

	@Override
	public void addHandler(final EventHandler handler) {
		super.addHandler(handler);
		
		eventsManagers[handlerCount % numOfThreads].addHandler(handler);
		handlerCount++;
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		super.removeHandler(handler);
		
		for (EventsManager eventsManager : eventsManagers) eventsManager.removeHandler(handler);
	}
	
	@Override
	public void resetCounter() {
		super.resetCounter();
		
		for (EventsManager eventsManager : eventsManagers) ((EventsManagerImpl) eventsManager).resetCounter();
	}

	@Override
	public void clearHandlers() {
		super.clearHandlers();
		
		for (EventsManager eventsManager : eventsManagers) ((EventsManagerImpl) eventsManager).clearHandlers();
	}

	@Override
	public void initProcessing() {
		super.initProcessing();

		Queue<Event>[] eventsQueuesArray = new Queue[this.numOfThreads];	
		List<Queue<Event>> eventsQueues = new ArrayList<Queue<Event>>();
		for (int i = 0; i < numOfThreads; i++) {
			Queue<Event> eventsQueue = new LinkedBlockingQueue<Event>();
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
		waitForEmptyQueuesBarrier = new CyclicBarrier(this.numOfThreads, processedEventsChecker);
		
		threads = new Thread[numOfThreads];
		runnables = new ProcessEventsRunnable[numOfThreads];
		for (int i = 0; i < numOfThreads; i++) {
			ProcessEventsRunnable processEventsRunnable = new ProcessEventsRunnable(eventsManagers[i], processedEventsChecker,
					waitForEmptyQueuesBarrier, simStepEndBarrier, iterationEndBarrier, eventsQueues.get(i), eventsQueues.get(i + 1));
			runnables[i] = processEventsRunnable;
			Thread thread = new Thread(processEventsRunnable);
			threads[i] = thread;
			thread.setDaemon(true);
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
		super.finishProcessing();
		try {
			this.processEvent(new LastEventOfIteration(0.0));
			iterationEndBarrier.await();
		} catch (InterruptedException e) {
			Gbl.errorMsg(e);
		} catch (BrokenBarrierException e) {
			Gbl.errorMsg(e);
		}
		
		/*
		 * After the simulation Events are processed in
		 * the Main Thread.
		 */
		this.parallelMode = false;
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		try {
			this.processedEventsChecker.setTime(e.getSimulationTime());
			this.processEvent(new LastEventOfSimStep(e.getSimulationTime()));
			simStepEndBarrier.await();
		} catch (InterruptedException e1) {
			Gbl.errorMsg(e1);
		} catch (BrokenBarrierException e1) {
			Gbl.errorMsg(e1);
		}
	}
	
	private static class ProcessEventsRunnable implements Runnable, EventsManager {
		
		private final EventsManager eventsManager;
		private final ProcessedEventsChecker processedEventsChecker;
		private final CyclicBarrier waitForEmptyQueuesBarrier;
		private final CyclicBarrier simStepEndBarrier;
		private final CyclicBarrier iterationEndBarrier;
		private final Queue<Event> eventsQueue;
		private final Queue<Event> nextEventsQueue;

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
				while (true) {
					Event event = ((LinkedBlockingQueue<Event>) eventsQueue).take();
					
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
			} catch (InterruptedException e) {
				Gbl.errorMsg(e);
			} catch (BrokenBarrierException e) {
				Gbl.errorMsg(e);
			}
			Gbl.printCurrentThreadCpuTime();
		}

		@Override
		public void processEvent(Event event) {
			this.eventsQueue.add(event);
		}
		
		@Override
		public void addHandler(EventHandler handler) {
			throw new RuntimeException("This method should never be called - calls should go to the EventHandlers directly.");
		}

		@Override
		public EventsFactory getFactory() {
			throw new RuntimeException("This method should never be called - calls should go to the EventHandlers directly.");
		}

		@Override
		public void removeHandler(EventHandler handler) {
			throw new RuntimeException("This method should never be called - calls should go to the EventHandlers directly.");
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
	
}
