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
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.vis.otfvis.SimulationViewForQueries;
import org.matsim.vis.otfvis.interfaces.OTFQuery;
import org.matsim.vis.otfvis.interfaces.OTFQueryResult;
import org.matsim.vis.otfvis.opengl.drawer.OTFOGLDrawer;

/**
 * 
 * Dumps all events occuring to an agent to the log.
 *  
 * @author michaz
 *
 */
public class QueryAgentEvents extends AbstractQuery implements AgentDepartureEventHandler, AgentArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, AgentWait2LinkEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	private static transient Logger logger = Logger.getLogger(QueryAgentEvents.class);
	
	public static class Result implements OTFQueryResult {

		private String agentId;
		private List<String> newEventStrings = new ArrayList<String>();
		
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

	private Id agentId = null;
	
	private EventsManager eventsManager = null;
	
	private BlockingQueue<Event> queue = new LinkedBlockingQueue<Event>();
	
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
		List<Event> newEvents = new ArrayList<Event>();
		queue.drainTo(newEvents);
		for (Event personEvent : newEvents) {
			result.newEventStrings.add(personEvent.toString());
		}
		return result;
	}

	@Override
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

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(AgentWait2LinkEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		if(event.getPersonId().equals(this.agentId)){
			queue.add(event);
		}
	}

}