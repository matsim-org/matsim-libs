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

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.events.handler.EventHandler;


public class EventHandlerCodeSeparator implements ActivityStartEventHandler, ActivityEndEventHandler, LinkEnterEventHandler,
		PersonArrivalEventHandler, PersonDepartureEventHandler {

	private LinkedList<ActivityStartEventHandler> activityStartEventHandlers;
	private LinkedList<ActivityEndEventHandler> activityEndEventHandlers;
	private LinkedList<LinkEnterEventHandler> linkEnterEventHandlers;
	private LinkedList<PersonArrivalEventHandler> arrivalEventHandlers;
	private LinkedList<PersonDepartureEventHandler> departureEventHandlers;

	public EventHandlerCodeSeparator() {
		activityStartEventHandlers = new LinkedList<ActivityStartEventHandler>();
		activityEndEventHandlers = new LinkedList<ActivityEndEventHandler>();
		linkEnterEventHandlers = new LinkedList<LinkEnterEventHandler>();
		arrivalEventHandlers = new LinkedList<PersonArrivalEventHandler>();
		departureEventHandlers = new LinkedList<PersonDepartureEventHandler>();
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
		
		if (eventHandler instanceof PersonArrivalEventHandler) {
			arrivalEventHandlers.add((PersonArrivalEventHandler) eventHandler);
		}
		
		if (eventHandler instanceof PersonDepartureEventHandler) {
			departureEventHandlers.add((PersonDepartureEventHandler) eventHandler);
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
	public void handleEvent(PersonDepartureEvent event) {
		for (PersonDepartureEventHandler eventHandler : departureEventHandlers) {
			eventHandler.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		for (PersonArrivalEventHandler eventHandler : arrivalEventHandlers) {
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
