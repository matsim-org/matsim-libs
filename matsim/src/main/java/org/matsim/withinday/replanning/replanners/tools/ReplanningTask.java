/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningTask.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.replanners.tools;

import org.matsim.api.core.v01.Id;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;

public class ReplanningTask {

	protected MobsimAgent agentToReplan;
	protected Id<WithinDayReplanner> withinDayReplannerId;
	
	public ReplanningTask(MobsimAgent agentToReplan, Id<WithinDayReplanner> withinDayReplannerId) {
		this.agentToReplan = agentToReplan;
		this.withinDayReplannerId = withinDayReplannerId;
	}
	
	public MobsimAgent getAgentToReplan() {
		return this.agentToReplan;
	}
	
	public Id<WithinDayReplanner> getWithinDayReplannerId() {
		return this.withinDayReplannerId;
	}
}
