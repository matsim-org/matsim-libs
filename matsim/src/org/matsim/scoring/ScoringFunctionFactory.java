/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionFactory.java
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

package org.matsim.scoring;

import org.matsim.population.Plan;

/**
 * The <code>ScoringFunctionFactory</code> creates new scoring functions.
 * <br>
 * <br>
 * Note: I think this factory will no longer be needed in this form as soon
 * as every agent has its own instance of a scoring function. [marcel, 21jun07]
 *
 * @author mrieser
 */
public interface ScoringFunctionFactory {

	/**
	 * Creates a new scoring function for the given plan.
	 *
	 * @param plan A reference plan when calculating the score. This plan may be
	 * used by the scoring function to look additional information.
	 * @return A scoring function.
	 */
	public ScoringFunction getNewScoringFunction(final Plan plan);

}
