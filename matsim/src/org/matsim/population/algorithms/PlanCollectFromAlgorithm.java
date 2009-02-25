/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.population.algorithms;

import java.util.HashSet;
import java.util.Set;

import org.matsim.interfaces.core.v01.Plan;

/**
 * @author dgrether
 */
public class PlanCollectFromAlgorithm implements PlanAlgorithm {

	private Set<Plan> plans;

	public PlanCollectFromAlgorithm() {
		this.plans = new HashSet<Plan>();
	}

	/**
	 * Just collects all plans in a set.
	 * @see org.matsim.population.algorithms.PlanAlgorithm#run(org.matsim.interfaces.core.v01.Plan)
	 */
	public void run(Plan plan) {
		this.plans.add(plan);
	}

	public Set<Plan> getPlans() {
		return this.plans;
	}

}
