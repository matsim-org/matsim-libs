/* *********************************************************************** *
 * project: org.matsim.*
 * ConflictSolverTestsFixture.java
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
package org.matsim.contrib.socnetsim.framework.replanning.selectors.coalitionselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import org.matsim.core.utils.collections.MapUtils;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.ScoreWeight;

/**
 * @author thibautd
 */
public class ConflictSolverTestsFixture {
	public final JointPlans jointPlans;
	public final ReplanningGroup replanningGroup;
	public final Set<Plan> expectedUnfeasiblePlans;
	public final CoalitionSelector.RecordsOfJointPlan recordsPerJointPlan;
	public final Collection<PlanRecord> allRecords = new ArrayList<PlanRecord>();

	public ConflictSolverTestsFixture(
			final JointPlans jointPlans,
			final ReplanningGroup group,
			final Collection<? extends Plan> expectedUnfeasiblePlans) {
		this.jointPlans = jointPlans;
		this.replanningGroup = group;
		this.expectedUnfeasiblePlans = new HashSet<>( expectedUnfeasiblePlans );
		this.recordsPerJointPlan = new CoalitionSelector.RecordsOfJointPlan( jointPlans );

		// create agents
		for ( Person person : group.getPersons() ) {
			final PointingAgent agent =
				new PointingAgent(
						person,
						group,
						new ScoreWeight() );

			for ( PlanRecord r : agent.getRecords() ) {
				allRecords.add( r );
				recordsPerJointPlan.addRecord( r );
			}
		}
	}
}

