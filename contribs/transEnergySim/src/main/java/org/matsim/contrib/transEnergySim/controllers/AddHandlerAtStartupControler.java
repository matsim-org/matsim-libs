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

package org.matsim.contrib.transEnergySim.controllers;

import java.util.LinkedList;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;

/**
 * @author wrashid
 *
 */
public class AddHandlerAtStartupControler extends Controler {

	protected LinkedList<EventHandler> handler = new LinkedList<EventHandler>();

	public AddHandlerAtStartupControler(Config config) {
		super(config);
		addControlerListener(new EventHandlerAdder());
	}

	public AddHandlerAtStartupControler(String[] args) {
		super(args);
		addControlerListener(new EventHandlerAdder());
	}

	public void addHandler(EventHandler handler) {
		this.handler.add(handler);
	}

	private class EventHandlerAdder implements StartupListener {

		@Override
		public void notifyStartup(StartupEvent event) {
			for (EventHandler h : handler) {
				getEvents().addHandler(h);
			}
		}

	}

}
