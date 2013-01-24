/* *********************************************************************** *
 * project: org.matsim.*
 * GroupPlans.java
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
package playground.thibautd.socnetsim.replanning.grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;

/**
 * Stores plans for a group.
 * For plans pertaining to a joint plan, the joint plan is stored;
 * for individual plans, the individual plan is stored.
 * <br>
 * The collections returned are the interal references.
 * GroupStrategyModules may modify them if needed.
 * @author thibautd
 */
public class GroupPlans {
	private final Collection<JointPlan> jointPlans;
	private final Collection<Plan> individualPlans;

	public GroupPlans() {
		this( Collections.EMPTY_LIST , Collections.EMPTY_LIST );
	}

	public GroupPlans(
			final Collection<JointPlan> jointPlans,
			final Collection<Plan> individualPlans) {
		this.jointPlans = new ArrayList<JointPlan>(jointPlans);
		this.individualPlans = new ArrayList<Plan>(individualPlans);
	}

	public Collection<JointPlan> getJointPlans() {
		return jointPlans;
	}

	public Collection<Plan> getIndividualPlans() {
		return individualPlans;
	}

	@Override
	public int hashCode() {
		return jointPlans.hashCode() + individualPlans.hashCode();
	}

	@Override
	public boolean equals(final Object o) {
		return o instanceof GroupPlans &&
			((GroupPlans) o).jointPlans.size() == jointPlans.size() &&
			((GroupPlans) o).jointPlans.containsAll( jointPlans ) &&
			((GroupPlans) o).individualPlans.size() == individualPlans.size() &&
			((GroupPlans) o).individualPlans.containsAll( individualPlans );
	}

	@Override
	public String toString() {
		return "{GroupPlans: jointPlans="+jointPlans+"; individualPlans="+individualPlans+"}";
	}

	public static GroupPlans copyPlans(
			final JointPlanFactory jointPlanFactory,
			final GroupPlans plans) {
		List<JointPlan> jps = new ArrayList<JointPlan>();
		List<Plan> ps = new ArrayList<Plan>();

		for (JointPlan jp : plans.getJointPlans()) {
			jps.add( jointPlanFactory.copyJointPlan( jp ) );
		}

		for (Plan p : plans.getIndividualPlans()) {
			PlanImpl newPlan = new PlanImpl( p.getPerson() );
			newPlan.copyFrom( p );
			p.getPerson().addPlan( newPlan );
			ps.add( newPlan );
		}

		return new GroupPlans( jps , ps );
	}
}

