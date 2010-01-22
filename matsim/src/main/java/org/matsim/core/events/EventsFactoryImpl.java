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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.AgentWait2LinkEvent;
import org.matsim.core.api.experimental.events.EventsFactory;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;


/**
 * @author dgrether
 *
 */
public class EventsFactoryImpl implements EventsFactory {

	public ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, Id facilityId, String acttype) {
		return new ActivityEndEventImpl(time, agentId, linkId, facilityId, acttype);
	}

	public ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, Id facilityId, String acttype) {
		return new ActivityStartEventImpl(time, agentId, linkId, facilityId, acttype);
	}

	public AgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId, final TransportMode legMode) {
		return new AgentArrivalEventImpl(time, agentId, linkId, legMode);
	}

	public AgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId, final TransportMode legMode) {
		return new AgentDepartureEventImpl(time, agentId, linkId, legMode);
	}

	public AgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new AgentMoneyEventImpl(time, agentId, amountMoney);
	}

	public AgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId, final TransportMode legMode) {
		return new AgentStuckEventImpl(time, agentId, linkId, legMode);
	}

	public AgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId, final TransportMode legMode) {
		return new AgentWait2LinkEventImpl(time, agentId, linkId, legMode);
	}

	public LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId) {
		return new LinkEnterEventImpl(time, agentId, linkId);
	}

	public LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId) {
		return new LinkLeaveEventImpl(time, agentId, linkId);
	}
	
	public PersonEntersVehicleEvent createPersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId, final Id transitRouteId) {
		PersonEntersVehicleEventImpl e = new PersonEntersVehicleEventImpl(time, personId, vehicleId, transitRouteId);
		return e;
	}

	public PersonLeavesVehicleEvent createPersonLeavesVehicleEvent(final double time, final Id personId, final Id vehicleId, final Id transitRouteId) {
		PersonLeavesVehicleEventImpl e = new PersonLeavesVehicleEventImpl(time, personId, vehicleId, transitRouteId);
		return e;
	}
	
	public VehicleArrivesAtFacilityEvent createVehicleArrivesAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId, final double delay) {
		return new VehicleArrivesAtFacilityEventImpl(time, vehicleId, facilityId, delay);
	}
	
	public VehicleDepartsAtFacilityEvent createVehicleDepartsAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId, final double delay) {
		return new VehicleDepartsAtFacilityEventImpl(time, vehicleId, facilityId, delay);
	}

	
}
