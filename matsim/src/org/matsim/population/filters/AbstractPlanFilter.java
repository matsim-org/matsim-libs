/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractPlanFilter.java.java
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

package org.matsim.population.filters;

import org.matsim.core.population.PlanImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

public abstract class AbstractPlanFilter implements PlanFilter, PlanAlgorithm {

	protected PlanAlgorithm nextAlgorithm = null;
	private int count = 0;

	abstract public boolean judge(PlanImpl plan);

	public void run(final PlanImpl plan) {
		if (judge(plan)) {
			count();
			this.nextAlgorithm.run(plan);
		}
	}

	public void count() {
		this.count++;
	}

	public int getCount() {
		return this.count;
	}

}
