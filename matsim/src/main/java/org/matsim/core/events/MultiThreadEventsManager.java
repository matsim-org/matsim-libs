/* *********************************************************************** *
 * project: org.matsim.*
 * MultiThreadEventsManager.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.SimulationBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.SimulationBeforeCleanupListener;

/**
 * This Implementation of an EventsManager processes the Events
 * in a separate Thread what reduces the Simulation Time.
 * During each SimStep the Events are only collected in Lists
 * but are not processed. After the SimStep all collected Events
 * are taken and handed over to a separate Thread that processes
 * them.
 *
 * The used EventsDelegators are thread-safe, so they can be used
 * with the parallel QSim without further synchronization effort.
 *
 * Optionally all Events of a SimStep have to be processed before
 * the Simulation continues with the next SimStep. This could
 * be necessary when using Within Day Replanning.
 *
 * Attention: Do not use it with JDEQSim!
 *
 * @author cdobler
 */
public class MultiThreadEventsManager implements EventsManager, SimulationAfterSimStepListener, SimulationBeforeCleanupListener{

	final private static Logger log = Logger.getLogger(MultiThreadEventsManager.class);

	private EventsManager eventsManager;
	private ProcessEventsThread processEventsThread;
	private EventsDelegator[] eventsDelegators;

	/*
	 * For every SimStep a List of Queues. Each Queue contains
	 * the Events of one EventsDelegator.
	 */
	private LinkedBlockingQueue<List<Queue<Event>>> eventsQueues;

	/*
	 * When synchronizing to SimSteps all Events of a SimStep
	 * are processed before the next SimStep is simulated.
	 */
	private boolean syncToSimSteps;

	/*
	 * Use the default EventsManager
	 */
	public MultiThreadEventsManager(int numOfThreads, boolean syncToSimSteps)
	{
		this(new EventsManagerImpl(), numOfThreads, syncToSimSteps);
	}

	/*
	 * Use a given EventsManager
	 */
	public MultiThreadEventsManager(EventsManager eventsManager, int numOfThreads, boolean syncToSimSteps)
	{
		this.eventsManager = eventsManager;
		this.syncToSimSteps = syncToSimSteps;
		init(numOfThreads);
	}

	/*
	 *  Just for testing purposes as long as we have to
	 *  register a EventsDelegator in the Controler.
	 */
	public EventsDelegator[] getEventsDelegators()
	{
		return this.eventsDelegators;
	}

	private void init(int numOfThreads)
	{
		this.eventsDelegators = new EventsDelegator[numOfThreads];
		this.eventsQueues = new LinkedBlockingQueue<List<Queue<Event>>>();

		for (int i = 0; i < numOfThreads; i++)
		{
			EventsDelegator eventsDelegator = new EventsDelegator(this);
			eventsDelegators[i] = eventsDelegator;
		}

		processEventsThread = new ProcessEventsThread(this, eventsQueues, syncToSimSteps);
		processEventsThread.start();
	}

	public void addHandler(EventHandler handler) {
		eventsManager.addHandler(handler);
	}

	public EventsFactory getFactory() {
		return eventsManager.getFactory();
	}

	public void processEvent(Event event) {
		eventsManager.processEvent(event);
	}

	public void removeHandler(EventHandler handler) {
		eventsManager.removeHandler(handler);
	}

	/*
	 * Get Events from EventsDelagators to process them.
	 * Call this method when the QSim throws an AfterSimStep
	 * Event. At that time the Simulation is paused and
	 * we don't have to worry about synchronization issues.
	 */
	private void collectEvents()
	{
		List<Queue<Event>> simStepEvents = new ArrayList<Queue<Event>>();
		for (EventsDelegator eventsDelegator : this.eventsDelegators)
		{
			simStepEvents.add(eventsDelegator.getEventsQueue());
			eventsDelegator.createNewEventsQueue();
		}
		this.eventsQueues.add(simStepEvents);
	}

	/*
	 * When doing Within Day Replanning we have to wait until all Events
	 * have been processed! This could be done using a CyclicBarrier for example.
	 */
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e) {
		collectEvents();

		if (syncToSimSteps)
		{
			try
			{
				processEventsThread.getSimStepEndBarrier().await();
			}
			catch (InterruptedException e1)
			{
				Gbl.errorMsg(e1);
			}
			catch (BrokenBarrierException e1)
			{
				Gbl.errorMsg(e1);
			}
		}
	}

	/*
	 * If the simulation has ended we have to wait until all Events have
	 * been processed.
	 */
	public void notifySimulationBeforeCleanup(SimulationBeforeCleanupEvent e) {
		finishProcessing();
	}

	public void finishProcessing()
	{
		log.info("Simulation finished - waiting for EventsManager...");
		processEventsThread.setSimulationRunning(false);

		/*
		 * The ProcessThread may be waiting for a List of Queue<Events>.
		 * The Simulation won't send it, so we send a dummy List.
		 */
		List<Queue<Event>> dummyList = new ArrayList<Queue<Event>>();
		this.eventsQueues.add(dummyList);

		try
		{
			if (syncToSimSteps) processEventsThread.getSimStepEndBarrier().await();
			processEventsThread.getIterationEndBarrier().await();
		}
		catch (InterruptedException e1)
		{
			Gbl.errorMsg(e1);
		}
		catch (BrokenBarrierException e1)
		{
			Gbl.errorMsg(e1);
		}

		log.info("... done.");
	}

	private static class ProcessEventsThread extends Thread
	{
		private boolean simulationRunning = true;
		private EventsManager eventsManager;
		private LinkedBlockingQueue<List<Queue<Event>>> eventsQueues;
		private CyclicBarrier simStepEndBarrier;
		private CyclicBarrier iterationEndBarrier;
		private boolean syncToSimSteps;

		public ProcessEventsThread(EventsManager eventsManager, LinkedBlockingQueue<List<Queue<Event>>> eventsQueues, boolean syncToSimSteps)
		{
			this.eventsManager = eventsManager;
			this.eventsQueues = eventsQueues;
			this.syncToSimSteps = syncToSimSteps;

			init();
		}

		private void init()
		{
			this.setName("ProcessEventsThread");
			this.simStepEndBarrier = new CyclicBarrier(2);
			this.iterationEndBarrier = new CyclicBarrier(2);
		}

		public void setSimulationRunning(boolean value)
		{
			this.simulationRunning = value;
		}

		public CyclicBarrier getSimStepEndBarrier()
		{
			return this.simStepEndBarrier;
		}

		public CyclicBarrier getIterationEndBarrier()
		{
			return this.iterationEndBarrier;
		}

		@Override
		public void run()
		{
			try
			{
				/*
				 * If the Simulation has ended we may still have some
				 * Events left to process. So we continue until the
				 * eventsList is empty.
				 */
				while(simulationRunning || eventsQueues.peek() != null)
				{
					List<Queue<Event>> queues = eventsQueues.take();

					for (Queue<Event> events : queues)
					{
						Event event = null;
						while ((event = events.poll()) != null)
						{
							eventsManager.processEvent(event);
						}
					}
					if (syncToSimSteps) simStepEndBarrier.await();
				}
				iterationEndBarrier.await();
			}
			catch (InterruptedException e)
			{
				Gbl.errorMsg(e);
			}
			catch (BrokenBarrierException e)
			{
				Gbl.errorMsg(e);
			}
			Gbl.printCurrentThreadCpuTime();
		}
	}	// ProcessEventsThread

}
