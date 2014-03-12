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

package org.matsim.core.scoring;

import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * The <code>ScoringFunctionFactory</code> creates new scoring functions.
 * <br>
 * <br>
 * Design comments:<ul>
 * <li> I think this factory will no longer be needed in this form as soon
 * as every agent has its own instance of a scoring function. [marcel, 21jun07]
 * <li> I find it a bit surprising that the factory obtains a plan as an argument.  I would have thought that the scoring
 * function is attached to the agent (or to all agents), and thus constant over the iterations.  It is, however, recreated
 * in every iteration, and so passing the selected plan makes _some_ sense.  kai, mar'12
 * <li> yyyy Still, it pretends that the plan is always necessary for scoring, which is not only a small oversight, but a true
 * conceptual flaw ... since there is absolutely no reason why agents without plans should not be scoreable.  I my intuition,
 * it would make far more sense to explicitly add a planscorer (similar to legscorer, activityscorer, etc.) into the agent in 
 * such cases.  kai, mar'12 
 * </ul>
 *
 * @author mrieser
 */
public interface ScoringFunctionFactory extends MatsimFactory {

	/**
	 * Creates a new scoring function for the given plan.
	 *
	 * @param person A reference plan when calculating the score. This plan may be
	 * used by the scoring function to look additional information. (But see comment above.  kai, mar'12)
	 * @return A scoring function.
	 */
	public ScoringFunction createNewScoringFunction(final Person person);

}
