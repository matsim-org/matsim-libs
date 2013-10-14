/* *********************************************************************** *
 * project: org.matsim.*
 * EventsWriterXMLInstance.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.christoph.events.implementations;

import java.util.Map;
import java.util.Queue;

import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.handler.BasicEventHandler;

import playground.christoph.events.EventHandlerInstance;

/**
 * @author mrieser
 * @author cdobler
 */
public class EventsWriterXMLInstance implements EventHandlerInstance, BasicEventHandler {

	private final int arrayLength = 1024;
	
	private final Queue<String[]> queues;
	private String[] array;
	private int position;
	
	public EventsWriterXMLInstance(Queue<String[]> queues) {
		this.queues = queues;
		this.array = new String[this.arrayLength];
	}

	/* Implementation of EventHandler-Interfaces */
	@Override
	public void handleEvent(final Event event) {
		if (position == this.arrayLength) this.flush();
		
		StringBuilder eventXML = new StringBuilder(180);
		eventXML.append("\t<event ");
		Map<String, String> attr = event.getAttributes();
		for (Map.Entry<String, String> entry : attr.entrySet()) {
			eventXML.append(entry.getKey());
			eventXML.append("=\"");
			eventXML.append(encodeAttributeValue(entry.getValue()));
			eventXML.append("\" ");
		}
		eventXML.append(" />\n");
		array[position++] = eventXML.toString();
	}

	// the following method was taken from MatsimXmlWriter in order to correctly encode attributes, but
	// to forego the overhead of using the full MatsimXmlWriter.
	/**
	 * Encodes the given string in such a way that it no longer contains
	 * characters that have a special meaning in xml.
	 * 
	 * @see <a href="http://www.w3.org/International/questions/qa-escapes#use">http://www.w3.org/International/questions/qa-escapes#use</a>
	 * @param attributeValue
	 * @return String with some characters replaced by their xml-encoding.
	 */
	private String encodeAttributeValue(final String attributeValue) {
		if (attributeValue == null) {
			return null;
		}
		if (attributeValue.contains("&") || attributeValue.contains("\"") || attributeValue.contains("<") || attributeValue.contains(">")) {
			return attributeValue.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");
		}
		return attributeValue;
	}
	
	@Override
	public void synchronize(double time) {
		flush();
	}

	@Override
	public void reset(int iteration) {
		this.array = new String[1024];
	}

	private void flush() {
		this.queues.add(array);
		this.array = new String[this.arrayLength];
		this.position = 0;
	}
}
