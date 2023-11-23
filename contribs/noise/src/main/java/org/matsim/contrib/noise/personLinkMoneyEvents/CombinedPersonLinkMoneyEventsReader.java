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

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * @author ikaddoura
 *
 */
public class CombinedPersonLinkMoneyEventsReader extends MatsimXmlParser {

	private final EventsManager eventsManager;
	private MatsimXmlParser defaultEventsReader;
	private PersonLinkMoneyEventsReader personLinkMoneyEventsReader;

	public CombinedPersonLinkMoneyEventsReader(EventsManager eventsManager) {
		super(ValidationType.NO_VALIDATION);

		this.eventsManager = eventsManager;
		setValidating(false);

		defaultEventsReader = new EventsReaderXMLv1(this.eventsManager);
		personLinkMoneyEventsReader = new PersonLinkMoneyEventsReader(this.eventsManager);
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		this.defaultEventsReader.startTag(name, atts, context);
		this.personLinkMoneyEventsReader.startTag(name, atts, context);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		this.defaultEventsReader.endTag(name, content, context);
		this.personLinkMoneyEventsReader.endTag(name, content, context);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		this.defaultEventsReader.characters(ch, start, length);
		this.personLinkMoneyEventsReader.characters(ch, start, length);
	}
}
