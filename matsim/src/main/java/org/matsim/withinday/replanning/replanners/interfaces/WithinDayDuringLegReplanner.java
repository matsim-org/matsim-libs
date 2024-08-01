/* *********************************************************************** *
 * project: org.matsim.*
 * WithinDayDuringLegReplanner.java
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

package org.matsim.withinday.replanning.replanners.interfaces;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.withinday.replanning.identifiers.interfaces.DuringLegAgentSelector;

/*
 * Replans only Agents that are currently performing a Leg.
 */
public abstract class WithinDayDuringLegReplanner extends WithinDayReplanner<DuringLegAgentSelector> {

	public WithinDayDuringLegReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface) {
		super(id, scenario, internalInterface);
	}

}
