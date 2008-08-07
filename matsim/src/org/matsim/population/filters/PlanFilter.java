/* *********************************************************************** *
 * project: org.matsim.*
 * PlanFilter.java.java
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

import org.matsim.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

public interface PlanFilter extends PlanAlgorithm, Filter {

	/**
	 * Judges whether the plan will be selected or not.
	 *
	 * @param plan
	 * @return true if the plan meets the criterion of the filter.
	 */
	boolean judge(Plan plan);

	/**
	 * Sends the person to the next algorithm
	 *
	 * @param plan
	 */
	void run(Plan plan);

}
