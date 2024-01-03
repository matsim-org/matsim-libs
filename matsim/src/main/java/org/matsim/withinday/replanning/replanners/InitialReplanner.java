/* *********************************************************************** *
 * project: org.matsim.*
 * InitialReplanner.java
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

package org.matsim.withinday.replanning.replanners;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.router.PlanRouter;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayInitialReplanner;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayReplanner;

/*
 * The InitialReplanner can be used when the Simulations is initialized but
 * has not started yet.
 */

public class InitialReplanner extends WithinDayInitialReplanner {

	private final PlanRouter planRouter;

	/*package*/ InitialReplanner(Id<WithinDayReplanner> id, Scenario scenario, ActivityEndRescheduler internalInterface, PlanRouter planRouter) {
		super(id, scenario, internalInterface);
		this.planRouter = planRouter;
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		this.planRouter.run(WithinDayAgentUtils.getModifiablePlan(withinDayAgent));
		return true;
	}
}
