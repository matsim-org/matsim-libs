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
package org.matsim.contrib.socnetsim.framework.replanning.grouping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.framework.population.PlanWithCachedJointPlan;

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
	private final List<JointPlan> jointPlans;
	private final Collection<JointPlan> unmodifiableJointPlans;
	private final List<Plan> individualPlans;
	private final Collection<Plan> unmodifiablePlans;

	public GroupPlans() {
		this(
				Collections.<JointPlan>emptyList(),
				Collections.<Plan>emptyList() );
	}

	public GroupPlans(
			final Collection<JointPlan> jointPlans,
			final Collection<Plan> individualPlans) {
		this.jointPlans = new ArrayList<JointPlan>(jointPlans);
		this.unmodifiableJointPlans = Collections.unmodifiableList( this.jointPlans );
		this.individualPlans = new ArrayList<Plan>(individualPlans);
		this.unmodifiablePlans = Collections.unmodifiableList( this.individualPlans );
	}

	public Collection<JointPlan> getJointPlans() {
		return this.unmodifiableJointPlans;
	}

	public Collection<Plan> getIndividualPlans() {
		return this.unmodifiablePlans;
	}

	public Collection<Plan> getAllIndividualPlans() {
		final Collection<Plan> plans = new ArrayList<Plan>();

		plans.addAll( individualPlans );
		for ( JointPlan jp : jointPlans ) {
			plans.addAll( jp.getIndividualPlans().values() );
		}

		return plans;
	}

	public void addJointPlan(final JointPlan jp) {
		jointPlans.add( jp );
	}

	public void removeJointPlan(final JointPlan jp) {
		jointPlans.remove( jp );
	}

	public void addJointPlans(final Collection<JointPlan> jps) {
		jointPlans.addAll( jps );
	}

	public void addIndividualPlan(final Plan p) {
		individualPlans.add( p );
	}

	public void removeIndividualPlan(final Plan p) {
		individualPlans.remove( p );
	}

	public void addIndividualPlans(final Collection<Plan> ps) {
		individualPlans.addAll( ps );
	}

	public void clear() {
		jointPlans.clear();
		individualPlans.clear();
	}

	public void clearJointPlans() {
		jointPlans.clear();
	}

	public void clearIndividualPlans() {
		individualPlans.clear();
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
			Plan newPlan = JointPlanFactory.createIndividualPlan( p.getPerson() );
			((PlanWithCachedJointPlan)newPlan).copyFrom( p );
			// I think that the above cast will now fail.  It probably worked originally, since JointPlan was an extension of PlanImpl, which is
			// no longer allowed. I would, however, say that it was not a clean copy anyways, since it ignored the additional fields
			// of JointPlan.  Thibaut, I am confident that you can resolve this if you encounter it, but please let me know if you want to dicuss.
			// kai, nov'15
			
			p.getPerson().addPlan( newPlan );
			ps.add( newPlan );
		}

		return new GroupPlans( jps , ps );
	}
}

