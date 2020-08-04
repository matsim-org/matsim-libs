/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.passenger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.Stack;

public class DvrpPassengerEventsReader extends MatsimXmlParser {

    private EventsReaderXMLv1 delegate;

    public DvrpPassengerEventsReader(EventsManager events) {
        delegate = new EventsReaderXMLv1(events);
        this.setValidating(false);
        delegate.addCustomEventMapper(PassengerRequestRejectedEvent.EVENT_TYPE, getPassengerRequestRejectedEventMapper());
        delegate.addCustomEventMapper(PassengerRequestScheduledEvent.EVENT_TYPE, getPassengerRequestScheduledEventMapper());
        delegate.addCustomEventMapper(PassengerRequestSubmittedEvent.EVENT_TYPE, getPassengerRequestSubmittedEventMapper());
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        delegate.characters(ch, start, length);
    }

    private MatsimEventsReader.CustomEventMapper<PassengerRequestRejectedEvent> getPassengerRequestRejectedEventMapper() {
        return event -> {

            Map<String, String> attributes = event.getAttributes();

            double time = Double.parseDouble(attributes.get(PassengerRequestRejectedEvent.ATTRIBUTE_TIME));
			String mode = attributes.get(PassengerRequestRejectedEvent.ATTRIBUTE_MODE);
			Id<Request> requestId = Id.create(attributes.get(PassengerRequestRejectedEvent.ATTRIBUTE_REQUEST), Request.class);
			Id<Person> personId = Id.createPersonId(attributes.get(PassengerRequestRejectedEvent.ATTRIBUTE_PERSON));
			String cause = attributes.get(PassengerRequestRejectedEvent.ATTRIBUTE_CAUSE);

            return new PassengerRequestRejectedEvent(time, mode, requestId, personId, cause);
        };
    }

    private MatsimEventsReader.CustomEventMapper<PassengerRequestScheduledEvent> getPassengerRequestScheduledEventMapper() {
        return event -> {

            Map<String, String> attributes = event.getAttributes();

            double time = Double.parseDouble(attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_TIME));
			String mode = attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_MODE);
			Id<Request> requestId = Id.create(attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_REQUEST), Request.class);
			Id<Person> personId = Id.createPersonId(attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_PERSON));
			Id<DvrpVehicle> vehicleId = Id.create(attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_VEHICLE), DvrpVehicle.class);
			double pickupTime = Double.parseDouble(attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_PICKUP_TIME));
            double dropoffTime = Double.parseDouble(attributes.get(PassengerRequestScheduledEvent.ATTRIBUTE_DROPOFF_TIME));

            return new PassengerRequestScheduledEvent(time, mode, requestId, personId, vehicleId, pickupTime, dropoffTime);
        };
    }

    private MatsimEventsReader.CustomEventMapper<PassengerRequestSubmittedEvent> getPassengerRequestSubmittedEventMapper() {
        return event -> {

            Map<String, String> attributes = event.getAttributes();

            double time = Double.parseDouble(attributes.get(PassengerRequestSubmittedEvent.ATTRIBUTE_TIME));
			String mode = attributes.get(PassengerRequestSubmittedEvent.ATTRIBUTE_MODE);
			Id<Request> requestId = Id.create(attributes.get(PassengerRequestSubmittedEvent.ATTRIBUTE_REQUEST), Request.class);
			Id<Person> personId = Id.createPersonId(attributes.get(PassengerRequestSubmittedEvent.ATTRIBUTE_PERSON));
			Id<Link> fromLinkId = Id.createLinkId(attributes.get(PassengerRequestSubmittedEvent.ATTRIBUTE_FROM_LINK));
			Id<Link> toLinkId = Id.createLinkId(attributes.get(PassengerRequestSubmittedEvent.ATTRIBUTE_TO_LINK));

			return new PassengerRequestSubmittedEvent(time, mode, requestId, personId, fromLinkId, toLinkId);
        };
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {
        delegate.startTag(name, atts, context);
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
        delegate.endTag(name, content, context);
    }

	public void addCustomEventMapper(String eventType, MatsimEventsReader.CustomEventMapper eventMapper) {
    	delegate.addCustomEventMapper(eventType,eventMapper);
	}
}
