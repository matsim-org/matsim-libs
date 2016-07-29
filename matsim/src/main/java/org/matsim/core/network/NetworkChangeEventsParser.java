/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkChangeEventsParser.java
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
package org.matsim.core.network;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.core.utils.misc.Time;
import org.xml.sax.Attributes;


/**
 * A parser building a list of {@link NetworkChangeEvent} out of a xml file.
 * 
 * @author illenberger
 * 
 */
public final class NetworkChangeEventsParser extends MatsimXmlParser {
	
	// ========================================================================
	// static fields
	// ========================================================================
	
	private static final Logger log = Logger.getLogger(NetworkChangeEventsParser.class);
	
	static final String NETWORK_CHANGE_EVENTS_TAG = "networkChangeEvents";
	
	static final String NETWORK_CHANGE_EVENT_TAG = "networkChangeEvent";
	
	static final String START_TIME_TAG = "startTime";
	
	static final String LINK_TAG = "link";
	
	static final String REF_ID_TAG = "refId";
	
	static final String FLOW_CAPACITY_TAG = "flowCapacity";
	
	static final String FREESPEED_TAG = "freespeed";
	
	static final String LANES_TAG = "lanes";
	
	static final String CHANGE_TYPE_TAG = "type";
	
	static final String VALUE_TAG = "value";
	
	static final String ABSOLUTE_VALUE = "absolute";
	
	static final String FACTOR_VALUE = "scaleFactor";

	// ========================================================================
	// private members
	// ========================================================================

	private final Network network;
	
	private NetworkChangeEvent currentEvent;
	
	private final List<NetworkChangeEvent> events ;
	
	// ========================================================================
	// constructor
	// ========================================================================

	public NetworkChangeEventsParser(Network network, List<NetworkChangeEvent> events ) {
		this.network = network;
		this.events = events;
	}
	
	// ========================================================================
	// parsing
	// ========================================================================
	
//	/**
//	 * Parses a file with network change events and returns a list with
//	 * instances of {@link NetworkChangeEvent}.
//	 * 
//	 * @param file
//	 *            a xml file containing network change events.
//	 */
//	public List<NetworkChangeEvent> parseEvents(String file) {
//		events = new ArrayList<>();
//		super.parse(file);
//		return events;
//	}
//	
//
//	@Override
//	public void parse(String filename) throws UncheckedIOException {
//		events = new ArrayList<>();
//		super.parse(filename);
//	}
//
//	@Override
//	public void parse(URL url) throws UncheckedIOException {
//		events = new ArrayList<>();
//		super.parse(url);
//	}
	
	// ========================================================================
	// accessor
	// ========================================================================

//	/**
//	 * Returns the list with network change events. Be sure to call
//	 * {@link #parseEvents(String)}, {@link #parse(String)} or
//	 * {@link #parse(URL)} before.
//	 * 
//	 * @return a list of network change events, or <tt>null</tt> if
//	 *         {@link #parseEvents(String)}, {@link #parse(String)} nor
//	 *         {@link #parse(URL)} has been called before.
//	 */
//	public List<NetworkChangeEvent> getEvents() {
//		return events;
//	}
	
	// ========================================================================
	// parsing methods
	// ========================================================================

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equalsIgnoreCase(NETWORK_CHANGE_EVENT_TAG)) {
			events.add(currentEvent);
			currentEvent = null;
		}
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		/*
		 * NetworkChangeEvent
		 */
		if(name.equalsIgnoreCase(NETWORK_CHANGE_EVENT_TAG)) {
			String value = atts.getValue(START_TIME_TAG);
			if(value != null) {
				currentEvent = new NetworkChangeEvent(Time.parseTime(value));
			} else {
				currentEvent = null;
				log.warn("A start time must be defined!");
			}
		/*
		 * Links
		 */
		} else if(name.equalsIgnoreCase(LINK_TAG) && currentEvent != null) {
			String value = atts.getValue(REF_ID_TAG);
			if(value != null) {
				Link link = network.getLinks().get(Id.create(value, Link.class));
				if(link != null)
					currentEvent.addLink(link);
				else
					log.warn(String.format("Link %1$s not found!", value));
			}
		/*
		 * flow capacity changes 
		 */
		} else if(name.equalsIgnoreCase(FLOW_CAPACITY_TAG) && currentEvent != null) {
			currentEvent.setFlowCapacityChange(newNetworkChangeType(atts
					.getValue(CHANGE_TYPE_TAG), atts.getValue(VALUE_TAG)));
		/*
		 * freespeed change
		 */
		} else if(name.equalsIgnoreCase(FREESPEED_TAG) && currentEvent != null) {
			currentEvent.setFreespeedChange(newNetworkChangeType(atts
					.getValue(CHANGE_TYPE_TAG), atts.getValue(VALUE_TAG)));
		/*
		 * lanes changes
		 */
		} else if(name.equalsIgnoreCase(LANES_TAG) && currentEvent != null) {
			currentEvent.setLanesChange(newNetworkChangeType(atts
					.getValue(CHANGE_TYPE_TAG), atts.getValue(VALUE_TAG)));
		}
		
	}

	private static ChangeValue newNetworkChangeType(String typeStr, String valueStr) {
		if(typeStr != null && valueStr != null) {
			double value = Double.parseDouble(valueStr);
			if(typeStr.equalsIgnoreCase(ABSOLUTE_VALUE))
				return new ChangeValue(ChangeType.ABSOLUTE_IN_SI_UNITS, value);
			else if(typeStr.equalsIgnoreCase(FACTOR_VALUE))
				return new ChangeValue(ChangeType.FACTOR, value);
			else {
				log.warn(String.format(
					"The change type %1$s is not allowed. Only \"%2$s\" and \"%3$s\" permitted!",
					typeStr, ABSOLUTE_VALUE, FACTOR_VALUE));
				return null;
			}
		} else {
			log.warn("Change type and value must be specified!");
			return null;
		}
	}
}
