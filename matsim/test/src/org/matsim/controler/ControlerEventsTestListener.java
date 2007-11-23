/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerEventsTestListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.controler;

import java.util.List;
import java.util.Vector;

import org.matsim.controler.events.ControlerFinishIterationEvent;
import org.matsim.controler.events.ControlerSetupIterationEvent;
import org.matsim.controler.events.ControlerShutdownEvent;
import org.matsim.controler.events.ControlerStartupEvent;
import org.matsim.controler.listener.ControlerFinishIterationListener;
import org.matsim.controler.listener.ControlerSetupIterationListener;
import org.matsim.controler.listener.ControlerShutdownListener;
import org.matsim.controler.listener.ControlerStartupListener;


public class ControlerEventsTestListener implements
		ControlerFinishIterationListener, ControlerSetupIterationListener, ControlerStartupListener, ControlerShutdownListener {

	
	private ControlerStartupEvent startupEvent;
	private ControlerShutdownEvent shutdownEvent;
  private List<ControlerFinishIterationEvent> finishIterationEvents = new Vector<ControlerFinishIterationEvent>();
  private List<ControlerSetupIterationEvent> setupIterationEvents = new Vector<ControlerSetupIterationEvent>();
	
	
	public void notifyStartup(final ControlerStartupEvent controlerStartupEvent) {
		this.startupEvent = controlerStartupEvent;
	}

	public void notifyShutdown(final ControlerShutdownEvent controlerShudownEvent) {
		this.shutdownEvent = controlerShudownEvent;
	}

	
	public void notifyIterationFinished(final ControlerFinishIterationEvent event) {
		System.out.println("iteration finished");
		this.finishIterationEvents.add(event);
	}

	public void notifyIterationSetup(final ControlerSetupIterationEvent event) {
		System.out.println("iteration setup");
		this.setupIterationEvents.add(event);
	}

	
	/**
	 * @return the startupEvent
	 */
	public ControlerStartupEvent getStartupEvent() {
		return this.startupEvent;
	}

	
	/**
	 * @return the shutdownEvent
	 */
	public ControlerShutdownEvent getShutdownEvent() {
		return this.shutdownEvent;
	}

	
	/**
	 * @return the finishIterationEvents
	 */
	public List<ControlerFinishIterationEvent> getFinishIterationEvents() {
		return this.finishIterationEvents;
	}

	
	/**
	 * @return the setupIterationEvents
	 */
	public List<ControlerSetupIterationEvent> getSetupIterationEvents() {
		return this.setupIterationEvents;
	}

	public void reset() {
		this.finishIterationEvents.clear();
		this.setupIterationEvents.clear();
		this.shutdownEvent = null;
		this.startupEvent = null;
	}


}
