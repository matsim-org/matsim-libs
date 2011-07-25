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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsFactoryImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author benjamin
 *
 */
public class EmissionEventsReader extends MatsimXmlParser{
	private static final Logger logger = Logger.getLogger(EmissionEventsReader.class);

	private static final String EVENT = "event";

	private final EventsManager eventsManager;
	private final EventsFactoryImpl builder;

	public EmissionEventsReader(EventsManager events) {
		super();
		this.eventsManager = events;
		this.builder = (EventsFactoryImpl) events.getFactory();
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
		String hotEventType = HotEmissionEventImpl.EVENT_TYPE;
		String coldEventType = ColdEmissionEventImpl.EVENT_TYPE;

		Double time = 0.0;
		Id linkId = null;
		Id vehicleId = null;
		Map<String, Double> hotEmissions = new HashMap<String, Double>();
		Map<String, Double> coldEmissions = new HashMap<String, Double>();

		if(hotEventType.equals(eventType) || coldEventType.equals(eventType)){
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getValue(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));				
				}
				else if(attributes.getValue(i).equals("linkId")){
					linkId = new IdImpl((attributes.getValue(i)));
				}
				else if(attributes.getValue(i).equals("vehicleId")){
					vehicleId = new IdImpl((attributes.getValue(i)));
				}
				else{
					String pollutant = attributes.getQName(i);
					Double value = Double.parseDouble(attributes.getValue(i));
					hotEmissions.put(pollutant, value);
				}

				if(hotEventType.equals(eventType)){
					this.eventsManager.processEvent(new HotEmissionEventImpl(
							time,
							linkId,
							vehicleId,
							hotEmissions
					));
				}
				if(coldEventType.equals(eventType)){
					this.eventsManager.processEvent(new HotEmissionEventImpl(
							time,
							linkId,
							vehicleId,
							coldEmissions
					));
				}
			}
		}
		else{
			logger.warn("You are trying to read a non emission events file. For reading these, please use EventsReaderXMLv1.");
			throw new RuntimeException();
		}
	}
}
