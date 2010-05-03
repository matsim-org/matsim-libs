/* *********************************************************************** *
 * project: org.matsim.*
 * IncidentsReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.johannes.teach.telematics;

import gnu.trove.TIntObjectHashMap;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeType;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author illenberger
 *
 */
public class IncidentsReader extends DefaultHandler {
	
	private static final Logger logger = Logger.getLogger(IncidentsReader.class);

	private static final String INCIDENT_TAG = "incident";
	
	private static final String LINK_PARAM = "link";
	
	private static final String ITERATION_PARAM = "iteration";
	
	private static final String CAPFACTOR_PARAM = "capfactor";
	
	private TIntObjectHashMap<List<NetworkChangeEvent>> changeEvents;
	
	private Network network;
	
	public IncidentsReader(Network network) {
		this.network = network;
	}
	
	public TIntObjectHashMap<List<NetworkChangeEvent>> read(String filename) {
		changeEvents = new TIntObjectHashMap<List<NetworkChangeEvent>>();
		
		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);
		factory.setNamespaceAware(false);
		SAXParser parser;
		try {
			parser = factory.newSAXParser();
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(this);
			reader.parse(filename);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		
		return changeEvents;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(INCIDENT_TAG.equalsIgnoreCase(qName)) {
			NetworkChangeEvent event = new NetworkChangeEvent(0.0);
			/*
			 * link id
			 */
			String val = attributes.getValue(LINK_PARAM);
			if(val == null) {
				logger.warn("No link paramter specified.");
				System.exit(-1);
			} else {
				event.addLink(network.getLinks().get(new IdImpl(val)));
			}
			/*
			 * capacity factor
			 */
			val = attributes.getValue(CAPFACTOR_PARAM);
			if(val == null) {
				logger.info("No capacity factor specified.");
				System.exit(-1);
			} else {
				event.setFlowCapacityChange(new ChangeValue(ChangeType.FACTOR, Double.parseDouble(val)));
			}
			/*
			 * iteration
			 */
			val = attributes.getValue(ITERATION_PARAM);
			if(val == null) {
				logger.warn("No iteration param specified.");
				System.exit(-1);
			} else {
				int it = Integer.parseInt(val);
				List<NetworkChangeEvent> events = changeEvents.get(it);
				if(events == null) {
					events = new LinkedList<NetworkChangeEvent>();
					changeEvents.put(it, events);
				}
				
				events.add(event);
			}
		}
	}

}
