/* *********************************************************************** *
 * project: org.matsim.*
 * PersonAlgorithm.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Plan;

/**
 * @author balmermi
 *
 */
public class MultiplePlanAlgorithmRunner implements PlanAlgorithm {

	private final List<PlanAlgorithm> planAlgorithms = new ArrayList<PlanAlgorithm>();
	
	public final boolean add(PlanAlgorithm planAlgorithm) {
		if (planAlgorithm == null) { return false; }
		planAlgorithms.add(planAlgorithm);
		return true;
	}
	
	@Override
	public void run(Plan plan) {
		for (PlanAlgorithm planAlgorithm : planAlgorithms) {
			planAlgorithm.run(plan);
		}
	}
}
