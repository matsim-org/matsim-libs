/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalBasicWithindayAgent.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim.agents;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

/**
 * <p>
 * This class is an attempt to provide access to the internals of PersonDriverAgentImpl
 * in a way that it can be used for within-day replanning.
 * </p>
 * <p>
 * Moreover, it is an attempt to replace ExperimentalBasicWithindayAgent which extends
 * PersonDriverAgentImpl. This has become a problem since other MATSim modules (such as
 * PT which uses TransitAgents) also extend that class. Instead, this class re-implements
 * the functionality of the PlanBasedWithinDayAgent Interface by accessing the package
 * protected methods from PlanBasedWithinDayAgent (this is possible since it is located
 * in the same package).
 * </p>
 * <i>The class is experimental. Use at your own risk, and expect even 
 * less support than with other pieces of matsim.</i>
 * 
 * @author cdobler
 */
public class WithinDayAgentUtils {

	public final Integer getCurrentPlanElementIndex(PersonDriverAgentImpl agent) {
		return agent.currentPlanElementIndex;
	}

	public final Integer getCurrentRouteLinkIdIndex(PersonDriverAgentImpl agent) {
		return agent.currentLinkIdIndex;
	}

	public final void calculateAndSetDepartureTime(PersonDriverAgentImpl agent, Activity act) {
		agent.calculateAndSetDepartureTime(act);
	}

	public final void resetCaches(PersonDriverAgentImpl agent) {
		agent.resetCaches();
	}
	
	public final Leg getCurrentLeg(PersonDriverAgentImpl agent) {
		PlanElement currentPlanElement = agent.getCurrentPlanElement();
		if (!(currentPlanElement instanceof Leg)) {
			return null;
		}
		return (Leg) currentPlanElement;
	}
}
