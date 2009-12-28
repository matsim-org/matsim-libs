/* *********************************************************************** *
 * project: org.matsim.*
 * TransitInteractionRemover.java
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

package playground.mrieser.pt.router;

import java.util.List;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.pt.PtConstants;

/**
 * Removes all transit activities (like "pt -interaction") as well as the legs
 * following those activities. In addition, all legs with mode "walk"
 * are set to mode "pt" to be routed again with the transit. 
 * 
 * @see PtConstants#TRANSIT_ACTIVITY_TYPE
 * 
 * @author mrieser
 */
public class TransitActsRemover implements PlanAlgorithm {

	public void run(final Plan plan) {
		List<PlanElement> planElements = plan.getPlanElements();
		for (int i = 0, n = planElements.size(); i < n; i++) {
			PlanElement pe = planElements.get(i);
			if (pe instanceof Activity) {
				Activity act = (Activity) pe;
				if (PtConstants.TRANSIT_ACTIVITY_TYPE.equals(act.getType())) {
					((PlanImpl) plan).removeActivity(i);
					n -= 2;
					i--; // i will be incremented again in next loop-iteration, so we'll check the next act
				}
			} else if (pe instanceof Leg) {
				Leg leg = (Leg) pe;
				if (TransportMode.walk.equals(leg.getMode())) {
					leg.setMode(TransportMode.pt);
					leg.setRoute(null);
				}
			}
		}
	}

}
