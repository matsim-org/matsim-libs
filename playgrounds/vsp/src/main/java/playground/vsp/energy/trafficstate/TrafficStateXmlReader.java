/* *********************************************************************** *
 * project: org.matsim.*
 * TrafficStateXmlReader
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.vsp.energy.trafficstate;

import java.io.File;
import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Stack;

import javax.xml.bind.DatatypeConverter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * @author dgrether
 * 
 */
public class TrafficStateXmlReader {

	private static final Logger log = Logger.getLogger(TrafficStateXmlReader.class);

	private EdgeInfo currentEdgeInfo = null;
	private Stack stack = new Stack();

	private TrafficState state = null;
	
	public TrafficStateXmlReader(TrafficState state){
		this.state = state;
	}
	
	private XMLInputFactory createXmlInputFactory(){
		XMLInputFactory xmlif = null;
		try {
			xmlif = XMLInputFactory.newInstance();
			xmlif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
			xmlif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
			// set the IS_COALESCING property to true , if application desires to
			// get whole text data as one event.
			xmlif.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return xmlif;
	}
	
	
	public void readFile(String filename) {
		File file = null;
		file = new File(filename);
		if (!file.exists()) {
			log.error("File: " + filename + " does not exist.");
			return;
		}

		XMLInputFactory xmlif = this.createXmlInputFactory();

		long starttime = System.currentTimeMillis();
		try {
			XMLStreamReader xmlr = xmlif.createXMLStreamReader(filename, new FileInputStream(file));
			int eventType = xmlr.getEventType();
			while (xmlr.hasNext()) {
				eventType = xmlr.next();
//				log.debug("EventType: " + eventType);
//				 printEventType(eventType);

				 if (XMLEvent.START_ELEMENT == eventType){
					 String name = xmlr.getLocalName();
//					 log.debug("start element: " + name);
					 if ("edge_info".compareTo(name) == 0){
						 String id = xmlr.getAttributeValue(""	, "id");
						 this.currentEdgeInfo =  new EdgeInfo(Id.create(id, Link.class));
					 }
					 else if ("start_time".compareTo(name ) == 0){
						 String text = xmlr.getElementText();
//						 log.debug("start time: " + text);
						 Calendar cal = DatatypeConverter.parseDateTime(text);
						 Double time_sec = getTimeSeconds(cal);
						 this.stack.push(time_sec);
					 }
					 else if ("end_time".compareTo(name ) == 0){
						 String text = xmlr.getElementText();
//						 log.debug("end time: " + text);
						 Calendar cal = DatatypeConverter.parseDateTime(text);
						 Double time_sec = getTimeSeconds(cal);
						 this.stack.push(time_sec);
					 }
					 else if ("average_speed".compareTo(name ) == 0){
						 String text = xmlr.getElementText();
//						 log.debug("average_speed: " + text);
						 Double time_sec = Double.parseDouble(text);
						 this.stack.push(time_sec);
					 }
				 }
				 else if (XMLEvent.END_ELEMENT == eventType){
					 String name = xmlr.getLocalName();
//					 log.debug("end element: " + name);
					 if ("time_bin".compareTo(name) == 0){
						 Double speed = (Double) stack.pop();
						 Double endTime = (Double) stack.pop();
						 Double startTime = (Double) stack.pop();
						 TimeBin tb = new TimeBin(startTime, endTime, speed);
						 this.currentEdgeInfo.getTimeBins().add(tb);
					 }
					 else if ("edge_info".compareTo(name) == 0){
						 this.state.addEdgeInfo(this.currentEdgeInfo);
						 this.currentEdgeInfo = null;
					 }
				 }
			}

		} catch (XMLStreamException ex) {
			System.out.println(ex.getMessage());
			if (ex.getNestedException() != null)
				ex.getNestedException().printStackTrace();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		long endtime = System.currentTimeMillis();
		log.info(" Parsing Time = " + (endtime - starttime));

	}
	
	private Double getTimeSeconds(Calendar cal){
		return Double.valueOf(cal.get(Calendar.SECOND) + 60 * cal.get(Calendar.MINUTE) + 60 * 60  * cal.get(Calendar.HOUR));
	}

  /**
   * Returns the String representation of the given integer constant.
   *
   * @param eventType Type of event.
   * @return String representation of the event
   */    
  private final static String getEventTypeString(int eventType) {
      switch (eventType){
          case XMLEvent.START_ELEMENT:
              return "START_ELEMENT";
          case XMLEvent.END_ELEMENT:
              return "END_ELEMENT";
          case XMLEvent.PROCESSING_INSTRUCTION:
              return "PROCESSING_INSTRUCTION";
          case XMLEvent.CHARACTERS:
              return "CHARACTERS";
          case XMLEvent.COMMENT:
              return "COMMENT";
          case XMLEvent.START_DOCUMENT:
              return "START_DOCUMENT";
          case XMLEvent.END_DOCUMENT:
              return "END_DOCUMENT";
          case XMLEvent.ENTITY_REFERENCE:
              return "ENTITY_REFERENCE";
          case XMLEvent.ATTRIBUTE:
              return "ATTRIBUTE";
          case XMLEvent.DTD:
              return "DTD";
          case XMLEvent.CDATA:
              return "CDATA";
          case XMLEvent.SPACE:
              return "SPACE";
      }
      return "UNKNOWN_EVENT_TYPE , " + eventType;
  }
  
  private static void printEventType(int eventType) {        
      System.out.println("EVENT TYPE("+eventType+") = " + getEventTypeString(eventType));
  }
  
	
}
