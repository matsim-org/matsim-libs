/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsWriter.java
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

package org.matsim.core.network.io;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimSomeWriter;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;

import java.io.IOException;
import java.util.Collection;


/**
 * @author illenberger
 *
 */
public final class NetworkChangeEventsWriter extends MatsimXmlWriter implements MatsimSomeWriter {
	// yy uses syntax 
	//   write( filename, container ) 
	// rather than the matsim standard
	//   ctor( container ) 
	//   write( filename )
	// kai, apr'10
	
	private static final Logger log = LogManager.getLogger(NetworkChangeEventsWriter.class);
	
	private static final String TAB = "\t";
	
	private static final String WHITESPACE = " ";
	
	private static final String OPEN_TAG_1 = "<";
	
	private static final String OPEN_TAG_2 = "</";
	
	private static final String CLOSE_TAG_1 = ">";
	
	private static final String CLOSE_TAG_2 = "/>";
	
	private static final String QUOTE = "\"";
	
	private static final String EQUALS = "=";
	
	// DEFAULT_DTD_LOCATION does not work for _some_ reason...
	private static final String DTD_LOCATION = "http://www.matsim.org/files/dtd";
	
	private static final String W3_URL = "http://www.w3.org/2001/XMLSchema-instance";
	
	private static final String XSD_LOCATION = "http://www.matsim.org/files/dtd/networkChangeEvents.xsd";

	public void write(String file, Collection<NetworkChangeEvent> events) {
		log.info("Writing network change events to file: " + file  + "...");
		try {
			openFile(file);
			super.writeXmlHead();
			
			this.writer.write(OPEN_TAG_1);
			this.writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENTS_TAG);
			
			this.writer.write(WHITESPACE);
			this.writer.write("xmlns");
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(DTD_LOCATION);
			this.writer.write(QUOTE);
			
			this.writer.write(WHITESPACE);
			this.writer.write("xmlns:xsi");
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(W3_URL);
			this.writer.write(QUOTE);
			
			this.writer.write(WHITESPACE);
			this.writer.write("xsi:schemaLocation");
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(DTD_LOCATION);
			this.writer.write(WHITESPACE);
			this.writer.write(XSD_LOCATION);
			this.writer.write(QUOTE);
			this.writer.write(CLOSE_TAG_1);
			this.writer.write(NL);
			this.writer.write(NL);
		
			for (NetworkChangeEvent event : events) {
				writeEvent(event);
				this.writer.write(NL);
				this.writer.write(NL);
			}
			
			this.writer.write(OPEN_TAG_2);
			this.writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENTS_TAG);
			this.writer.write(CLOSE_TAG_1);
			this.writer.write(NL);
			
			close();
		} catch (IOException e) {
			log.fatal("Error during writing network change events!", e);
		}
		log.info("done.");
	}
	
	private void writeEvent(NetworkChangeEvent event) throws IOException {
//		if ( event.getLinks().isEmpty() ) {
//			return ;
//			// yyyy is this a problem?  There is otherwise this condition (*) below ?? 
		// or is it a problem to just write the empty change event?  kai, nov'17
//		}
		this.writer.write(TAB);
		this.writer.write(OPEN_TAG_1);
		this.writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENT_TAG);
		this.writer.write(WHITESPACE);
		this.writer.write(NetworkChangeEventsParser.START_TIME_TAG);
		this.writer.write(EQUALS);
		this.writer.write(QUOTE);
		this.writer.write(Time.writeTime(event.getStartTime()));
		this.writer.write(QUOTE);
		this.writer.write(CLOSE_TAG_1);
		this.writer.write(NL);

		if (event.getLinks().isEmpty()) {
			throw new IllegalArgumentException("NetworkChangeEvent must contain at least one link.");
			// (*)
		}
		for(Link link : event.getLinks()) {
			this.writer.write(TAB);
			this.writer.write(TAB);
			this.writer.write(OPEN_TAG_1);
			this.writer.write(NetworkChangeEventsParser.LINK_TAG);
			this.writer.write(WHITESPACE);
			this.writer.write(NetworkChangeEventsParser.REF_ID_TAG);
			this.writer.write(EQUALS);
			this.writer.write(QUOTE);
			this.writer.write(link.getId().toString());
			this.writer.write(QUOTE);
			this.writer.write(CLOSE_TAG_2);
			this.writer.write(NL);
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

		this.writer.write(TAB);
		this.writer.write(OPEN_TAG_2);
		this.writer.write(NetworkChangeEventsParser.NETWORK_CHANGE_EVENT_TAG);
		this.writer.write(CLOSE_TAG_1);
	}
	
	private void writeChangeValue(String attName, ChangeValue value) throws IOException {
		this.writer.write(TAB);
		this.writer.write(TAB);
		
		this.writer.write(OPEN_TAG_1);
		this.writer.write(attName);
		
		this.writer.write(WHITESPACE);
		this.writer.write(NetworkChangeEventsParser.CHANGE_TYPE_TAG);
		this.writer.write(EQUALS);
		this.writer.write(QUOTE);
		switch( value.getType() ) {
		case ABSOLUTE_IN_SI_UNITS:
			this.writer.write(NetworkChangeEventsParser.ABSOLUTE_VALUE);
			break;
		case FACTOR:
			this.writer.write(NetworkChangeEventsParser.FACTOR_VALUE);
			break;
		case OFFSET_IN_SI_UNITS:
			this.writer.write(NetworkChangeEventsParser.OFFSET_VALUE);
			break;
		default:
			throw new RuntimeException("missing ChangeType") ;
		}
		this.writer.write(QUOTE);
		
		this.writer.write(WHITESPACE);
		this.writer.write(NetworkChangeEventsParser.VALUE_TAG);
		this.writer.write(EQUALS);
		this.writer.write(QUOTE);
		this.writer.write(String.valueOf(value.getValue()));
		this.writer.write(QUOTE);
		
		this.writer.write(CLOSE_TAG_2);
		this.writer.write(NL);
	}
}
