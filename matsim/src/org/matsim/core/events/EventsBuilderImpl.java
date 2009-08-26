/* *********************************************************************** *
 * project: org.matsim.*
 * BasicEventsBuilderImpl
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
import org.matsim.core.api.experimental.events.EventsBuilder;
import org.matsim.core.basic.v01.events.BasicPersonEntersVehicleEvent;
import org.matsim.core.basic.v01.events.BasicPersonLeavesVehicleEvent;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEvent;
import org.matsim.core.basic.v01.events.BasicVehicleArrivesAtFacilityEventImpl;
import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEvent;
import org.matsim.core.basic.v01.events.BasicVehicleDepartsAtFacilityEventImpl;


/**
 * @author dgrether
 *
 */
public class EventsBuilderImpl implements EventsBuilder {

	public BasicActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityEndEventImpl(time, agentId, linkId, acttype);
	}

	public BasicActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityStartEventImpl(time, agentId, linkId, acttype);
	}

	public BasicAgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId) {
		return new AgentArrivalEventImpl(time, agentId, linkId);
	}

	public BasicAgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId) {
		return new AgentDepartureEventImpl(time, agentId, linkId);
	}

	public BasicAgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new AgentMoneyEventImpl(time, agentId, amountMoney);
	}

	public BasicAgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId) {
		return new AgentStuckEventImpl(time, agentId, linkId);
	}

	public BasicAgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId) {
		return new AgentWait2LinkEventImpl(time, agentId, linkId);
	}

	public BasicLinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId) {
		return new LinkEnterEventImpl(time, agentId, linkId);
	}

	public BasicLinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId) {
		return new LinkLeaveEventImpl(time, agentId, linkId);
	}
	
	public BasicPersonEntersVehicleEvent createPersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		return new PersonEntersVehicleEventImpl(time, personId, vehicleId);
	}
	
	public BasicPersonLeavesVehicleEvent createPersonLeavesVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		return new PersonLeavesVehicleEventImpl(time, personId, vehicleId);
	}
	
	public BasicVehicleArrivesAtFacilityEvent createVehicleArrivesAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId) {
		return new BasicVehicleArrivesAtFacilityEventImpl(time, vehicleId, facilityId);
	}
	
	public BasicVehicleDepartsAtFacilityEvent createVehicleDepartsAtFacilityEvent(final double time, final Id vehicleId, final Id facilityId) {
		return new BasicVehicleDepartsAtFacilityEventImpl(time, vehicleId, facilityId);
	}
}
