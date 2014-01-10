/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.julia.distribution;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsReaderXMLv1;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

public class ResponsibilityEventReader extends MatsimXmlParser{

	private static final Logger logger = Logger.getLogger(ResponsibilityEventReader.class);

	private static final String EVENT = "event";

	private final EventsManager eventsManager;
	
	public ResponsibilityEventReader(EventsManager eventsManager){
		super();
		this.eventsManager = eventsManager;
		setValidating(false); // events-files have no DTD, thus they cannot validate
	}
	

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (EVENT.equals(name)) {

			Id responsiblePersonId = new IdImpl(atts.getValue("responsiblePersonId"));  
			Id receivingPersonId = new IdImpl(atts.getValue("receivingPersonId"));	
			Double emissionEventTime = Double.parseDouble(atts.getValue("emissionEventStartTime"));
			Double exposureStartTime = Double.parseDouble(atts.getValue("exposureStartTime"));
			Double exposureEndTime = Double.parseDouble(atts.getValue("exposureEndTime"));
			Double concentration = Double.parseDouble(atts.getValue("concentration"));
			String exposureLocation = atts.getValue("location");

			ResponsibilityEventImpl event = new ResponsibilityEventImpl(responsiblePersonId, receivingPersonId, emissionEventTime, exposureStartTime, exposureEndTime, concentration, exposureLocation); 
			this.eventsManager.processEvent(event);
		}else{
			logger.warn("You are trying to read a non emission events file. For reading this, please use " + EventsReaderXMLv1.class);
			throw new RuntimeException();
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {		
	}

}
