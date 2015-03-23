/* *********************************************************************** *
 * project: org.matsim.*
 * MarathonCurrentLegReplanner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.christoph.icem2012;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.ActivityEndRescheduler;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.router.TripRouter;
import org.matsim.withinday.replanning.replanners.interfaces.WithinDayDuringLegReplanner;
import org.matsim.withinday.utils.EditRoutes;

/*
 * Re-routes an agents currently performed leg.
 * For re-routing, walk2d legs are handled as walk legs since the
 * router cannot handle them.
 */
public class MarathonCurrentLegReplanner extends WithinDayDuringLegReplanner {

	private final TripRouter tripRouter;
	private final EditRoutes editRoutes;
	
	/*package*/ MarathonCurrentLegReplanner(Id id, Scenario scenario, ActivityEndRescheduler internalInterface,
			TripRouter tripRouter) {
		super(id, scenario, internalInterface);
		this.tripRouter = tripRouter;
		this.editRoutes = new EditRoutes();
	}

	@Override
	public boolean doReplanning(MobsimAgent withinDayAgent) {

		// If we don't have a valid PersonAgent
		if (withinDayAgent == null) return false;

		Plan executedPlan = ((PlanAgent) withinDayAgent).getCurrentPlan();

		// If we don't have an executed plan
		if (executedPlan == null) return false;

		int currentLinkIndex = this.withinDayAgentUtils.getCurrentRouteLinkIdIndex(withinDayAgent);

		// for walk2d legs: switch mode to walk for routing
		Leg currentLeg = this.withinDayAgentUtils.getModifiableCurrentLeg(withinDayAgent);
		boolean isWalk2d = currentLeg.getMode().equals("walk2d");
		
		// switch to walk mode for routing
		if (isWalk2d) {
			currentLeg.setMode(TransportMode.walk);
		}

		// new Route for current Leg
		this.editRoutes.replanCurrentLegRoute(currentLeg, executedPlan.getPerson(), currentLinkIndex, time, 
				scenario.getNetwork(), tripRouter); 

		// switch back to walk2d
		if (isWalk2d) {
			currentLeg.setMode("walk2d");
		}
		
		// Finally reset the cached Values of the PersonAgent - they may have changed!
		this.withinDayAgentUtils.resetCaches(withinDayAgent);

		return true;
	}
}