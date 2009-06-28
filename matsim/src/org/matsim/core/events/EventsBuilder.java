/* *********************************************************************** *
 * project: org.matsim.*
 * EventsBuilder
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
import org.matsim.core.population.ActivityImpl;


/**
 * Builder for non basic events
 * @author dgrether
 * @deprecated just a draft, concept not ready for implementation:
 * how to deal with the activities, they have no container and are not 
 * a standalone part of the scenario. they have no id and their semantics
 * is not clear without the person's plan. same for the leg given in
 * non basic AgentEvents.
 */
@Deprecated
public interface EventsBuilder {

	LinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId);

	LinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId);

	AgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId);

	AgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId);

	AgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId);

	AgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId);

	ActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, ActivityImpl act);

	ActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, ActivityImpl act);

	AgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney);
	
}
