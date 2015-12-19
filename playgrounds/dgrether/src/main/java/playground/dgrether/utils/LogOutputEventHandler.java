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
package playground.dgrether.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.VehicleAbortsEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleAbortsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.utils.misc.Time;


/**
 * @author dgrether
 *
 */
public class LogOutputEventHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, 
	ActivityStartEventHandler, ActivityEndEventHandler, 
	PersonDepartureEventHandler, PersonArrivalEventHandler, 
	PersonMoneyEventHandler, PersonStuckEventHandler, 
	VehicleEntersTrafficEventHandler,
	LaneEnterEventHandler, LaneLeaveEventHandler,
	SignalGroupStateChangedEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
	VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, VehicleAbortsEventHandler, VehicleLeavesTrafficEventHandler{

	private static final Logger log = Logger.getLogger(LogOutputEventHandler.class);

	public void handleEvent(LinkEnterEvent event) {
		log.info("LinkEnterEvent at " + Time.writeTime(event.getTime()) + " vehicle id " + event.getVehicleId() + " link id " + event.getLinkId());
	}

	public void reset(int iteration) {}

	public void handleEvent(LinkLeaveEvent event) {
		log.info("LinkLeaveEvent at " + Time.writeTime(event.getTime()) + " vehicle id " + event.getVehicleId() + " link id " + event.getLinkId());
	}

	public void handleEvent(ActivityStartEvent event) {
		log.info("ActivityStartEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " activity type " + event.getActType());
	}

	public void handleEvent(ActivityEndEvent event) {
		log.info("ActivityEndEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " activity type " + event.getActType());
	}

	public void handleEvent(PersonDepartureEvent event) {
		log.info("AgentDepartureEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(PersonArrivalEvent event) {
		log.info("AgentArrivalEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(PersonMoneyEvent event) {
		log.info("AgentMoneyEvent person id " + event.getPersonId());
	}

	public void handleEvent(PersonStuckEvent event) {
		log.info("AgentStuckEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId());
	}

	public void handleEvent(Event event) {
//		log.info("PersonEvent at " + Time.writeTime(event.getTime()) + " person id "  + event.getDriverId());
	}

	public void handleEvent(VehicleEntersTrafficEvent event) {
		log.info("VehicleEntersTrafficEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId() + " vehicle id " + event.getVehicleId());
	}

	public void handleEvent(LaneEnterEvent event) {
		log.info("LaneEnterEvent at " + Time.writeTime(event.getTime()) + " vehicle id " + event.getVehicleId() + " lane id " + event.getLaneId() + " link id " + event.getLinkId());
	}

	public void handleEvent(LaneLeaveEvent event) {
		log.info("LaneLeaveEvent at " + Time.writeTime(event.getTime()) + " vehicle id " + event.getVehicleId() + " lane id " + event.getLaneId() + " link id " + event.getLinkId());
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

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		log.info("VehicleLeavesTrafficEvent at " + Time.writeTime(event.getTime()) + " person id " + event.getPersonId() + " link id " + event.getLinkId() + " vehicle id " + event.getVehicleId());
	}

	@Override
	public void handleEvent(VehicleAbortsEvent event) {
		log.info("VehicleAbortsEvent at " + Time.writeTime(event.getTime()) + " vehicle id " + event.getVehicleId());
	}

}
