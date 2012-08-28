/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.lib.obj.event;

import java.util.LinkedList;

import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.events.handler.EventHandler;


public class EventHandlerCodeSeparator implements ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler,
		AgentArrivalEventHandler, AgentDepartureEventHandler {

	private LinkedList<ActivityStartEventHandler> activityStartEventHandlers;
	private LinkedList<ActivityEndEventHandler> activityEndEventHandlers;
	private LinkedList<LinkEnterEventHandler> linkEnterEventHandlers;
	private LinkedList<AgentArrivalEventHandler> arrivalEventHandlers;
	private LinkedList<AgentDepartureEventHandler> departureEventHandlers;

	public EventHandlerCodeSeparator() {
		activityStartEventHandlers = new LinkedList<ActivityStartEventHandler>();
		activityEndEventHandlers = new LinkedList<ActivityEndEventHandler>();
		linkEnterEventHandlers = new LinkedList<LinkEnterEventHandler>();
		arrivalEventHandlers = new LinkedList<AgentArrivalEventHandler>();
		departureEventHandlers = new LinkedList<AgentDepartureEventHandler>();
	}

	public void addHandler(EventHandler eventHandler) {
		if (eventHandler instanceof ActivityStartEventHandler) {
			activityStartEventHandlers.add((ActivityStartEventHandler) eventHandler);
		}
		
		if (eventHandler instanceof ActivityEndEventHandler) {
			activityEndEventHandlers.add((ActivityEndEventHandler) eventHandler);
		}
		
		if (eventHandler instanceof LinkEnterEventHandler) {
			linkEnterEventHandlers.add((LinkEnterEventHandler) eventHandler);
		}
		
		if (eventHandler instanceof AgentArrivalEventHandler) {
			arrivalEventHandlers.add((AgentArrivalEventHandler) eventHandler);
		}
		
		if (eventHandler instanceof AgentDepartureEventHandler) {
			departureEventHandlers.add((AgentDepartureEventHandler) eventHandler);
		} 
		
		
	}

	@Override
	public void reset(int iteration) {
		for (EventHandler eventHandler:activityEndEventHandlers){
			eventHandler.reset(iteration);
		}
		
		for (EventHandler eventHandler:activityEndEventHandlers){
			eventHandler.reset(iteration);
		}

		for (EventHandler eventHandler:linkEnterEventHandlers){
			eventHandler.reset(iteration);
		}
		
		for (EventHandler eventHandler:arrivalEventHandlers){
			eventHandler.reset(iteration);
		}
		
		for (EventHandler eventHandler:departureEventHandlers){
			eventHandler.reset(iteration);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		for (ActivityStartEventHandler eventHandler : activityStartEventHandlers) {
			eventHandler.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		for (ActivityEndEventHandler eventHandler : activityEndEventHandlers) {
			eventHandler.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		for (AgentDepartureEventHandler eventHandler : departureEventHandlers) {
			eventHandler.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		for (AgentArrivalEventHandler eventHandler : arrivalEventHandlers) {
			eventHandler.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		for (LinkEnterEventHandler eventHandler : linkEnterEventHandlers) {
			eventHandler.handleEvent(event);
		}
	}
}
