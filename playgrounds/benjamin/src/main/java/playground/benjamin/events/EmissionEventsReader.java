/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventsReader.java
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
package playground.benjamin.events;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.xml.sax.Attributes;

/**
 * @author benjamin
 *
 */
public class EmissionEventsReader extends EventsReaderXMLv1{

	public EmissionEventsReader(EventsManager events) {
		super(events);
	}

	private void startEvent(final Attributes attributes){
		double time = Double.parseDouble(attributes.getValue("time"));
		String eventType = attributes.getValue("type");
		
		if(HotEmissionEventImpl.EVENT_TYPE.equals(eventType)){
			
		}
	}
}
