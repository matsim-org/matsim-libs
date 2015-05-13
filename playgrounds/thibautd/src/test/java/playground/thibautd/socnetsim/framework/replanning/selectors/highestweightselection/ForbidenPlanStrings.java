/* *********************************************************************** *
 * project: org.matsim.*
 * ForbidenPlanStrings.java
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
package playground.thibautd.socnetsim.framework.replanning.selectors.highestweightselection;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;

/**
 * @author thibautd
 */
public class ForbidenPlanStrings {
	private final List<GroupPlans> forbidden = new ArrayList<GroupPlans>();

	public void forbid(final GroupPlans plans) {
		forbidden.add( plans );
	}

	public boolean isForbidden(final PlanString ps) {
		for (GroupPlans p : forbidden) {
			if ( forbids( p , ps ) ) return true;
		}
		return false;
	}

	public boolean isForbidden(final GroupPlans groupPlans) {
		return forbidden.contains( groupPlans );
	}

	private static boolean forbids(
			final GroupPlans forbidden,
			final PlanString string) {
		PlanString tail = string;

		// check if all plans in the string are in the groupPlans
		// copying the list and removing the elements is much faster
		// than using "contains" on big lists.
		final List<Plan> plans = new ArrayList<Plan>( forbidden.getIndividualPlans() );
		while (tail != null) {
			final PlanRecord head = tail.planRecord;
			tail = tail.tail;

			if (head.jointPlan != null &&
					!forbidden.getJointPlans().contains( head.jointPlan )) {
				return false;
			}

			if (head.jointPlan == null &&
					!plans.remove( head.plan )) {
				assert !forbidden.getIndividualPlans().contains( head.plan ) : "planString contains duplicates";
				return false;
			}
		}

		return true;
	}
}
