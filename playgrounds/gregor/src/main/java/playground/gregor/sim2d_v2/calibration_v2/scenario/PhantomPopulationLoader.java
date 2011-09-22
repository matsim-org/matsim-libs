/* *********************************************************************** *
 * project: org.matsim.*
 * PhantomPopulationLoader.java
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
package playground.gregor.sim2d_v2.calibration_v2.scenario;

import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.EventsUtils;

import playground.gregor.sim2d_v2.events.XYVxVyEvent;
import playground.gregor.sim2d_v2.events.XYVxVyEventsFileReader;
import playground.gregor.sim2d_v2.events.XYVxVyEventsHandler;

/**
 * @author laemmel
 * 
 */
public class PhantomPopulationLoader implements XYVxVyEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler {

	private final String file;
	//	private List<Event> phantomPopulation;
	private final PhantomEvents pe = new PhantomEvents();

	/**
	 * @param file
	 */
	public PhantomPopulationLoader(String file) {
		this.file = file;
	}

	/**
	 * @return
	 */
	public PhantomEvents getPhantomPopulation() {
		EventsManager ev = EventsUtils.createEventsManager();
		ev.addHandler(this);
		XYVxVyEventsFileReader reader = new XYVxVyEventsFileReader(ev);
		reader.parse(this.file);
		return this.pe;
	}

	@Override
	public void handleEvent(XYVxVyEvent event) {
		this.pe.addEvent(event);
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.pe.addEvent(event);
	}

	@Override
	public void handleEvent(AgentArrivalEvent event) {
		this.pe.addEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.pe.addEvent(event);

	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.pe.addEvent(event);

	}

}
