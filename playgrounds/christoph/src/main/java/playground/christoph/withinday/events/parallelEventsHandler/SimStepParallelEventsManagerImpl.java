/* *********************************************************************** *
 * project: org.matsim.*
 * SimStepParallelEventsManagerImpl.java
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

package playground.christoph.withinday.events.parallelEventsHandler;

import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.events.parallelEventsHandler.LastEventOfIteration;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;

/**
 * An EventsHandler that handles all occurring Events in a separate Thread.
 * When a Time Step of the QSim ends, all Events that have been created
 * in that Time Step are processed before the simulation can go on.
 * This is necessary e.g. when using Within Day Replanning.
 * 
 * @author cdobler
 * 
 */
public class SimStepParallelEventsManagerImpl extends EventsManagerImpl implements SimulationAfterSimStepListener {
	
	private CyclicBarrier simStepEndBarrier = null;
	private CyclicBarrier iterationEndBarrier = null;

	private ProcessEventsThread processEventsThread;
	
	/*
	 * Points to the EventsManager that is currently active.
	 * This may be the eventsManager or the processEventsThread
	 * (which again uses the eventsManger). 
	 */
	private EventsManager activeEventsManager;
	
	/*
	 * The EventsManger that really handles the Events.
	 * During the running Simulation it is used by the
	 * ProcessEventsThread, afterwards by the Main Thread
	 * (e.g. if Agent Money Events are created after
	 * the Simulation has already ended).
	 */
	private EventsManagerImpl eventsManager;
	
	public SimStepParallelEventsManagerImpl() {
		init();
	}
	
	private void init() {
		eventsManager = new EventsManagerImpl();
		
		this.simStepEndBarrier = new CyclicBarrier(2);
		this.iterationEndBarrier = new CyclicBarrier(2);
		
		/*
		 * Before the Simulation is started, events are
		 * processed in the Main Thread.
		 */
		this.activeEventsManager = eventsManager;
	}

	@Override
	public void processEvent(final Event event) {
		activeEventsManager.processEvent(event);
	}

	@Override
	public void addHandler(final EventHandler handler) {
		activeEventsManager.addHandler(handler);
	}

	@Override
	public void removeHandler(final EventHandler handler) {
		activeEventsManager.removeHandler(handler);
	}

	@Override
	public EventsFactory getFactory() {
		return activeEventsManager.getFactory();
	}
	
	@Override
	public void resetHandlers(final int iteration) {
		eventsManager.resetHandlers(iteration);
	}

	@Override
	public void resetCounter() {
		eventsManager.resetCounter();
	}

	@Override
	public void clearHandlers() {
		eventsManager.clearHandlers();
	}

	@Override
	public void printEventHandlers() {
		eventsManager.printEventHandlers();
	}

	@Override
	public void initProcessing() {
//		super.initProcessing();
		eventsManager.initProcessing();
		
		processEventsThread = new ProcessEventsThread();
		processEventsThread.setEventsManager(this.eventsManager);
		processEventsThread.setSimStepEndBarrier(this.simStepEndBarrier);
		processEventsThread.setIterationEndBarrier(this.iterationEndBarrier);
		processEventsThread.setDaemon(true);
		processEventsThread.start();
		
		/*
		 *  While the simulation is running, the events are
		 *  processed in the parallel Thread.
		 */
		this.activeEventsManager = processEventsThread;
	}
		
	/*
	 * In some chases Events are created after this method has been called.
	 * To ensure that they are processed in real time, we process them not
	 * in the parallel thread. To do so, we replace the parallel events manager
	 * with its EventsManager instance.
	 */
	@Override
	public synchronized void finishProcessing() {
//		super.finishProcessing();
		eventsManager.finishProcessing();
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
		this.activeEventsManager = eventsManager;
	}

	@Override
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		try {
			this.processEvent(new LastEventOfSimStep(e.getSimulationTime()));
			simStepEndBarrier.await();
		} catch (InterruptedException e1) {
			Gbl.errorMsg(e1);
		} catch (BrokenBarrierException e1) {
			Gbl.errorMsg(e1);
		}
	}
	
	private static class ProcessEventsThread extends Thread implements EventsManager {
		
		private EventsManagerImpl eventsManager;
		private CyclicBarrier simStepEndBarrier;
		private CyclicBarrier iterationEndBarrier;
		private Queue<Event> eventsQueue;

		public ProcessEventsThread() {
			init();
		}

		private void init() {
			this.setName("ProcessEventsThread");
			this.eventsQueue = new LinkedBlockingQueue<Event>();
		}

		public void setEventsManager(EventsManagerImpl eventsManager) {
			this.eventsManager = eventsManager;
		}

		public void setSimStepEndBarrier(CyclicBarrier simStepEndBarrier) {
			this.simStepEndBarrier = simStepEndBarrier;
		}
		
		public void setIterationEndBarrier(CyclicBarrier iterationEndBarrier) {
			this.iterationEndBarrier = iterationEndBarrier;
		}

		@Override
		public void run() {
			try {
				/*
				 * If the Simulation has ended we may still have some
				 * Events left to process. So we continue until the
				 * eventsList is empty.
				 */
				while (true) {
					Event event = ((LinkedBlockingQueue<Event>) eventsQueue).take();
					
					if (event instanceof LastEventOfSimStep) {
						simStepEndBarrier.await();
						continue;
					}
					else if (event instanceof LastEventOfIteration) {
						break;
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
		public void addHandler(EventHandler handler) {
			this.eventsManager.addHandler(handler);
		}

		@Override
		public EventsFactory getFactory() {
			return this.eventsManager.getFactory();
		}

		@Override
		public void processEvent(Event event) {
			this.eventsQueue.add(event);
		}

		@Override
		public void removeHandler(EventHandler handler) {
			this.eventsManager.removeHandler(handler);
		}

	}	// ProcessEventsThread
}
