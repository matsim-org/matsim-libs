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

package playground.andreas.bvgAna.personEnterLeave2Act;

import java.util.HashMap;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.core.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.pt.PtConstants;

/**
 * Collects all <code>PersonEntersVehicleEvent</code> and <code>PersonLeavesVehicleEvent</code> with their
 * corresponding <code>ActivityEndEvent</code> and <code>ActivityStartEvent</code> for a given set of agent ids ignoring <code>pt interaction</code> events.
 * 
 * @author aneumann
 *
 */
public class PersonEnterLeave2ActHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, ActivityStartEventHandler, ActivityEndEventHandler{
	
	private final Logger log = Logger.getLogger(PersonEnterLeave2ActHandler.class);
	private final Level logLevel = Level.DEBUG;
	private Set<Id> agentIds;
	private TreeMap<Id, ActivityEndEvent> agentId2ActEndEvent = new TreeMap<Id, ActivityEndEvent>();
	private TreeMap<Id, PersonLeavesVehicleEvent> agentId2LeaveVehEvent = new TreeMap<Id, PersonLeavesVehicleEvent>();
	
	private HashMap<PersonEntersVehicleEvent, ActivityEndEvent> personsEntersVehicleEvent2ActivityEndEvent = new HashMap<PersonEntersVehicleEvent, ActivityEndEvent>();
	private HashMap<PersonLeavesVehicleEvent, ActivityStartEvent> personLeavesVehicleEvent2ActivityStartEvent = new HashMap<PersonLeavesVehicleEvent, ActivityStartEvent>();
	
	public PersonEnterLeave2ActHandler(Set<Id> agentIds){
		this.log.setLevel(this.logLevel);
		this.agentIds = agentIds;
	}
	
	/**
	 * @return Returns a map containing the preceding <code>ActivityEndEvent</code> for a given <code>PersonEntersVehicleEvent</code>
	 */
	public HashMap<PersonEntersVehicleEvent, ActivityEndEvent> getPersonsEntersVehicleEvent2ActivityEndEvent() {
		return this.personsEntersVehicleEvent2ActivityEndEvent;
	}

	/**
	 * @return Returns a map containing the following <code>ActivityStartEvent</code> for a given <code>PersonLeavesVehicleEvent</code>
	 */
	public HashMap<PersonLeavesVehicleEvent, ActivityStartEvent> getPersonLeavesVehicleEvent2ActivityStartEvent() {
		return this.personLeavesVehicleEvent2ActivityStartEvent;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		// pair the events
		Id personId = event.getPersonId();
		if(this.agentIds.contains(personId)){
			if(this.agentId2ActEndEvent.get(personId) != null){				
				this.personsEntersVehicleEvent2ActivityEndEvent.put(event, this.agentId2ActEndEvent.remove(personId));
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		// simply collect the event
		if(this.agentIds.contains(event.getPersonId())){
			this.agentId2LeaveVehEvent.put(event.getPersonId(), event);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if(!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// pair the events
			Id personId = event.getPersonId();
			if(this.agentIds.contains(personId)){
				if(this.agentId2LeaveVehEvent.get(personId) != null){
					this.personLeavesVehicleEvent2ActivityStartEvent.put(this.agentId2LeaveVehEvent.remove(personId), event);
				}
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if(!event.getActType().equalsIgnoreCase(PtConstants.TRANSIT_ACTIVITY_TYPE)){
			// simply collect the event
			if(this.agentIds.contains(event.getPersonId())){
				this.agentId2ActEndEvent.put(event.getPersonId(), event);
			}
		}
	}

	@Override
	public void reset(int iteration) {
		this.log.debug("reset method in iteration " + iteration + " not implemented, yet");
	}

}
