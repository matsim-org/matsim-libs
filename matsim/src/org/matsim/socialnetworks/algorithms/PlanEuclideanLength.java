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

package org.matsim.socialnetworks.algorithms;

import org.matsim.population.Act;
import org.matsim.population.Plan;

public class PlanEuclideanLength {

	public double getPlanLength(Plan plan) {

		double length = 0.;
		Act fromAct = (Act) plan.getActsLegs().get(0);
		for (int i = 2, max = plan.getActsLegs().size(); i < max; i += 2) {
			Act toAct = (Act) (plan.getActsLegs().get(i));

			if (fromAct != null && toAct != null) {
				double dist = fromAct.getCoord().calcDistance(toAct.getCoord());
				length += dist;
			}
			fromAct = toAct;
		}
		return length;
	}
}
