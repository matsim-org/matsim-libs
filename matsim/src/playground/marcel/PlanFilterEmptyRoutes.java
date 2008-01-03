/* *********************************************************************** *
 * project: org.matsim.*
 * PlanFilterEmptyRoutes.java
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

package playground.marcel;


import java.util.ArrayList;
import java.util.Iterator;

import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.algorithms.PersonAlgorithm;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.utils.misc.Time;

/**
 * This algorithm filters empty routes from plans. If two activities happen on the same link
 * right one after the other, the router generates an empty route (new behaviour in matsimJ).
 * The old simulation (matsimC) generated in such cases a route from the end of the link to the
 * start of the link, resulting in the strange behaviour that agents travelled once ``around
 * the block'' or at least drove once up and down the street, when it did not move at all in
 * reality.<br/>
 * This algorithms thus removes empty plans, merging the activities together and thus generating
 * plans which can be run with the old matsimC-simulation.
 * 
 * PS: okay, that last sentence is rather wrong. I don't think this algo is really doing
 * something useful or is interesting for anybody, that's why I did not check it in to
 * org.matsim.demandmodeling.plans.algorithms, but to my own playground...
 * marcel, end of june 2006
 */
public class PlanFilterEmptyRoutes extends PersonAlgorithm implements PlanAlgorithmI {

	public PlanFilterEmptyRoutes() {
		super();
	}

	@Override
	public void run(Person person) {
		Iterator<Plan> iter = person.getPlans().iterator();
		while (iter.hasNext()) {
			Plan plan = iter.next();
			run(plan);
		}
	}
	
	public final void run(Plan plan) {
		ArrayList<Object> actslegs = plan.getActsLegs();
		for (int i = 1; i < actslegs.size(); i = i + 2) {
			Leg leg = (Leg)actslegs.get(i);
			if (leg.getRoute().getRoute().size() == 0) {
				Act act1 = (Act)actslegs.get(i-1);
				Act act2 = (Act)actslegs.get(i+1);
				
				double dur1 = act1.getDur();
				double dur2 = act2.getDur();
				double start1 = act1.getStartTime();
//				int start2 = act2.getStartTime();
//				int end1 = act1.getEndTime();
				double end2 = act2.getEndTime();
				
				if (end2 != Time.UNDEFINED_TIME) {
					act1.setEndTime(end2);
					if (start1 != Time.UNDEFINED_TIME) {
						act1.setDur(end2 - start1);
					}
				} else if (dur2 != Time.UNDEFINED_TIME && dur1 != Time.UNDEFINED_TIME) {
					act1.setDur(dur1 + dur2);
				}

				actslegs.remove(i+1);
				actslegs.remove(i);
				i = i - 2;
			}
		}
	}
	
}

