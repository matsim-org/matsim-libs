/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.thibautd.socnetsim.replanning.selectors.whoisthebossselector;

import java.util.Collection;
import java.util.HashSet;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.framework.population.JointPlan;

final class PlanRecord {
	PersonRecord person;
	final Plan plan;
	/**
	 * The joint plan to which pertains the individual plan,
	 * if any.
	 */
	final JointPlan jointPlan;
	/**
	 * the plan records corresponding to the other plans in the joint plan
	 */
	final Collection<PlanRecord> linkedPlans = new HashSet<PlanRecord>();
	final double individualPlanWeight;
	// true if all partners are still unallocated
	boolean isStillFeasible = true;

	public PlanRecord(
			final Plan plan,
			final JointPlan jointPlan,
			final double weight) {
		this.plan = plan;
		this.jointPlan = jointPlan;
		this.individualPlanWeight = weight;
	}

	@Override
	public String toString() {
		return "{PlanRecord: "+plan.getPerson().getId()+":"+plan.getScore()+
			" linkedWith:"+(jointPlan == null ? "[]" : jointPlan.getIndividualPlans().keySet())+
			" weight="+individualPlanWeight+
			" isFeasible="+isStillFeasible+"}";
	}
}
