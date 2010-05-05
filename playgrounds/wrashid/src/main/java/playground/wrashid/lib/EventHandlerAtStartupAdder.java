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

package playground.wrashid.lib;

import java.util.LinkedList;

import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

public class EventHandlerAtStartupAdder implements StartupListener {

	LinkedList<EventHandler> eventHandler = new LinkedList<EventHandler>();

	public void addEventHandler(EventHandler eventHandler) {
		this.eventHandler.add(eventHandler);
	}

	public void notifyStartup(StartupEvent event) {
		// add handlers
		for (int i = 0; i < eventHandler.size(); i++) {
			event.getControler().getEvents().addHandler(eventHandler.get(i));
		}
	}

}
