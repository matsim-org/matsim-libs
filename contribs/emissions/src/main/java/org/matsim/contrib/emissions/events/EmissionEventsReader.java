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
package org.matsim.contrib.emissions.events;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.ColdPollutant;
import org.matsim.contrib.emissions.types.WarmPollutant;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
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

	public EmissionEventsReader(EventsManager events) {
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
		Id<Vehicle> vehicleId = null;
		Map<WarmPollutant, Double> warmEmissions = new HashMap<>();
		Map<ColdPollutant, Double> coldEmissions = new HashMap<>();

		if(WarmEmissionEvent.EVENT_TYPE.equals(eventType)){
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(WarmEmissionEvent.ATTRIBUTE_LINK_ID)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				}
				else if(attributes.getQName(i).equals(WarmEmissionEvent.ATTRIBUTE_VEHICLE_ID)){
					vehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				}
				else {
					WarmPollutant pollutant = WarmPollutant.valueOf(attributes.getQName(i));
					Double value = Double.parseDouble(attributes.getValue(i));
					warmEmissions.put(pollutant, value);
				}
			}
			this.eventsManager.processEvent(new WarmEmissionEvent(
					time,
					linkId,
					vehicleId,
					warmEmissions
			));
		}
		else if (ColdEmissionEvent.EVENT_TYPE.equals(eventType)){
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(ColdEmissionEvent.ATTRIBUTE_LINK_ID)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				}
				else if(attributes.getQName(i).equals(ColdEmissionEvent.ATTRIBUTE_VEHICLE_ID)){
					vehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				}
				else {
					ColdPollutant pollutant = ColdPollutant.valueOf(attributes.getQName(i));
					Double value = Double.parseDouble(attributes.getValue(i));
					coldEmissions.put(pollutant, value);
				}
			}
			this.eventsManager.processEvent(new ColdEmissionEvent(
					time,
					linkId,
					vehicleId,
					coldEmissions
			));
		}
	}
}
