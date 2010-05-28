/* *********************************************************************** *
 * project: org.matsim.*
 * MyEventHandler1.java
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

/**
 * 
 */
package tryouts.examples;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;

/**
 * @author thomas
 *
 */
public class MyEventHandler1 implements LinkEnterEventHandler,LinkLeaveEventHandler{
	
	private final static Logger log = Logger.getLogger(MyEventHandler1.class);
	
	public void reset(int iteration) {
		log.info("reset...");
	}


	public void handleEvent(LinkEnterEvent event) {
		log.info("LinkEnterEvent");
		log.info("Time: " + event.getTime());
		log.info("LinkId: " + event.getLinkId());
		log.info("PersonId: " + event.getPersonId());
	}

	public void handleEvent(LinkLeaveEvent event) {
		log.info("LinkLeaveEvent");
		log.info("Time: " + event.getTime());
		log.info("LinkId: " + event.getLinkId());
		log.info("PersonId: " + event.getPersonId());
	}

	public void handleEvent(AgentArrivalEvent event) {
		log.info("AgentArrivalEvent");
		log.info("Time: " + event.getTime());
		log.info("LinkId: " + event.getLinkId());
		log.info("PersonId: " + event.getPersonId());
	}

	public void handleEvent(AgentDepartureEvent event) {
		log.info("AgentDepartureEvent");
		log.info("Time: " + event.getTime());
		log.info("LinkId: " + event.getLinkId());
		log.info("PersonId: " + event.getPersonId());
	}

}

