/* *********************************************************************** *
 * project: org.matsim.*
 * EmissionEventsReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package playground.ikaddoura.integrationCN;

import java.io.InputStream;
import java.net.URL;
import java.util.Stack;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.events.EventsReaderXMLv1.CustomEventMapper;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.vehicles.Vehicle;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import playground.ikaddoura.noise2.data.ReceiverPoint;
import playground.ikaddoura.noise2.events.NoiseEventAffected;
import playground.ikaddoura.noise2.events.NoiseEventCaused;
import playground.vsp.congestion.events.CongestionEvent;

/**
 * @author ikaddoura
 *
 */
public class CNEventsReader extends MatsimXmlParser {

	private final EventsManager events;
	
	EventsReaderXMLv1 delegate;

	public CNEventsReader(EventsManager events) {
		this.events = events;
		delegate = new EventsReaderXMLv1(events);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		delegate.endTag(name, content, context);
	}
	
	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		System.out.println("start tag");
		delegate.startTag(name, atts, context);
	}

	public void addCustomEventMapper(String eventType, CustomEventMapper cem) {
		delegate.addCustomEventMapper(eventType, cem);
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		delegate.characters(ch, start, length);
	}

	public void endDocument() throws SAXException {
		delegate.endDocument();
	}

	public void setValidating(boolean validateXml) {
		delegate.setValidating(validateXml);
	}

	public void setNamespaceAware(boolean awareness) {
		delegate.setNamespaceAware(awareness);
	}

	public void setLocalDtdDirectory(String localDtdDirectory) {
		delegate.setLocalDtdDirectory(localDtdDirectory);
	}

	public void parse(String filename) throws UncheckedIOException {
		delegate.parse(filename);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		delegate.endElement(uri, localName, qName);
	}

	public void endPrefixMapping(String prefix) throws SAXException {
		delegate.endPrefixMapping(prefix);
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public InputSource resolveEntity(String publicId, String systemId) {
		return delegate.resolveEntity(publicId, systemId);
	}

	public void error(SAXParseException ex) throws SAXException {
		delegate.error(ex);
	}

	public void fatalError(SAXParseException ex) throws SAXException {
		delegate.fatalError(ex);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		delegate.ignorableWhitespace(ch, start, length);
	}

	public void notationDecl(String name, String publicId, String systemId)
			throws SAXException {
		delegate.notationDecl(name, publicId, systemId);
	}

	public void parse(URL url) throws UncheckedIOException {
		delegate.parse(url);
	}

	public void parse(InputStream stream) throws UncheckedIOException {
		delegate.parse(stream);
	}

	public void processingInstruction(String target, String data)
			throws SAXException {
		delegate.processingInstruction(target, data);
	}

	public void setDocumentLocator(Locator locator) {
		delegate.setDocumentLocator(locator);
	}

	public void skippedEntity(String name) throws SAXException {
		delegate.skippedEntity(name);
	}

	public void startDocument() throws SAXException {
		delegate.startDocument();
	}

	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		delegate.startPrefixMapping(prefix, uri);
	}

	public String toString() {
		return delegate.toString();
	}

	public void unparsedEntityDecl(String name, String publicId,
			String systemId, String notationName) throws SAXException {
		delegate.unparsedEntityDecl(name, publicId, systemId, notationName);
	}

	public void warning(SAXParseException ex) throws SAXException {
		delegate.warning(ex);
	}

	private void startEventCongestion(final Attributes attributes){

		String eventType = attributes.getValue("type");

		Double time = 0.0;
		Id<Link> linkId = null;
		Id<Person> causingAgentId = null;
		Id<Person> affectedAgentId = null;
		Double delay = 0.0;
		String constraint = null;
		Double emergenceTime = 0.0;

		if(CongestionEvent.EVENT_TYPE.equals(eventType)){
			
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_LINK)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_PERSON)){
					causingAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_AFFECTED_AGENT)){
					affectedAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_DELAY)){
					delay = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(CongestionEvent.EVENT_CAPACITY_CONSTRAINT)){
					constraint = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(CongestionEvent.ATTRIBUTE_EMERGENCETIME)){
					emergenceTime = Double.parseDouble(attributes.getValue(i));
				}				
				else {
					throw new RuntimeException("Unknown event attribute. Aborting...");
				}
			}
			this.events.processEvent(new CongestionEvent(time, constraint, causingAgentId, affectedAgentId, delay, linkId, emergenceTime));
		}
	}
	
	private void startEventNoise(final Attributes attributes){

		String eventType = attributes.getValue("type");

		if (NoiseEventCaused.EVENT_TYPE.equals(eventType)){
			Double time = 0.0;
			Double emergenceTime = 0.0;
			Id<Person> causingAgentId = null;
			Id<Vehicle> causingVehicleId = null;
			Double amount = 0.0;
			Id<Link> linkId = null;
			
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_EMERGENCE_TIME)){
					emergenceTime = Double.parseDouble(attributes.getValue(i));
				}	
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_AGENT_ID)){
					causingAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_VEHICLE_ID)){
					causingVehicleId = Id.create((attributes.getValue(i)), Vehicle.class);
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_AMOUNT_DOUBLE)){
					amount = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(NoiseEventCaused.ATTRIBUTE_LINK_ID)){
					linkId = Id.create((attributes.getValue(i)), Link.class);
				}
				else {
					throw new RuntimeException("Unknown event attribute. Aborting... " + attributes.getQName(i));
				}
			}
			this.events.processEvent(new NoiseEventCaused(time, emergenceTime, causingAgentId, causingVehicleId, amount, linkId));
		}
		
		else if (NoiseEventAffected.EVENT_TYPE.equals(eventType)){
			Double time = 0.0;
			Double emergenceTime = 0.0;
			Id<Person> affectedAgentId = null;
			Double amount = 0.0;
			Id<ReceiverPoint> receiverPointId = null;
			String activityType = null;
			
			for (int i = 0; i < attributes.getLength(); i++){
				if (attributes.getQName(i).equals("time")){
					time = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals("type")){
					eventType = attributes.getValue(i);
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_EMERGENCE_TIME)){
					emergenceTime = Double.parseDouble(attributes.getValue(i));
				}	
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_AGENT_ID)){
					affectedAgentId = Id.create((attributes.getValue(i)), Person.class);
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_ACTIVTITY_TYPE)){
					activityType = (attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_AMOUNT_DOUBLE)){
					amount = Double.parseDouble(attributes.getValue(i));
				}
				else if(attributes.getQName(i).equals(NoiseEventAffected.ATTRIBUTE_RECEIVERPOINT_ID)){
					receiverPointId = Id.create((attributes.getValue(i)), ReceiverPoint.class);
				}
				else {
					throw new RuntimeException("Unknown event attribute. Aborting... " + attributes.getQName(i));
				}
			}
			this.events.processEvent(new NoiseEventAffected(time, emergenceTime, affectedAgentId, amount, receiverPointId, activityType));
		}
	}
}
