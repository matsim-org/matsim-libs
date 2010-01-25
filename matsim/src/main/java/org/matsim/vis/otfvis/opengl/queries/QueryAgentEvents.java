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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.vis.otfvis.OTFVisQSimFeature;
import org.matsim.vis.otfvis.data.OTFServerQuad2;
import org.matsim.vis.otfvis.interfaces.OTFDrawer;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;

/**
 * QueryAgentEvents shows a visual representation of the agents route along with the events happening
 * to the agent. The events will only show from the time the has first been selected. No past events will be shown.
 *  
 * @author dstrippgen
 *
 */
public class QueryAgentEvents extends AbstractQuery implements PersonEventHandler {

	private static final long serialVersionUID = -7388598935268835323L;
	
	private static transient Logger logger = Logger.getLogger(QueryAgentEvents.class);
	
	public static class Result implements OTFQueryResult {

		private static final long serialVersionUID = 1L;

		private String agentId;
		private List<String> newEventStrings = new ArrayList<String>();
		
		public void draw(OTFDrawer drawer) {
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

	private Id agentId;
	
	private EventsManager eventsManager;
	
	private BlockingQueue<PersonEvent> queue = new LinkedBlockingQueue<PersonEvent>();
	
	private Result result;

	@Override
	public void handleEvent(PersonEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
			logger.debug("Got an event for my person. I am: " + this);
		}
	}
	
	@Override
	public void installQuery(OTFVisQSimFeature queueSimulation, EventsManager events, OTFServerQuad2 quad) {
		this.eventsManager = events;
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
		List<PersonEvent> newEvents = new ArrayList<PersonEvent>();
		queue.drainTo(newEvents);
		for (PersonEvent personEvent : newEvents) {
			result.newEventStrings.add(personEvent.toString());
		}
		return result;
	}

	public void setId(String id) {
		this.agentId = new IdImpl(id);
	}

	@Override
	public void reset(int iteration) {
		
	}

	@Override
	public void uninstall() {
		eventsManager.removeHandler(this);
		logger.debug("Events query deregistered from handler.");
	}

}