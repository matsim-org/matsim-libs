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
package analysis.experiencedTrips;

import java.util.Map;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestRejectedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestScheduledEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsReaderXMLv1.CustomEventMapper;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * An events reader which reads the default events and the additional custom events CongestionEvent, NoiseEventAffected, NoiseEventCaused.
 * 
 * @author gleich
 *
 */
public class DrtEventsReader extends MatsimXmlParser {
	
	EventsReaderXMLv1 delegate;

	public void characters(char[] ch, int start, int length) throws SAXException {
		delegate.characters(ch, start, length);
	}

	public DrtEventsReader(EventsManager events) {
		delegate = new EventsReaderXMLv1(events);
		this.setValidating(false);
		
		CustomEventMapper<DrtRequestSubmittedEvent> drtRequestSubmittedMapper = new CustomEventMapper<DrtRequestSubmittedEvent>() {
			
			@Override
			public DrtRequestSubmittedEvent apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				Double time = Double.parseDouble(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_TIME));
				Id<Request> requestId = Id.create(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_REQUEST), Request.class);
				Id<Person> personId = Id.createPersonId(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_PERSON));
				Id<Link> fromLinkId = Id.createLinkId(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_FROM_LINK));
				Id<Link> toLinkId = Id.createLinkId(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_TO_LINK));
				Double unsharedRideTime = Double.parseDouble(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_UNSHARED_RIDE_TIME));
				Double unsharedRideDistance = Double.parseDouble(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_UNSHARED_RIDE_DISTANCE));
				
				return new DrtRequestSubmittedEvent(time, requestId, personId, fromLinkId,
						toLinkId, unsharedRideTime, unsharedRideDistance);
			}
		};
		
		delegate.addCustomEventMapper(DrtRequestSubmittedEvent.EVENT_TYPE, drtRequestSubmittedMapper);
		
		CustomEventMapper<DrtRequestRejectedEvent> drtRequestRejectedMapper = new CustomEventMapper<DrtRequestRejectedEvent>() {
			
			@Override
			public DrtRequestRejectedEvent apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				Double time = Double.parseDouble(attributes.get(DrtRequestRejectedEvent.ATTRIBUTE_TIME));
				Id<Request> requestId = Id.create(attributes.get(DrtRequestRejectedEvent.ATTRIBUTE_REQUEST), Request.class);
				String cause = attributes.get(DrtRequestRejectedEvent.ATTRIBUTE_CAUSE);

				return new DrtRequestRejectedEvent(time, requestId, cause);
			}
		};
		
		delegate.addCustomEventMapper(DrtRequestRejectedEvent.EVENT_TYPE, drtRequestRejectedMapper);
		
		CustomEventMapper<DrtRequestScheduledEvent> drtRequestScheduledMapper = new CustomEventMapper<DrtRequestScheduledEvent>() {
			
			@Override
			public DrtRequestScheduledEvent apply(GenericEvent event) {
				
				Map<String, String> attributes = event.getAttributes();
				
				double time = Double.parseDouble(attributes.get(DrtRequestScheduledEvent.ATTRIBUTE_TIME));
				Id<Request> requestId = Id.create(attributes.get(DrtRequestScheduledEvent.ATTRIBUTE_REQUEST), Request.class);
				Id<Vehicle> vehicleId = Id.create(attributes.get(DrtRequestScheduledEvent.ATTRIBUTE_VEHICLE), Vehicle.class);
				double pickUpTime = Double.parseDouble(attributes.get(DrtRequestScheduledEvent.ATTRIBUTE_PICKUP_TIME));
				double dropOffDistance = Double.parseDouble(attributes.get(DrtRequestScheduledEvent.ATTRIBUTE_DROPOFF_TIME));
				
				return new DrtRequestScheduledEvent(time, requestId, vehicleId, pickUpTime, dropOffDistance);
			}
		};
		
		delegate.addCustomEventMapper(DrtRequestScheduledEvent.EVENT_TYPE, drtRequestScheduledMapper);
		
	
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
