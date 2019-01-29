/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.parking.parkingchoice.lib;

import java.util.LinkedList;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

/**
 * This class does not carry documentation by its author.  I suspect that Rashid programmed it because the EventsManager
 * (i.e. services.getEvents()) used to be unavailable between new Controler and services.run().  This is, however,
 * no longer the case, and so the present class should be deprecated. 
 * 
 * @author (of javadoc) nagel
  */
@Deprecated // see javadoc of class
public final class EventHandlerAtStartupAdder implements StartupListener {

	LinkedList<EventHandler> eventHandlers = new LinkedList<EventHandler>();

	@Deprecated // see javadoc of class
	public EventHandlerAtStartupAdder(){
		
	}
	
	@Deprecated // see javadoc of class
	public EventHandlerAtStartupAdder(EventHandler eventHandler){
		addEventHandler(eventHandler);
	}
	
	@Deprecated // see javadoc of class
	public void addEventHandler(EventHandler eventHandler) {
		this.eventHandlers.add(eventHandler);
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		// add handlers
		for (int i = 0; i < eventHandlers.size(); i++) {
			event.getServices().getEvents().addHandler(eventHandlers.get(i));
		}
	}

}
