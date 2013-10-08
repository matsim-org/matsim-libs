/* *********************************************************************** *
 * project: org.matsim.*
 * InMemoryEventsManager
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.dgrether.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.handler.EventHandler;


/**
 * @author dgrether
 *
 */
public class InMemoryEventsManager implements EventsManager {

	private static final Logger log = Logger.getLogger(InMemoryEventsManager.class);
	
	private EventsManager delegate = new EventsManagerImpl();
	private List<Event> events = new ArrayList<Event>();
	private long counter = 0;
	private long nextCounterMsg = 1;

	public List<Event> getEvents(){
		return this.events;
	}
	
	@Override
	public void processEvent(Event event) {
		this.counter++;
		if (this.counter == this.nextCounterMsg) {
			this.nextCounterMsg *= 2;
			log.info(" event # " + this.counter);
		}
		this.events.add(event);
	}

	@Override
	public void addHandler(EventHandler handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removeHandler(EventHandler handler) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void resetHandlers(int iteration) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void initProcessing() {

	}
	@Override
	public void finishProcessing() {
	}

	@Override
	public void afterSimStep(double time) {
	}


}
