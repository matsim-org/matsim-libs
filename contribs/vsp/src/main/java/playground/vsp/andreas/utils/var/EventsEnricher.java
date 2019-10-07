/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.vsp.andreas.utils.var;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.events.handler.BasicEventHandler;

/**
 * 
 * Add missing event attributes, e.g. LinkEnterEvent used to have no vehicleId
 * 
 * @author aneumann
 *
 */
public class EventsEnricher implements BasicEventHandler {
	
	private static final Logger log = Logger.getLogger(EventsEnricher.class);
	
	private EventWriterXML eventsWriter;

	private HashMap<Id, Id> driver2vehicleIdMap;
	
	
	
	public EventsEnricher(String outFile) {
		this.eventsWriter = new EventWriterXML(outFile);
		this.driver2vehicleIdMap = new HashMap<Id, Id>();
	}



	public static void main(String[] args) {
		String inFile = args[0];
		String outFile = args[1];
		
		log.info("handling events...");
		EventsManager manager = EventsUtils.createEventsManager();

		EventsEnricher eR = new EventsEnricher(outFile);
		
		manager.addHandler(eR);
		new MatsimEventsReader(manager).readFile(inFile);
		log.info("event-handling finished...");
		
		eR.finish();
		
	}

	private void finish() {
		this.eventsWriter.closeFile();
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
	}

	public void handleTransitDriverStartsEvent(TransitDriverStartsEvent event) {
		this.driver2vehicleIdMap.put(event.getDriverId(), event.getVehicleId());
		this.eventsWriter.handleEvent(event);
	}

	public void handleLinkLeaveEvent(LinkLeaveEvent event) {
		Id vehId = this.driver2vehicleIdMap.get(event.getDriverId());
		if (vehId == null) {
			// private car
			vehId = event.getDriverId();
		}
		
		LinkLeaveEvent newEvent = new LinkLeaveEvent(event.getTime(), vehId, event.getLinkId());
		this.eventsWriter.handleEvent(newEvent);
	}

	public void handleLinkEnterEvent(LinkEnterEvent event) {
		Id vehId = this.driver2vehicleIdMap.get(event.getDriverId());
		if (vehId == null) {
			// private car
			vehId = event.getDriverId();
		}
		LinkEnterEvent newEvent = new LinkEnterEvent(event.getTime(), vehId, event.getLinkId());
		this.eventsWriter.handleEvent(newEvent);
	}

	@Override
	public void handleEvent(Event event) {
		if (event instanceof LinkLeaveEvent) {
			this.handleLinkLeaveEvent((LinkLeaveEvent) event);
		} else if (event instanceof LinkEnterEvent) {
			this.handleLinkEnterEvent((LinkEnterEvent) event);
		} else if (event instanceof TransitDriverStartsEvent) {
			this.handleTransitDriverStartsEvent((TransitDriverStartsEvent) event);
		} else {
			this.eventsWriter.handleEvent(event);
		}
	}
}
