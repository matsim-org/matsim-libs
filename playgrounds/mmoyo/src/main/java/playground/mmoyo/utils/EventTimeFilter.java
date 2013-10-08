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

package playground.mmoyo.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.events.handler.EventHandler;

class EventTimeFilter  implements BasicEventHandler{
	private final static Logger log = Logger.getLogger(EventTimeFilter.class);
	
	@Override
	public void handleEvent(Event event)  {
		String strPerson = event.getAttributes().get("person");
		if(strPerson!=null && (event.getTime()> 86399) ){
			System.out.println(event.toString());
		}
	}

	@Override
	public void reset(int iteration) {
		
	}
	
	public static void main(String[] args) {
		String inputEventFile = "../../";
		
		//read and filter out events
		EventTimeFilter eventScanner = new EventTimeFilter(); 
		EventsManager eventsManager = (EventsManager) EventsUtils.createEventsManager();
		eventsManager.addHandler((EventHandler) eventScanner);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.readFile(inputEventFile);
		log.info("Events file read");
	}

}
