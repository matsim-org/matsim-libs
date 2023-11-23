/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.vsp.analysis.modules.bvgAna.anaLevel1.enterLeaveVehicle2Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.pt.PtConstants;

import playground.vsp.analysis.modules.ptDriverPrefix.PtDriverIdAnalyzer;

/**
 * Collects all <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEvent</code> with their
 * corresponding <code>ActivityEndEvent</code> and <code>ActivityStartEvent</code> ignoring <code>pt interaction</code> events.
 *
 * @author ikaddoura, aneumann
 *
 */
public class EnterLeaveVehicle2ActivityHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{

	private final Logger log = LogManager.getLogger(EnterLeaveVehicle2ActivityHandler.class);
//	private final Level logLevel = Level.DEBUG;
	private PtDriverIdAnalyzer ptDriverIdAnalyzer;
	private TreeMap<Id, ActivityEndEvent> agentId2ActEndEvent = new TreeMap<Id, ActivityEndEvent>();
	private TreeMap<Id, List<PersonLeavesVehicleEvent>> agentId2LeaveVehEvent = new TreeMap<Id, List<PersonLeavesVehicleEvent>>();

	private HashMap<PersonEntersVehicleEvent, ActivityEndEvent> personsEntersVehicleEvent2ActivityEndEvent = new HashMap<PersonEntersVehicleEvent, ActivityEndEvent>();
	private HashMap<PersonLeavesVehicleEvent, ActivityStartEvent> personLeavesVehicleEvent2ActivityStartEvent = new HashMap<PersonLeavesVehicleEvent, ActivityStartEvent>();

	public EnterLeaveVehicle2ActivityHandler(PtDriverIdAnalyzer ptDriverPrefixAnalyzer){
//		this.log.setLevel(this.logLevel);
		this.ptDriverIdAnalyzer = ptDriverPrefixAnalyzer;
	}

	/**
	 * @return Returns a map containing the preceding <code>ActivityEndEvent</code> for a given <code>PersonEntersVehicleEvent</code>
	 */
	public Map<PersonEntersVehicleEvent, ActivityEndEvent> getPersonEntersVehicleEvent2ActivityEndEvent() {
		return this.personsEntersVehicleEvent2ActivityEndEvent;
	}

	/**
	 * @return Returns a map containing the following <code>ActivityStartEvent</code> for a given <code>PersonLeavesVehicleEvent</code>
	 */
	public Map<PersonLeavesVehicleEvent, ActivityStartEvent> getPersonLeavesVehicleEvent2ActivityStartEvent() {
		return this.personLeavesVehicleEvent2ActivityStartEvent;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// pair the events
		Id personId = event.getPersonId();
		ActivityEndEvent endEvent = this.agentId2ActEndEvent.get(personId);
		if (endEvent != null) {
			this.personsEntersVehicleEvent2ActivityEndEvent.put(event, endEvent);
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// simply collect the event
		if (this.ptDriverIdAnalyzer.isPtDriver(event.getPersonId())){
			// pt driver
		} else {
			List<PersonLeavesVehicleEvent> events = this.agentId2LeaveVehEvent.get(event.getPersonId());
			if (events == null) {
				events = new ArrayList<PersonLeavesVehicleEvent>(3);
				this.agentId2LeaveVehEvent.put(event.getPersonId(), events);
			}
			events.add(event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// pair the events
			Id personId = event.getPersonId();
			this.agentId2ActEndEvent.remove(personId); // should not be necessary if we trust the events
			List<PersonLeavesVehicleEvent> events = this.agentId2LeaveVehEvent.remove(event.getPersonId());
			if (events != null) {
				for (PersonLeavesVehicleEvent leaveEvent : events) {
					this.personLeavesVehicleEvent2ActivityStartEvent.put(leaveEvent, event);
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// simply collect the event
			this.agentId2ActEndEvent.put(event.getPersonId(), event);
		}
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

}
