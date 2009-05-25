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


/**
 * @author dgrether
 *
 */
public class BasicEventsBuilderImpl implements BasicEventsBuilder {

	public BasicActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityEndEvent(time, agentId, linkId, acttype);
	}

	public BasicActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, String acttype) {
		return new ActivityStartEvent(time, agentId, linkId, acttype);
	}

	public BasicAgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId) {
		return new AgentArrivalEvent(time, agentId, linkId);
	}

	public BasicAgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId) {
		return new AgentDepartureEvent(time, agentId, linkId);
	}

	public BasicAgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney) {
		return new AgentMoneyEvent(time, agentId, amountMoney);
	}

	public BasicAgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId) {
		return new AgentStuckEvent(time, agentId, linkId);
	}

	public BasicAgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId) {
		return new AgentWait2LinkEvent(time, agentId, linkId);
	}

	public BasicLinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId) {
		return new LinkEnterEvent(time, agentId, linkId);
	}

	public BasicLinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId) {
		return new LinkLeaveEvent(time, agentId, linkId);
	}
	
	public PersonEntersVehicleEvent createPersonEntersVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		return new PersonEntersVehicleEvent(time, personId, vehicleId);
	}
	
	public PersonLeavesVehicleEvent createPersonLeavesVehicleEvent(final double time, final Id personId, final Id vehicleId) {
		return new PersonLeavesVehicleEvent(time, personId, vehicleId);
	}
}
