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

package org.matsim.contrib.drt.passenger.events;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.DvrpPassengerEventsReader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import java.util.Map;
import java.util.Stack;

public final class DrtPassengerEventsReader extends MatsimXmlParser {

    private DvrpPassengerEventsReader delegate;

    public DrtPassengerEventsReader(EventsManager events) {
        delegate = new DvrpPassengerEventsReader(events);
        this.setValidating(false);
        delegate.addCustomEventMapper(org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent.EVENT_TYPE, getDrtRequestSubmittedEventMapper());
    }

    private MatsimEventsReader.CustomEventMapper<org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent> getDrtRequestSubmittedEventMapper() {
        return event -> {

            Map<String, String> attributes = event.getAttributes();

            double time = Double.parseDouble(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_TIME));
			String mode = attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_MODE);
			Id<Request> requestId = Id.create(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_REQUEST), Request.class);
			Id<Person> personId = Id.createPersonId(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_PERSON));
			Id<Link> fromLinkId = Id.createLinkId(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_FROM_LINK));
			Id<Link> toLinkId = Id.createLinkId(attributes.get(DrtRequestSubmittedEvent.ATTRIBUTE_TO_LINK));

			double unsharedRideTime = Double.parseDouble(attributes.get(org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent.ATTRIBUTE_UNSHARED_RIDE_TIME));
			double unsharedRideDistance = Double.parseDouble(attributes.get(org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent.ATTRIBUTE_UNSHARED_RIDE_DISTANCE));

			return new org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent(time, mode, requestId, personId, fromLinkId, toLinkId, unsharedRideTime, unsharedRideDistance);
        };
    }

	public void characters(char[] ch, int start, int length) throws SAXException {
		delegate.characters(ch, start, length);
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
