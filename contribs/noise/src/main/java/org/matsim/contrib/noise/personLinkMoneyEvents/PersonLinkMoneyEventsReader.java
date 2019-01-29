/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventsReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
package org.matsim.contrib.noise.personLinkMoneyEvents;

import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author ikaddoura
 *
 */
public class PersonLinkMoneyEventsReader extends MatsimXmlParser{

	private static final String EVENT = "event";

	private final EventsManager eventsManager;

	public PersonLinkMoneyEventsReader(EventsManager events) {
		super();
		this.eventsManager = events;
		setValidating(false);
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
		
	}

	private void startEvent(final Attributes attributes){

		String eventType = attributes.getValue("type");

		if (PersonLinkMoneyEvent.EVENT_TYPE.equals(eventType)){
			Double time = 0.0;
			Id<Person> personId = null;
			Id<Link> linkId = null;
			Double amount = 0.0;
			Double relevantTime = 0.0;
			String description = "";
			
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				} else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				} else if(attributes.getQName(i).equals(PersonLinkMoneyEvent.ATTRIBUTE_LINK)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				} else if(attributes.getQName(i).equals(PersonLinkMoneyEvent.ATTRIBUTE_PERSON)){
					personId = Id.create((attributes.getValue(i)), Person.class);
				} else if(attributes.getQName(i).equals(PersonLinkMoneyEvent.ATTRIBUTE_AMOUNT)){
					amount = Double.parseDouble(attributes.getValue(i));
				} else if(attributes.getQName(i).equals(PersonLinkMoneyEvent.ATTRIBUTE_RELEVANT_TIME)){
					relevantTime = Double.parseDouble(attributes.getValue(i));
				} else if(attributes.getQName(i).equals(PersonLinkMoneyEvent.ATTRIBUTE_DESCRIPTION)){
					description = attributes.getValue(i);
				}
			}
			this.eventsManager.processEvent(new PersonLinkMoneyEvent(time, personId, linkId, amount, relevantTime, description));
		}
	}
}
