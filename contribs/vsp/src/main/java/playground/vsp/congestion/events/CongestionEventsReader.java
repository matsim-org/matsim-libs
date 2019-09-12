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
package playground.vsp.congestion.events;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author ihab
 *
 */
public class CongestionEventsReader extends MatsimXmlParser{

	private static final String EVENT = "event";

	private final EventsManager eventsManager;

	public CongestionEventsReader(EventsManager events) {
		super();
		this.eventsManager = events;
		setValidating(false); // events-files have no DTD, thus they cannot validate
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (EVENT.equals(name)) {
			startEvent(atts);
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		// ignore characters to prevent OutOfMemoryExceptions
		/* the events-file only contains empty tags with attributes,
		 * but without the dtd or schema, all whitespace between tags is handled
		 * by characters and added up by super.characters, consuming huge
		 * amount of memory when large events-files are read in.
		 */
	}

	private void startEvent(final Attributes attributes){

		String eventType = attributes.getValue("type");

		Double time = 0.0;
		Id<Link> linkId = null;
		Id<Person> causingAgentId = null;
		Id<Person> affectedAgentId = null;
		Double delay = 0.0;
		String constraint = null;
		Double emergenceTime = 0.0;

		if(CongestionEvent.EVENT_TYPE.equals(eventType)){
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_LINK)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_PERSON)){
					causingAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_AFFECTED_AGENT)){
					affectedAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_DELAY)){
					delay = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(CongestionEvent.EVENT_CAPACITY_CONSTRAINT)){
					constraint = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_EMERGENCETIME)){
					emergenceTime = Double.parseDouble(attributes.getValue(i));
				}				
				else {
					throw new RuntimeException("Unknown event attribute. Aborting...");
				}
			}
			this.eventsManager.processEvent(new CongestionEvent(time, constraint, causingAgentId, affectedAgentId, delay, linkId, emergenceTime));
		}
		
		else {
			// other event type
		}
	}
}
