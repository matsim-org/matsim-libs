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
package playground.gregor.sim2d_v2.calibration.scenario;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

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
import org.xml.sax.SAXException;

import playground.gregor.sim2d_v2.events.XYZAzimuthEvent;
import playground.gregor.sim2d_v2.events.XYZEventsFileReader;
import playground.gregor.sim2d_v2.events.XYZEventsHandler;

/**
 * @author laemmel
 * 
 */
public class PhantomPopulationLoader implements XYZEventsHandler, AgentDepartureEventHandler, AgentArrivalEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler {

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
		XYZEventsFileReader reader = new XYZEventsFileReader(ev);
		try {
			reader.parse(this.file);
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return this.pe;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * playground.gregor.sim2d.events.XYZEventsHandler#handleEvent(playground
	 * .gregor.sim2d.events.XYZAzimuthEvent)
	 */
	@Override
	public void handleEvent(XYZAzimuthEvent event) {
		this.pe.addEvent(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.events.handler.EventHandler#reset(int)
	 */
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentDepartureEvent)
	 */
	@Override
	public void handleEvent(AgentDepartureEvent event) {
		this.pe.addEvent(event);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler
	 * #handleEvent(org.matsim.core.api.experimental.events.AgentArrivalEvent)
	 */
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
