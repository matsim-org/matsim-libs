/* *********************************************************************** *
 * project: org.matsim.*
 * EventBuilder
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

import org.matsim.api.core.v01.Id;
import org.matsim.core.api.internal.MatsimFactory;
import org.matsim.core.events.PersonEntersVehicleEvent;
import org.matsim.core.events.PersonLeavesVehicleEvent;
import org.matsim.core.network.NetworkChangeEvent.ChangeValue;

/**
 * Builder for basic events.
 * @author dgrether
 *
 */
public interface EventsFactory extends MatsimFactory {

	LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId, Id vehicleId);

	LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId, Id vehicleId);

	LinkChangeEvent createLinkChangeFlowCapacityEvent(double time, Id linkId, ChangeValue changeValue);
	
	LinkChangeEvent createLinkChangeFreespeedEvent(double time, Id linkId, ChangeValue changeValue);
	
	LinkChangeEvent createLinkChangeLanesEvent(double time, Id linkId, ChangeValue changeValue);
	
	AgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId, final String legMode);

	AgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId, Id vehicleId);

	AgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId, final String legMode);

	AgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId, final String legMode);

	ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, final Id facilityId, String acttype);

	ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, final Id facilityId, String acttype);

	AgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney);

	PersonEntersVehicleEvent createPersonEntersVehicleEvent(double time, Id personId, Id vehicleId);

	PersonLeavesVehicleEvent createPersonLeavesVehicleEvent(double time, Id personId, Id vehicleId);
	
	GenericEvent createGenericEvent( String type, double time ) ;

}
