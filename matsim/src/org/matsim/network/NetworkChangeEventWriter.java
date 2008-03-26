/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventWriter.java
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

/**
 * 
 */
package org.matsim.network;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.network.NetworkChangeEvent.ChangeType;
import org.matsim.network.NetworkChangeEvent.ChangeValue;
import org.matsim.utils.misc.Time;
import org.matsim.writer.MatsimXmlWriter;


/**
 * @author illenberger
 *
 */
public class NetworkChangeEventWriter extends MatsimXmlWriter {
	
	private static final Logger log = Logger.getLogger(NetworkChangeEventWriter.class);
	
	public static final String TAB = "\t";
	
	public static final String WHITESPACE = " ";
	
	public static final String OPEN_TAG_1 = "<";
	
	public static final String OPEN_TAG_2 = "</";
	
	public static final String CLOSE_TAG_1 = ">";
	
	public static final String CLOSE_TAG_2 = "/>";
	
	public static final String QUOTE = "\"";
	
	public static final String EQUALS = "=";
	
	// DEFAULT_DTD_LOCATION does not work for _some_ reason...
	public static final String DTD_LOCATION = "http://www.matsim.org/files/dtd";
	
	public static final String W3_URL = "http://www.w3.org/2001/XMLSchema-instance";
	
	public static final String XSD_LOCATION = "http://www.matsim.org/files/dtd/networkChangeEvents.xsd";

	public void write(String file, List<NetworkChangeEvent> events) {
		try {
			openFile(file);
			super.writeXmlHead();
			
			writer.write(OPEN_TAG_1);
			writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENTS_TAG);
			
			writer.write(WHITESPACE);
			writer.write("xmlns");
			writer.write(EQUALS);
			writer.write(QUOTE);
			writer.write(DTD_LOCATION);
			writer.write(QUOTE);
			
			writer.write(WHITESPACE);
			writer.write("xmlns:xsi");
			writer.write(EQUALS);
			writer.write(QUOTE);
			writer.write(W3_URL);
			writer.write(QUOTE);
			
			writer.write(WHITESPACE);
			writer.write("xsi:schemaLocation");
			writer.write(EQUALS);
			writer.write(QUOTE);
			writer.write(DTD_LOCATION);
			writer.write(WHITESPACE);
			writer.write(XSD_LOCATION);
			writer.write(QUOTE);
			writer.write(CLOSE_TAG_1);
			writer.write(NL);
			writer.write(NL);
		
			for (NetworkChangeEvent event : events) {
				writeEvent(event);
				writer.write(NL);
				writer.write(NL);
			}
			
			writer.write(OPEN_TAG_2);
			writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENTS_TAG);
			writer.write(CLOSE_TAG_1);
			writer.write(NL);
			
			close();
		} catch (IOException e) {
			log.fatal("Error during writing network change events!", e);
		}
	}
	
	private void writeEvent(NetworkChangeEvent event) throws IOException {
		writer.write(TAB);
		writer.write(OPEN_TAG_1);
		writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENT_TAG);
		writer.write(WHITESPACE);
		writer.write(NetworkChangeEventsParser.START_TIME_TAG);
		writer.write(EQUALS);
		writer.write(QUOTE);
		writer.write(Time.writeTime(event.getStartTime()));
		writer.write(QUOTE);
		if(event.getEndTime() >= 0) {
			writer.write(WHITESPACE);
			writer.write(NetworkChangeEventsParser.END_TIME_TAG);
			writer.write(EQUALS);
			writer.write(QUOTE);
			writer.write(Time.writeTime(event.getEndTime()));
			writer.write(QUOTE);
		}
		writer.write(CLOSE_TAG_1);
		writer.write(NL);
		
		for(Link link : event.getLinks()) {
			writer.write(TAB);
			writer.write(TAB);
			writer.write(OPEN_TAG_1);
			writer.write(NetworkChangeEventsParser.LINK_TAG);
			writer.write(WHITESPACE);
			writer.write(NetworkChangeEventsParser.REF_ID_TAG);
			writer.write(EQUALS);
			writer.write(QUOTE);
			writer.write(link.getId().toString());
			writer.write(QUOTE);
			writer.write(CLOSE_TAG_2);
			writer.write(NL);
		}
		
		if(event.getFlowCapacityChange() != null) {
			writeChangeValue(NetworkChangeEventsParser.FLOW_CAPACITY_TAG, event.getFlowCapacityChange());
		}

		if(event.getFreespeedChange() != null) {
			writeChangeValue(NetworkChangeEventsParser.FREESPEED_TAG, event.getFreespeedChange());
		}

		if(event.getLanesChange() != null) {
			writeChangeValue(NetworkChangeEventsParser.LANES_TAG, event.getLanesChange());
		}

		writer.write(TAB);
		writer.write(OPEN_TAG_2);
		writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENT_TAG);
		writer.write(CLOSE_TAG_1);
	}
	
	private void writeChangeValue(String attName, ChangeValue value) throws IOException {
		writer.write(TAB);
		writer.write(TAB);
		
		writer.write(OPEN_TAG_1);
		writer.write(attName);
		
		writer.write(WHITESPACE);
		writer.write(NetworkChangeEventsParser.CHANGE_TYPE_TAG);
		writer.write(EQUALS);
		writer.write(QUOTE);
		if(value.getType() == ChangeType.ABSOLUTE) {
			writer.write(NetworkChangeEventsParser.ABSOLUTE_VALUE);
		} else if(value.getType() == ChangeType.FACTOR) {
			writer.write(NetworkChangeEventsParser.FACTOR_VALUE);
		}
		writer.write(QUOTE);
		
		writer.write(WHITESPACE);
		writer.write(NetworkChangeEventsParser.VALUE_TAG);
		writer.write(EQUALS);
		writer.write(QUOTE);
		writer.write(String.valueOf(value.getValue()));
		writer.write(QUOTE);
		
		writer.write(CLOSE_TAG_2);
		writer.write(NL);
	}
}
