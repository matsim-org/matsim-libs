/* *********************************************************************** *
 * project: org.matsim.*
 * BasicEventBuilder
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
package org.matsim.core.api.experimental.events;

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.internal.MatsimFactory;


/**
 * Builder for basic events.
 * @author dgrether
 *
 */
public interface EventsFactory extends MatsimFactory {

	LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId);

	LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId);

	AgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId, final TransportMode legMode);

	AgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId, final TransportMode legMode);

	AgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId, final TransportMode legMode);

	AgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId, final TransportMode legMode);

	ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, String acttype);

	ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, String acttype);

	AgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney);

	// the following are not (yet) in the api?
	
//	BasicPersonEntersVehicleEvent createPersonEntersVehicleEvent(double time, Id personId, Id vehicleId);
//	BasicPersonLeavesVehicleEvent createPersonLeavesVehicleEvent(double time, Id personId, Id vehicleId);
//
//	BasicVehicleArrivesAtFacilityEvent createVehicleArrivesAtFacilityEvent(double time, Id vehicleId, Id facilityId);
//	BasicVehicleDepartsAtFacilityEvent createVehicleDepartsAtFacilityEvent(double time, Id vehicleId, Id facilityId);
	
}
