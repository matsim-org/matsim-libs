/* *********************************************************************** *
 * project: org.matsim.*
 * EventsDelegator.java
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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.events.handler.EventHandler;

/**
 * A class that collects Events that should be
 * processed. After every SimStep the MultiThreadEventsManager 
 * takes those events and processes them in a separated Thread.
 * 
 * Instead of a ConcurrentLinkedQueue we could use a simple
 * List if each parallel QSim Thread would use his own
 * EventsDelegator.
 * 
 * @author cdobler
 */

// Should be "implements EventsManger" but Controler needs a EventsManagerImpl...
//public class EventsDelegator implements EventsManager {
public class EventsDelegator extends EventsManagerImpl{

	private MultiThreadEventsManager eventsManager;
	private Queue<Event> events;
	
	public EventsDelegator(MultiThreadEventsManager eventsManager)
	{
		this.eventsManager = eventsManager;
		this.events = new ConcurrentLinkedQueue<Event>();
	}
	
	public void addHandler(EventHandler handler)
	{
		this.eventsManager.addHandler(handler);
	}

	public EventsFactory getFactory()
	{
		return this.eventsManager.getFactory();
	}

	public void processEvent(Event event)
	{
		this.events.add(event);
	}

	public void removeHandler(EventHandler handler)
	{
		this.eventsManager.removeHandler(handler);
	}

	public Queue<Event> getEventsQueue()
	{
		return this.events;
	}
	
	protected void createNewEventsQueue()
	{
		this.events = new ConcurrentLinkedQueue<Event>();
	}
	
//	// Just a Hack to not need the SimulationBeforeCleanupListener
//	public void finishProcessing()
//	{
//		((MultiThreadEventsManager)this.eventsManager).finishProcessing();
//	}
}
