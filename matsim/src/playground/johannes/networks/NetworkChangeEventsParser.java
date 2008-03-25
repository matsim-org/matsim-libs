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
package playground.johannes.networks;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.utils.io.MatsimXmlParser;
import org.matsim.utils.misc.Time;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import playground.johannes.networks.NetworkChangeEvent.ChangeType;
import playground.johannes.networks.NetworkChangeEvent.ChangeValue;

/**
 * A parser building a list of {@link NetworkChangeEvent} out of a xml file.
 * 
 * @author illenberger
 * 
 */
public class NetworkChangeEventsParser extends MatsimXmlParser {
	
	// ========================================================================
	// static fields
	// ========================================================================
	
	private static final Logger log = Logger.getLogger(NetworkChangeEventsParser.class);
	
	protected static final String NETWORK_CHANGE_EVENTS_TAG = "networkChangeEvents";
	
	protected static final String NETWORK_CHANGE_EVENT_TAG = "networkChangeEvent";
	
	protected static final String START_TIME_TAG = "startTime";
	
	protected static final String END_TIME_TAG = "endTime";
	
	protected static final String LINK_TAG = "link";
	
	protected static final String REF_ID_TAG = "refId";
	
	protected static final String FLOW_CAPACITY_TAG = "flowCapacity";
	
	protected static final String FREESPEED_TAG = "freespeed";
	
	protected static final String LANES_TAG = "lanes";
	
	protected static final String CHANGE_TYPE_TAG = "type";
	
	protected static final String VALUE_TAG = "value";
	
	protected static final String ABSOLUTE_VALUE = "absolute";
	
	protected static final String FACTOR_VALUE = "scaleFactor";

	// ========================================================================
	// private members
	// ========================================================================

	private NetworkLayer network;
	
	private NetworkChangeEvent currentEvent;
	
	private List<NetworkChangeEvent> events;
	
	// ========================================================================
	// constructor
	// ========================================================================

	public NetworkChangeEventsParser(NetworkLayer network) {
		this.network = network;
	}
	
	// ========================================================================
	// parsing
	// ========================================================================
	
	/**
	 * Parses a file with network change events and returns a list with
	 * instances of {@link NetworkChangeEvent}.
	 * 
	 * @param file
	 *            a xml file containing network change events.
	 */
	public List<NetworkChangeEvent> parseEvents(String file) {
		events = new ArrayList<NetworkChangeEvent>();
		try {
			super.parse(file);
		} catch (SAXException e) {
			log.fatal("Error during parsing.", e);
		} catch (ParserConfigurationException e) {
			log.fatal("Error during parsing.", e);
		} catch (IOException e) {
			log.fatal("Error during parsing.", e);
		}
		return events;
	}
	

	@Override
	public void parse(String filename) throws SAXException,
			ParserConfigurationException, IOException {
		events = new ArrayList<NetworkChangeEvent>();
		super.parse(filename);
	}

	@Override
	public void parse(URL url) throws SAXException,
			ParserConfigurationException, IOException {
		events = new ArrayList<NetworkChangeEvent>();
		super.parse(url);
	}
	
	// ========================================================================
	// accessor
	// ========================================================================

	/**
	 * Returns the list with network change events. Be sure to call
	 * {@link #parseEvents(String)}, {@link #parse(String)} or
	 * {@link #parse(URL)} before.
	 * 
	 * @return a list of network change events, or <tt>null</tt> if
	 *         {@link #parseEvents(String)}, {@link #parse(String)} nor
	 *         {@link #parse(URL)} has been called before.
	 */
	public List<NetworkChangeEvent> getEvents() {
		return events;
	}
	
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
				value = atts.getValue(END_TIME_TAG);
				if(value != null)
					currentEvent.setEndTime(Time.parseTime(value));
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
				Link link = network.getLink(value);
				if(link != null)
					currentEvent.getLinks().add(link);
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

	private ChangeValue newNetworkChangeType(String typeStr, String valueStr) {
		if(typeStr != null && valueStr != null) {
			double value = Double.parseDouble(valueStr);
			if(typeStr.equalsIgnoreCase(ABSOLUTE_VALUE))
				return new ChangeValue(ChangeType.ABSOLUTE, value);
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
