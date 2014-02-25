/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.wrashid.parkingSearch.withindayFW.util;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.parking.lib.DebugLib;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.PersonDriverAgentImpl;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;


/**
 * Problem: the simulation can already be one plan element further than the
 * event handler, as event might happen in the middle of a sim-step. Therefore this method of access is safer than the +2,
 * 
 * This assumes, you are on a leg and want the next leg/ you are on a act and want the next act.
 * 
 * @author wrashid
 * 
 */
public class ParallelSafePlanElementAccessLib {

	
	
	public static Leg getNextLeg(PersonDriverAgentImpl agent) {
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		for (int i = planElementIndex + 1; i < executedPlan.getPlanElements().size(); i++) {
			if (executedPlan.getPlanElements().get(i) instanceof Leg) {
				return (Leg) executedPlan.getPlanElements().get(i);
			}
		}
		DebugLib.stopSystemAndReportInconsistency();

		return null;
	}

	public static Activity getNextAct(PersonDriverAgentImpl agent) {
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		for (int i = planElementIndex + 1; i < executedPlan.getPlanElements().size(); i++) {
			if (executedPlan.getPlanElements().get(i) instanceof Activity) {
				return (Activity) executedPlan.getPlanElements().get(i);
			}
		}
		DebugLib.stopSystemAndReportInconsistency();

		return null;
	}

	public static Activity getPreviousAct(PersonDriverAgentImpl agent) {
		Plan executedPlan = agent.getCurrentPlan();
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		for (int i = planElementIndex - 1; i < executedPlan.getPlanElements().size(); i--) {
			if (executedPlan.getPlanElements().get(i) instanceof Activity) {
				return (Activity) executedPlan.getPlanElements().get(i);
			}
		}
		DebugLib.stopSystemAndReportInconsistency();

		return null;
	}

	
	public static int getCurrentExpectedLegIndex(PersonDriverAgentImpl agent){
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		
		if (planElementIndex%2==0){
			planElementIndex--;
		}
		return planElementIndex;
	}
	
	public static int getCurrentExpectedActIndex(PersonDriverAgentImpl agent){
		int planElementIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
		
		if (planElementIndex%2==1){
			planElementIndex--;
		}
		return planElementIndex;
	}

}
