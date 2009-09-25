/* *********************************************************************** *
 * project: org.matsim.*
 * LogOutEventHandler
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package playground.dgrether.tests;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.api.basic.v01.events.BasicPersonEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentWait2LinkEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;
import org.matsim.core.events.LaneEnterEvent;
import org.matsim.core.events.LaneLeaveEvent;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.events.handler.SignalGroupStateChangedEventHandler;
import org.matsim.core.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class LogOutputEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	ActivityStartEventHandler, ActivityEndEventHandler, 
	AgentDepartureEventHandler, AgentArrivalEventHandler, 
	AgentMoneyEventHandler, AgentStuckEventHandler, 
	PersonEventHandler, AgentWait2LinkEventHandler,
	LaneEnterEventHandler, LaneLeaveEventHandler,
	SignalGroupStateChangedEventHandler{

	private static final Logger log = Logger.getLogger(LogOutputEventHandler.class);

	public void handleEvent(BasicLinkEnterEvent event) {
		log.info("LinkEnterEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId());
	}

	public void reset(int iteration) {}

	public void handleEvent(BasicLinkLeaveEvent event) {
		log.info("LinkLeaveEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId());
	}

	public void handleEvent(BasicActivityStartEvent event) {
		log.info("ActivityStartEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " activity type " + event.getActType());
	}

	public void handleEvent(BasicActivityEndEvent event) {
		log.info("ActivityEndEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " activity type " + event.getActType());
	}

	public void handleEvent(BasicAgentDepartureEvent event) {
		log.info("AgentDepartureEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(BasicAgentArrivalEvent event) {
		log.info("AgentArrivalEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(BasicAgentMoneyEvent event) {
		log.info("AgentMoneyEvent person id " + event.getPersonId());
	}

	public void handleEvent(BasicAgentStuckEvent event) {
		log.info("AgentStuckEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(BasicPersonEvent event) {
//		log.info("PersonEvent at " + Time.writeTime(event.getTime()) + " person id "  + event.getPersonId());
	}

	public void handleEvent(BasicAgentWait2LinkEvent event) {
		log.info("AgentWait2LinkEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId());
	}

	public void handleEvent(LaneEnterEvent event) {
		log.info("LaneEnterEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " lane id " + event.getLaneId() + " link id " + event.getLinkId());
	}

	public void handleEvent(LaneLeaveEvent event) {
		log.info("LaneLeaveEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " lane id " + event.getLaneId() + " link id " + event.getLinkId());
	}

	public void handleEvent(SignalGroupStateChangedEvent event) {
		log.info("SignalGroupStateChangedEvent at " + Time.writeTime(event.getTime()) 
				+	" SignalSystem id " + event.getSignalSystemId() 
				+ " SignalGroup id " + event.getSignalGroupId() 
				+ " SignalGroupState " + event.getNewState());
	}


}
