/* *********************************************************************** *
 * project: org.matsim.*
 * QueryAgentPlan.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.opengl.queries;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * Dumps all events occuring to an agent to the log.
 *  
 * @author michaz
 *
 */
public class QueryAgentEvents extends AbstractQuery implements BasicEventHandler {

	private static Logger logger = Logger.getLogger(QueryAgentEvents.class);

	public static class Result implements OTFQueryResult {

		private String agentId;
		private List<String> newEventStrings = new ArrayList<>();
		
		@Override
		public void draw(OTFOGLDrawer drawer) {
			for(String eventString : newEventStrings) {
				logger.info(agentId + ": " + eventString);
			}
			newEventStrings.clear();
		}

		@Override
		public boolean isAlive() {
			return true;
		}

		@Override
		public void remove() {
			
		}
		
	}

	private Id<Person> agentId = null;

	private EventsManager eventsManager = null;
	
	private BlockingQueue<Event> queue = new LinkedBlockingQueue<>();
	
	private Result result = null;
	
	@Override
	public void installQuery(SimulationViewForQueries simulationView) {
		this.eventsManager = simulationView.getEvents();
		result = new Result();
		result.agentId = agentId.toString();
		eventsManager.addHandler(this);
		logger.debug("Query initialized.");
	}

	@Override
	public Type getType() {
		return OTFQuery.Type.AGENT;
	}

	@Override
	public OTFQueryResult query() {
		result.newEventStrings.clear();
		List<Event> newEvents = new ArrayList<>();
		queue.drainTo(newEvents);
		for (Event personEvent : newEvents) {
			result.newEventStrings.add(personEvent.toString());
		}
		return result;
	}

	@Override
	public void setId(String id) {
		this.agentId = Id.create(id, Person.class);
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void uninstall() {
		eventsManager.removeHandler(this);
		logger.debug("Events query deregistered from handler.");
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof HasPersonId) {
			HasPersonId personEvent = (HasPersonId) event;
			if (personEvent.getPersonId().equals(this.agentId)) {
				queue.add(event);
			}
		}
	}

}