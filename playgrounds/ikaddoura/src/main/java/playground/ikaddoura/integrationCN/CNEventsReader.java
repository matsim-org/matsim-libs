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
package playground.ikaddoura.integrationCN;

import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.noise.data.ReceiverPoint;
import org.matsim.contrib.noise.events.NoiseEventAffected;
import org.matsim.contrib.noise.events.NoiseEventCaused;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsReaderXMLv1.CustomEventMapper;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.vsp.congestion.events.CongestionEvent;

/**
 * An events reader which reads the default events and the additional custom events CongestionEvent, NoiseEventAffected, NoiseEventCaused.
 * 
 * @author ikaddoura
 *
 */
public class CNEventsReader extends MatsimXmlParser {
	
	EventsReaderXMLv1 delegate;

	public void characters(char[] ch, int start, int length) throws SAXException {
		delegate.characters(ch, start, length);
	}

	public CNEventsReader(EventsManager events) {
		delegate = new EventsReaderXMLv1(events);
		this.setValidating(false);
		
		CustomEventMapper<CongestionEvent> congestionEventMapper = new CustomEventMapper<CongestionEvent>() {
			
			@Override
			public CongestionEvent apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				Double time = Double.parseDouble(attributes.get(CongestionEvent.ATTRIBUTE_TIME));
				Id<Link> linkId = Id.createLinkId(attributes.get(CongestionEvent.ATTRIBUTE_LINK));
				Id<Person> causingAgentId = Id.createPersonId(attributes.get(CongestionEvent.ATTRIBUTE_PERSON));
				Id<Person> affectedAgentId = Id.createPersonId(attributes.get(CongestionEvent.ATTRIBUTE_AFFECTED_AGENT));
				Double delay = Double.parseDouble(attributes.get(CongestionEvent.ATTRIBUTE_DELAY));
				String constraint = attributes.get(CongestionEvent.EVENT_CAPACITY_CONSTRAINT);
				Double emergenceTime = Double.parseDouble(attributes.get(CongestionEvent.ATTRIBUTE_EMERGENCETIME));
			
				return new CongestionEvent(time, constraint, causingAgentId, affectedAgentId, delay, linkId, emergenceTime);
			}
		};
		
		delegate.addCustomEventMapper(CongestionEvent.EVENT_TYPE, congestionEventMapper);
		
		CustomEventMapper<NoiseEventAffected> noiseEventAffectedMapper = new CustomEventMapper<NoiseEventAffected>() {
			
			@Override
			public NoiseEventAffected apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				Double time = Double.parseDouble(attributes.get(NoiseEventAffected.ATTRIBUTE_TIME));
				Id<Person> affectedAgentId = Id.createPersonId(attributes.get(NoiseEventAffected.ATTRIBUTE_AGENT_ID));
				Double amount = Double.parseDouble(attributes.get(NoiseEventAffected.ATTRIBUTE_AMOUNT_DOUBLE));
				String activityType = attributes.get(NoiseEventAffected.ATTRIBUTE_ACTIVTITY_TYPE);
				Double emergenceTime = Double.parseDouble(attributes.get(NoiseEventAffected.ATTRIBUTE_EMERGENCE_TIME));
				Id<ReceiverPoint> receiverPointId = Id.create(attributes.get(NoiseEventAffected.ATTRIBUTE_RECEIVERPOINT_ID), ReceiverPoint.class);
			
				return new NoiseEventAffected(time, emergenceTime, affectedAgentId, amount, receiverPointId, activityType);
			}
		};
		
		delegate.addCustomEventMapper(NoiseEventAffected.EVENT_TYPE, noiseEventAffectedMapper);
		
		CustomEventMapper<NoiseEventCaused> noiseEventCausedMapper = new CustomEventMapper<NoiseEventCaused>() {
			
			@Override
			public NoiseEventCaused apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				Double time = Double.parseDouble(attributes.get(NoiseEventCaused.ATTRIBUTE_TIME));
				Double emergenceTime = Double.parseDouble(attributes.get(NoiseEventCaused.ATTRIBUTE_EMERGENCE_TIME));
				Id<Person> causingAgentId = Id.createPersonId(attributes.get(NoiseEventCaused.ATTRIBUTE_AGENT_ID));
				Id<Vehicle> causingVehicleId = Id.create(attributes.get(NoiseEventCaused.ATTRIBUTE_VEHICLE_ID), Vehicle.class);
				Double amount = Double.parseDouble(attributes.get(NoiseEventCaused.ATTRIBUTE_AMOUNT_DOUBLE));
				Id<Link> linkId = Id.createLinkId(attributes.get(NoiseEventCaused.ATTRIBUTE_LINK_ID));
			
				return new NoiseEventCaused(time, emergenceTime, causingAgentId, causingVehicleId, amount, linkId);
			}
		};
		
		delegate.addCustomEventMapper(NoiseEventCaused.EVENT_TYPE, noiseEventCausedMapper);
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
