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
 * Builder for basic events.
 * @author dgrether
 *
 */
public interface BasicEventsBuilder {

	BasicLinkLeaveEvent createLinkLeaveEvent(double time, Id agentId, Id linkId);

	BasicLinkEnterEvent createLinkEnterEvent(double time, Id agentId, Id linkId);

	BasicAgentStuckEvent createAgentStuckEvent(double time, Id agentId, Id linkId);

	BasicAgentWait2LinkEvent createAgentWait2LinkEvent(double time, Id agentId, Id linkId);

	BasicAgentDepartureEvent createAgentDepartureEvent(double time, Id agentId, Id linkId);

	BasicAgentArrivalEvent createAgentArrivalEvent(double time, Id agentId, Id linkId);

	BasicActivityStartEvent createActivityStartEvent(double time, Id agentId, Id linkId, String acttype);

	BasicActivityEndEvent createActivityEndEvent(double time, Id agentId, Id linkId, String acttype);

	BasicAgentMoneyEvent createAgentMoneyEvent(double time, Id agentId, double amountMoney);

}
