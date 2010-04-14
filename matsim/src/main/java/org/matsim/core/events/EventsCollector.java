/* *********************************************************************** *
 * project: org.matsim.*
 * EventsCollector.java
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

import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.events.SimulationAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.SimulationAfterSimStepListener;

/**
 * This Implementation of an EventsManager collects the Events
 * during a SimStep and processes them afterwards.
 *
 * The Events are collected in a ConcurrentLinkedQueue which is
 * thread-safe so multiple write access are possible without
 * synchronization (e.g. from multiple threads from the parallel
 * QSim).
 *
 * Using an EventsMangerImpl will process all Events in the main
 * Thread after the SimStep. This ensures, that all events have
 * been processed before the next SimStep starts. This could for
 * example be necessary when using Within Day Replanning.
 *
 * Alternatively a ParallelEventsMangerImpl could be used. In that
 * case the collected Events are processed in a separate Thread
 * and the next SimStep is simulated before all Events are processed.
 *
 * Attention: Do not use it with JDEQSim! (It should create correct
 * Results but will be very slow because all Events would be processed
 * after the Simulation itself ended)
 *
 * @author cdobler
 */
public class EventsCollector extends EventsManagerImpl implements SimulationAfterSimStepListener {

	/*
	 * The EventsManager that is used the process the Events.
	 */
	private EventsManagerImpl eventsManager;

	/*
	 *  We use a ConcurrentLinkedQueue - it is thread-safe so we can
	 *  use it with the ParallelQSim without synchronization.
	 */
	private ConcurrentLinkedQueue<Event> events;

	/*
	 * Use the default EventsManager
	 */
	public EventsCollector()
	{
		this(new EventsManagerImpl());
	}

	/*
	 * Use a given EventsManager
	 */
	public EventsCollector(EventsManagerImpl eventsManager)
	{
		this.eventsManager = eventsManager;

		this.events = new ConcurrentLinkedQueue<Event>();
	}

	@Override
	public void addHandler(EventHandler handler)
	{
		eventsManager.addHandler(handler);
	}

	@Override
	public EventsFactory getFactory()
	{
		return eventsManager.getFactory();
	}

	@Override
	public void processEvent(Event event)
	{
		this.events.add(event);
	}

	@Override
	public void removeHandler(EventHandler handler)
	{
		eventsManager.removeHandler(handler);
	}

	@Override
	public void initProcessing()
	{
		eventsManager.initProcessing();
	}

	@Override
	public void finishProcessing()
	{
		eventsManager.finishProcessing();
	}

	/*
	 * We process all collected Events of a SimStep after the
	 * SimStep has been simulated.
	 */
	public void notifySimulationAfterSimStep(SimulationAfterSimStepEvent e)
	{
		Event event = null;
		while ((event = events.poll()) != null) eventsManager.processEvent(event);
	}

}