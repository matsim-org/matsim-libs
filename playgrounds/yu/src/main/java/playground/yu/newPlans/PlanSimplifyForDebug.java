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

package playground.yu.newPlans;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.RoutingContextImpl;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.population.algorithms.PlanAlgorithm;

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
public class PlanSimplifyForDebug extends AbstractPersonAlgorithm {

	// ////////////////////////////////////////////////////////////////////
	// member variables
	// ////////////////////////////////////////////////////////////////////

	protected Set<String> homeActs = null;
	protected Set<String> workActs = null;
	protected Set<String> eduActs = null;
	protected PlanAlgorithm router = null;

	// ////////////////////////////////////////////////////////////////////
	// constructors
	// ////////////////////////////////////////////////////////////////////

	public PlanSimplifyForDebug(final Scenario scenario) {
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

		FreespeedTravelTimeAndDisutility timeCostCalc = new FreespeedTravelTimeAndDisutility(new PlanCalcScoreConfigGroup());
		this.router =
				new PlanRouter(
						new TripRouterFactoryBuilderWithDefaults().build(
								scenario ).instantiateAndConfigureTripRouter(
										new RoutingContextImpl(
												timeCostCalc,
												timeCostCalc ) ) );
	}

	// ////////////////////////////////////////////////////////////////////
	// run methods
	// ////////////////////////////////////////////////////////////////////

	@Override
	public void run(final Person person) {
		for (Iterator<? extends Plan> iter = person.getPlans().iterator(); iter.hasNext();) {
			Plan plan = iter.next();
			run(plan);
			if (plan.getPlanElements().size() != 5) {
				iter.remove();
			}
		}
	}

	private void run(final Plan plan) {
		removeUnwantedActs((PlanImpl) plan);
		shortenPlan((PlanImpl) plan);
		setTimes((PlanImpl) plan);
		restoreRoutes((PlanImpl) plan);
	}

	/**
	 * removes all activities with an unwanted type. "unwanted" means the type
	 * is not listed in this.workActs or this.eduActs and is not "home". In the
	 * same step, all activities listed in workActs are renamed "work", while
	 * all acts listed in eduActs are renamed "edu".
	 * 
	 * @param plan
	 */
	private void removeUnwantedActs(final PlanImpl plan) {
		List<?> actsLegs = plan.getPlanElements();
		for (int i = 0; i < actsLegs.size(); i += 2) {
			ActivityImpl act = (ActivityImpl) actsLegs.get(i);
			if (this.homeActs.contains(act.getType())) {
				act.setType("h");
			} else if (this.workActs.contains(act.getType())) {
				act.setType("w");
			} else if (this.eduActs.contains(act.getType())) {
				act.setType("e");
			} else {
				// nothing we're interested in, remove
				plan.removeActivity(i);
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
	private void shortenPlan(final PlanImpl plan) {
		List<?> actsLegs = plan.getPlanElements();
		while (actsLegs.size() > 5) {
			plan.removeActivity(4);
		}
	}

	private void setTimes(final PlanImpl plan) {
		// we assume we get simple h-X-h-Plans at this stage
		if (plan.getPlanElements().size() != 5)
			return;

		ActivityImpl act = (ActivityImpl) plan.getPlanElements().get(0);
		int time = 6 * 3600 + (int) (MatsimRandom.getRandom().nextDouble() * 3600 * 3);
		act.setStartTime(0);
		act.setEndTime(time);
		act.setMaximumDuration(time);

		LegImpl leg = (LegImpl) plan.getPlanElements().get(1);
		leg.setDepartureTime(time);
		leg.setArrivalTime(time);
		leg.setTravelTime(0);

		act = (ActivityImpl) plan.getPlanElements().get(2);
		act.setStartTime(time);
		act.setEndTime(24 * 3600);
		act.setMaximumDuration(8 * 3600);
		// set endTime to 24h, not time+8h, so agents are not forced to leave
		// work
		// before duration is over when they arrived late

		leg = (LegImpl) plan.getPlanElements().get(3);
		leg.setDepartureTime(time + 8 * 3600);
		leg.setArrivalTime(time + 8 * 3600);
		leg.setTravelTime(0);

		act = (ActivityImpl) plan.getPlanElements().get(4);
		act.setStartTime(time + 8 * 3600);
		act.setEndTime(24 * 3600);
		act.setMaximumDuration(16 * 3600 - time);
	}

	private void restoreRoutes(final PlanImpl plan) {
		boolean needsRouter = false;
		List<?> actsLegs = plan.getPlanElements();
		for (int i = 1, max = actsLegs.size(); i < max; i += 2) {
			LegImpl leg = (LegImpl) actsLegs.get(i);
			if (leg.getRoute() == null) {
				needsRouter = true;
			}
		}
		if (needsRouter) {
			this.router.run(plan);
		}
	}

}
