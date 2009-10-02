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
public class EventsFactoryImpl implements EventsFactory {

	public ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityEndEventImpl(time, agentId, linkId, acttype);
	}

	public ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityStartEventImpl(time, agentId, linkId, acttype);
	}

	public AgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId) {
		return new AgentArrivalEventImpl(time, agentId, linkId);
	}

	public AgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId) {
		return new AgentDepartureEventImpl(time, agentId, linkId);
	}

	public AgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new AgentMoneyEventImpl(time, agentId, amountMoney);
	}

	public AgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId) {
		return new AgentStuckEventImpl(time, agentId, linkId);
	}

	public AgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId) {
		return new AgentWait2LinkEventImpl(time, agentId, linkId);
	}

	public LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId) {
		return new LinkEnterEventImpl(time, agentId, linkId);
	}

	public LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId) {
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
