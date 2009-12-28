/* *********************************************************************** *
 * project: org.matsim.*
 * PlanEuclideanLength.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.jhackney.socialnetworks.algorithms;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class PlanEuclideanLength {

	public double getPlanLength(Plan plan) {

		double length = 0.;
		ActivityImpl fromAct = (ActivityImpl) plan.getPlanElements().get(0);
		for (int i = 2, max = plan.getPlanElements().size(); i < max; i += 2) {
			ActivityImpl toAct = (ActivityImpl) (plan.getPlanElements().get(i));

			if (fromAct != null && toAct != null) {
				double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
				length += dist;
			}
			fromAct = toAct;
		}
		return length;
	}
}
