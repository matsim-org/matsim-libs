/* *********************************************************************** *
 * project: org.matsim.*
 * PrepareVehicleAllocationForSimAlgorithm.java
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
package playground.thibautd.socnetsim.sharedvehicles;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.modules.PlanLinkIdentifier;
import playground.thibautd.socnetsim.replanning.modules.RecomposeJointPlanAlgorithm;
import playground.thibautd.socnetsim.sharedvehicles.replanning.AllocateVehicleToPlansInGroupPlanAlgorithm;

/**
 * @author thibautd
 */
public class PrepareVehicleAllocationForSimAlgorithm implements GenericPlanAlgorithm<ReplanningGroup> {
	private final JointPlans jointPlans;

	private final GenericPlanAlgorithm<GroupPlans> allocateVehiclesAlgo;
	private final GenericPlanAlgorithm<GroupPlans> recomposeJointPlansAlgo;

	public PrepareVehicleAllocationForSimAlgorithm(
			final Random random,
			final JointPlans jointPlans,
			final VehicleRessources vehicles,
			final PlanLinkIdentifier planLinkIdentifier) {
		this.jointPlans = jointPlans;

		this.allocateVehiclesAlgo =
			new AllocateVehicleToPlansInGroupPlanAlgorithm(
					random,
					vehicles,
					SharedVehicleUtils.DEFAULT_VEHICULAR_MODES,
					true,
					true);

		this.recomposeJointPlansAlgo =
			planLinkIdentifier == null ? null :
			new RecomposeJointPlanAlgorithm(
					jointPlans.getFactory(),
					planLinkIdentifier);
	}

	@Override
	public void run(final ReplanningGroup group) {
		final Set<Plan> ps = new HashSet<Plan>();
		final Set<JointPlan> jps = new HashSet<JointPlan>();

		for ( Person person : group.getPersons() ) {
			if ( person.getPlans().size() > 1 ) return;
			final Plan plan = person.getSelectedPlan();
			final JointPlan jp = jointPlans.getJointPlan( plan );

			if ( jp != null ) {
				// TODO: check that all members of JP are in group
				jps.add( jp );
			}
			else {
				if ( plan == null ) throw new NullPointerException();
				ps.add( plan );
			}
		}

		final GroupPlans groupPlans = new GroupPlans( jps , ps );
		allocateVehiclesAlgo.run( groupPlans );
		if ( recomposeJointPlansAlgo != null ) {
			recomposeJointPlansAlgo.run( groupPlans );
		}

		for ( JointPlan jp : jps ) jointPlans.removeJointPlan( jp );
		jointPlans.addJointPlans( groupPlans.getJointPlans() );
	}
}

