/* *********************************************************************** *
 * project: org.matsim.*
 * PlanSimplifyForDebug.java
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

package org.matsim.plans.algorithms;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.plans.Act;
import org.matsim.plans.Leg;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.plans.Route;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;

/**
 * @author mrieser
 * 
 * Removes all non-work and non-edu/non-uni activities and ensures that at most
 * one work or edu activity exists in a plan, and removes all plans from a
 * person not having one work or edu activity. This could lead to person having
 * no plans at all, they should be removed in a later step. All resulting plans
 * have exactly three activities, home-{work|edu}-work. The first activity
 * (home) ends at a random time between 6am and 9am, the second activity (work
 * or edu) has a duration of exactly 8 hours, and the final activity goes on
 * until midnight.
 */
public class PlanSimplifyForDebug extends PersonAlgorithm {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	protected Set<String> homeActs = null;
	protected Set<String> workActs = null;
	protected Set<String> eduActs = null;
	protected PlansCalcRoute router = null;

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	public PlanSimplifyForDebug(final NetworkLayer network) {
		this.homeActs = new HashSet<String>();
		// this.homeActs.add("h0.5");

		// this.workActs = new HashSet<String>(3);
		this.workActs = new HashSet<String>();
		// this.workActs.add("w0.5");
		// this.workActs.add("work2");
		// this.workActs.add("work3");

		// this.eduActs = new HashSet<String>(2);
		this.eduActs = new HashSet<String>();
		// this.eduActs.add("e0.5");
		// this.eduActs.add("uni");

		FreespeedTravelTimeCost timeCostCalc = new FreespeedTravelTimeCost();
		this.router = new PlansCalcRoute(network, timeCostCalc, timeCostCalc);
	}

	// ////////////////////////////////////////////////////////////////////
	// run methods
	// ////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		for (Iterator<Plan> iter = person.getPlans().iterator(); iter.hasNext();) {
			Plan plan = iter.next();
			run(plan);
			if (plan.getActsLegs().size() != 5) {
				iter.remove();
			}
		}
	}

	private void run(final Plan plan) {
		removeUnwantedActs(plan);
		shortenPlan(plan);
		setTimes(plan);
		restoreRoutes(plan);
	}

	/**
	 * removes all activities with an unwanted type. "unwanted" means the type
	 * is not listed in this.workActs or this.eduActs and is not "home". In the
	 * same step, all activities listed in workActs are renamed "work", while
	 * all acts listed in eduActs are renamed "edu".
	 * 
	 * @param plan
	 */
	private void removeUnwantedActs(final Plan plan) {
		List<?> actsLegs = plan.getActsLegs();
		for (int i = 0; i < actsLegs.size(); i += 2) {
			Act act = (Act) actsLegs.get(i);
			if (this.homeActs.contains(act.getType())) {
				act.setType("h");
			} else if (this.workActs.contains(act.getType())) {
				act.setType("w");
			} else if (this.eduActs.contains(act.getType())) {
				act.setType("e");
			} else {
				// nothing we're interested in, remove
				plan.removeAct(i);
				i -= 2;
			}
		}
	}

	/**
	 * deletes all activities of a plan except the first, the second and the
	 * last activity, so the plan should only consist of h-X-h activities.
	 * 
	 * @param plan
	 */
	private void shortenPlan(final Plan plan) {
		List<?> actsLegs = plan.getActsLegs();
		while (actsLegs.size() > 5) {
			plan.removeAct(4);
		}
	}

	private void setTimes(final Plan plan) {
		// we assume we get simple h-X-h-Plans at this stage
		if (plan.getActsLegs().size() != 5)
			return;

		Act act = (Act) plan.getActsLegs().get(0);
		int time = 6 * 3600 + (int) (Gbl.random.nextDouble() * 3600 * 3); // random
																			// time
																			// between
																			// 6am
																			// and
																			// 9am
		act.setStartTime(0);
		act.setEndTime(time);
		act.setDur(time);

		Leg leg = (Leg) plan.getActsLegs().get(1);
		leg.setDepTime(time);
		leg.setArrTime(time);
		leg.setTravTime(0);

		act = (Act) plan.getActsLegs().get(2);
		act.setStartTime(time);
		act.setEndTime(24 * 3600);
		act.setDur(8 * 3600);
		// set endTime to 24h, not time+8h, so agents are not forced to leave
		// work
		// before duration is over when they arrived late

		leg = (Leg) plan.getActsLegs().get(3);
		leg.setDepTime(time + 8 * 3600);
		leg.setArrTime(time + 8 * 3600);
		leg.setTravTime(0);

		act = (Act) plan.getActsLegs().get(4);
		act.setStartTime(time + 8 * 3600);
		act.setEndTime(24 * 3600);
		act.setDur(16 * 3600 - time);
	}

	private void restoreRoutes(final Plan plan) {
		boolean needsRouter = false;
		List<?> actsLegs = plan.getActsLegs();
		for (int i = 1, max = actsLegs.size(); i < max; i += 2) {
			Leg leg = (Leg) actsLegs.get(i);
			Route route = leg.getRoute();
			if (route == null) {
				needsRouter = true;
			}
		}
		if (needsRouter) {
			this.router.run(plan);
		}
	}

}
