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
package playground.mzilske.logevents;

import org.apache.log4j.Logger;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.PersonEvent;
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
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.SignalGroupStateChangedEvent;
import org.matsim.core.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.events.handler.LaneEnterEventHandler;
import org.matsim.core.events.handler.LaneLeaveEventHandler;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.events.handler.SignalGroupStateChangedEventHandler;
import org.matsim.core.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.events.handler.VehicleDepartsAtFacilityEventHandler;
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
	SignalGroupStateChangedEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler{

	private static final Logger log = Logger.getLogger(LogOutputEventHandler.class);

	public void handleEvent(LinkEnterEvent event) {
		log.info("LinkEnterEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId());
	}

	public void reset(int iteration) {}

	public void handleEvent(LinkLeaveEvent event) {
		log.info("LinkLeaveEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId());
	}

	public void handleEvent(ActivityStartEvent event) {
		log.info("ActivityStartEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " activity type " + event.getActType());
	}

	public void handleEvent(ActivityEndEvent event) {
		log.info("ActivityEndEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " activity type " + event.getActType());
	}

	public void handleEvent(AgentDepartureEvent event) {
		log.info("AgentDepartureEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(AgentArrivalEvent event) {
		log.info("AgentArrivalEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(AgentMoneyEvent event) {
		log.info("AgentMoneyEvent person id " + event.getPersonId());
	}

	public void handleEvent(AgentStuckEvent event) {
		log.info("AgentStuckEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(PersonEvent event) {
//		log.info("PersonEvent at " + Time.writeTime(event.getTime()) + " person id "  + event.getPersonId());
	}

	public void handleEvent(AgentWait2LinkEvent event) {
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

	public void handleEvent(PersonEntersVehicleEvent e) {
		log.info("PersonEntersVehicleEvent at " + Time.writeTime(e.getTime()) + " person id " + e.getPersonId() + " vehicle id " + e.getVehicleId());
	}

	public void handleEvent(PersonLeavesVehicleEvent e) {
		log.info("PersonLeavesVehicleEvent at " + Time.writeTime(e.getTime()) + " person id " + e.getPersonId() + " vehicle id " + e.getVehicleId());		
	}

	@Override
	public void handleEvent(VehicleArrivesAtFacilityEvent event) {
		log.info("VehicleArrivesAtFacilityEvent at " + Time.writeTime(event.getTime()) + " facility id " + event.getFacilityId() + " vehicle id " + event.getVehicleId() + " delay " + event.getDelay());
	}
	
	@Override
	public void handleEvent(VehicleDepartsAtFacilityEvent event) {
		log.info("VehicleDepartsAtFacilityEvent at " + Time.writeTime(event.getTime()) + " facility id " + event.getFacilityId() + " vehicle id " + event.getVehicleId() + " delay " + event.getDelay());
	}

}
