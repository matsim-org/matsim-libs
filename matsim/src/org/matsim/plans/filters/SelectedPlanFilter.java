/* *********************************************************************** *
 * project: org.matsim.*
 * SelectedPlanFilter.java
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

package org.matsim.plans.filters;

import org.matsim.plans.Person;
import org.matsim.plans.algorithms.PlanAlgorithmI;

public class SelectedPlanFilter extends AbstractPersonFilter {

	private final PlanAlgorithmI nextAlgo;

	public SelectedPlanFilter(final PlanAlgorithmI nextAlgo) {
		this.nextAlgo = nextAlgo;
	}

	@Override
	public boolean judge(final Person person) {
		return true;
	}

	@Override
	public void run(final Person person) {
		count();
		this.nextAlgo.run(person.getSelectedPlan());
	}


}
