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
package org.matsim.contrib.noise.events;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author ihab
 *
 */
public class NoiseEventsReader extends MatsimXmlParser{

	private static final String EVENT = "event";

	private final EventsManager eventsManager;

	public NoiseEventsReader(EventsManager events) {
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

		if (NoiseEventCaused.EVENT_TYPE.equals(eventType)){
			Double time = 0.0;
			Double emergenceTime = 0.0;
			Id<Person> causingAgentId = null;
			Id<Vehicle> causingVehicleId = null;
			Double amount = 0.0;
			Id<Link> linkId = null;
			
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_EMERGENCE_TIME)){
					emergenceTime = Double.parseDouble(attributes.getValue(i));
				}	
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_AGENT_ID)){
					causingAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_VEHICLE_ID)){
					causingVehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_AMOUNT_DOUBLE)){
					amount = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_LINK_ID)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				}
				
//				else {
//					throw new RuntimeException("Unknown event attribute. Aborting... " + attributes.getQName(i));
//				}
			}
			this.eventsManager.processEvent(new NoiseEventCaused(time, emergenceTime, causingAgentId, causingVehicleId, amount, linkId));
		}
		
		else if (NoiseEventAffected.EVENT_TYPE.equals(eventType)){
			Double time = 0.0;
			Double emergenceTime = 0.0;
			Id<Person> affectedAgentId = null;
			Double amount = 0.0;
			Id<ReceiverPoint> receiverPointId = null;
			String activityType = null;
			
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_EMERGENCE_TIME)){
					emergenceTime = Double.parseDouble(attributes.getValue(i));
				}	
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_AGENT_ID)){
					affectedAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_ACTIVTITY_TYPE)){
					activityType = (attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_AMOUNT_DOUBLE)){
					amount = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_RECEIVERPOINT_ID)){
					receiverPointId = Id.create((attributes.getValue(i)), ReceiverPoint.class);
				}
				else {
					throw new RuntimeException("Unknown event attribute. Aborting... " + attributes.getQName(i));
				}
			}
			this.eventsManager.processEvent(new NoiseEventAffected(time, emergenceTime, affectedAgentId, amount, receiverPointId, activityType));
		}
	}
}
