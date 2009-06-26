/* *********************************************************************** *
 * project: org.matsim.*
 * EventsBuilderImpl
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
package org.matsim.core.events;

import org.matsim.api.basic.v01.Coord;
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicActivityStartEvent;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentDepartureEvent;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.BasicAgentWait2LinkEvent;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.BasicLinkLeaveEvent;
import org.matsim.core.api.Scenario;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEvent;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEventImpl;
import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEventImpl;
import org.matsim.core.population.ActivityImpl;


/**
 * Builder for full, non basic events. use with care as activities are created
 * instead of set from the person's plan, the leg attributes of AgentEvents is set to null.
 * @author dgrether
 *
 */
public class EventsBuilderImpl implements BasicEventsBuilder {

	private Scenario scenario;

	public EventsBuilderImpl(Scenario scenario){
		this.scenario = scenario;
	}

	public BasicActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityEndEvent(time, this.scenario.getPopulation().getPersons().get(agentId)
				, this.scenario.getNetwork().getLinks().get(linkId), new ActivityImpl(acttype, (Coord)null));
	}

	public BasicActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityStartEvent(time, this.scenario.getPopulation().getPersons().get(agentId), 
				this.scenario.getNetwork().getLinks().get(linkId), new ActivityImpl(acttype, (Coord)null));
	}

	public BasicAgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId) {
		return new AgentArrivalEvent(time, this.scenario.getPopulation().getPersons().get(agentId), 
				this.scenario.getNetwork().getLinks().get(linkId), null);
	}

	public BasicAgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId) {
		return new AgentDepartureEvent(time, this.scenario.getPopulation().getPersons().get(agentId), 
				this.scenario.getNetwork().getLinks().get(linkId), null);
	}

	public BasicAgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new AgentMoneyEvent(time, this.scenario.getPopulation().getPersons().get(agentId), amountMoney);
	}

	public BasicAgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId) {
		return new AgentStuckEvent(time, this.scenario.getPopulation().getPersons().get(agentId), 
				this.scenario.getNetwork().getLinks().get(linkId), null);
	}

	public BasicAgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId) {
		return new AgentWait2LinkEvent(time, this.scenario.getPopulation().getPersons().get(agentId), 
				this.scenario.getNetwork().getLinks().get(linkId), null);
	}

	public BasicLinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId) {
		return new LinkEnterEvent(time, this.scenario.getPopulation().getPersons().get(agentId), this.scenario.getNetwork().getLinks().get(linkId));
	}

	public BasicLinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId) {
		return new LinkLeaveEvent(time, this.scenario.getPopulation().getPersons().get(agentId), this.scenario.getNetwork().getLinks().get(linkId));
	}

	public PersonEntersVehicleEvent createPersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		return new PersonEntersVehicleEvent(time, personId, vehicleId);
	}

	public PersonLeavesVehicleEvent createPersonLeavesVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		return new PersonLeavesVehicleEvent(time, personId, vehicleId);
	}
	
	public BasicVehicleArrivesAtFacilityEvent createVehicleArrivesAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId) {
		return new BasicVehicleArrivesAtFacilityEventImpl(time, vehicleId, facilityId);
	}
	
	public BasicVehicleDepartsAtFacilityEvent createVehicleDepartsAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId) {
		return new BasicVehicleDepartsAtFacilityEventImpl(time, vehicleId, facilityId);
	}

}
