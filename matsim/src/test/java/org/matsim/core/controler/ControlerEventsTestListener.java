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

package org.matsim.core.controler;

import java.util.List;
import java.util.Vector;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;


public class ControlerEventsTestListener implements
		IterationStartsListener, IterationEndsListener, StartupListener, ShutdownListener {


	private StartupEvent startupEvent;
	private ShutdownEvent shutdownEvent;
  private final List<IterationStartsEvent> startIterationEvents = new Vector<IterationStartsEvent>();
  private final List<IterationEndsEvent> endIterationEvents = new Vector<IterationEndsEvent>();
	private int number;

	private ControlerEventsTest testClass;

	public ControlerEventsTestListener(int i, ControlerEventsTest test) {
		this.number = i;
		this.testClass = test;
	}

	public void notifyStartup(final StartupEvent controlerStartupEvent) {
		this.startupEvent = controlerStartupEvent;
		this.testClass.addCalledStartupListenerNumber(this.number);
	}

	public void notifyShutdown(final ShutdownEvent controlerShudownEvent) {
		this.shutdownEvent = controlerShudownEvent;
	}


	public void notifyIterationEnds(final IterationEndsEvent event) {
		this.endIterationEvents.add(event);
	}

	public void notifyIterationStarts(final IterationStartsEvent event) {
		this.startIterationEvents.add(event);
	}

	/**
	 * @return the startupEvent
	 */
	public StartupEvent getStartupEvent() {
		return this.startupEvent;
	}

	/**
	 * @return the shutdownEvent
	 */
	public ShutdownEvent getShutdownEvent() {
		return this.shutdownEvent;
	}


	/**
	 * @return the finishIterationEvents
	 */
	public List<IterationEndsEvent> getIterationEndsEvents() {
		return this.endIterationEvents;
	}


	/**
	 * @return the setupIterationEvents
	 */
	public List<IterationStartsEvent> getIterationStartsEvents() {
		return this.startIterationEvents;
	}

	public void reset() {
		this.startIterationEvents.clear();
		this.endIterationEvents.clear();
		this.shutdownEvent = null;
		this.startupEvent = null;
	}


}
