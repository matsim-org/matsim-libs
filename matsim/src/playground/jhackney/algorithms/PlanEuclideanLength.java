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

package playground.jhackney.algorithms;

import org.matsim.plans.Act;
import org.matsim.plans.Plan;

public class PlanEuclideanLength {
    
    public double getPlanLength(Plan plan){

	double length=0.;
	for (int i = 0, max= plan.getActsLegs().size(); i < max-2; i += 2) {
		Act act1 = (Act)(plan.getActsLegs().get(i));
		Act act2 = (Act)(plan.getActsLegs().get(i+2));

		if (act2 != null || act1 != null) {
			double dist = act1.getCoord().calcDistance(act2.getCoord());
			length += dist;
		}
	}
	return length;
    }
}
