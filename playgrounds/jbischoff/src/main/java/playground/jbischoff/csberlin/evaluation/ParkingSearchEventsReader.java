/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventsReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.jbischoff.csberlin.evaluation;

import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsReaderXMLv1.CustomEventMapper;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * An events reader which reads the default events and the additional custom events CongestionEvent, NoiseEventAffected, NoiseEventCaused.
 * 
 * @author ikaddoura
 *
 */
public class ParkingSearchEventsReader extends MatsimXmlParser {
	
	EventsReaderXMLv1 delegate;

	public void characters(char[] ch, int start, int length) throws SAXException {
		delegate.characters(ch, start, length);
	}

	public ParkingSearchEventsReader(EventsManager events) {
		delegate = new EventsReaderXMLv1(events);
		this.setValidating(false);
		
		CustomEventMapper<StartParkingSearchEvent> parkingSearchMapper = new CustomEventMapper<StartParkingSearchEvent>() {
			
			@Override
			public StartParkingSearchEvent apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				Double time = Double.parseDouble(attributes.get(StartParkingSearchEvent.ATTRIBUTE_TIME));
				Id<Link> linkId = Id.createLinkId(attributes.get(StartParkingSearchEvent.ATTRIBUTE_LINK));
				Id<Vehicle> vid = Id.createVehicleId(attributes.get(StartParkingSearchEvent.ATTRIBUTE_VEHICLE));
				
				return new StartParkingSearchEvent(time, vid, linkId);
			}
		};
		
		delegate.addCustomEventMapper(StartParkingSearchEvent.EVENT_TYPE, parkingSearchMapper);
		
	
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		delegate.startTag(name, atts, context);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		delegate.endTag(name, content, context);
	}
	
}
