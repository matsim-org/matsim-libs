/* *********************************************************************** *
 * project: org.matsim.*
 * Controller.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.api.experimental.controller;

import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.run.Controler;

/**
 * 
 * @author michaz
 *
 */
public final class Controller {

	private org.matsim.core.controler.Controler controler;
	
	private ArrayList<EventHandler> eventHandlers = new ArrayList<EventHandler>();
	
	public Controller(final String[] args) {
		this.controler = new org.matsim.core.controler.Controler(args);
	}
	
	public Controller(final String configFilename) {
		this.controler = new org.matsim.core.controler.Controler(configFilename);
	}
	
	public void setOverwriteFiles(final boolean overwriteFiles) {
		this.controler.setOverwriteFiles(overwriteFiles);
	}
	
	public Scenario getScenario() {
		return this.controler.getScenario() ;
	}
	
	public void run() {
		ControlerListener startupListener = new StartupListener() {

			@Override
			public void notifyStartup(StartupEvent event) {
				for (EventHandler eventHandler : eventHandlers) {
					event.getControler().getEvents().addHandler(eventHandler);
				}
			}
			
		};
		this.controler.addControlerListener(startupListener);
		this.controler.run();
	}
	
	public void addEventHandler(EventHandler eventHandler) {
		eventHandlers.add(eventHandler);
	}

	public static void main(String[] args) {
		new Controler(args).run();
	}
	
}
