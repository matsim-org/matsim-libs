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
import org.matsim.core.api.internal.MatsimExtensionPoint;
import org.matsim.core.api.internal.MatsimFactory;

/**
 * The <code>ScoringFunctionFactory</code> creates new scoring functions.
 * <br>
 * Examples:<ul>
 * <li> {@link tutorial.programming.example16customscoring.RunCustomScoringExample}
 * </ul>
 *
 * @author mrieser
 */
public interface ScoringFunctionFactory extends MatsimFactory, MatsimExtensionPoint {

	/**
	 * Creates a new scoring function for the given plan.
	 *
	 * @param person A reference plan when calculating the score. This plan may be
	 * used by the scoring function to look additional information. (But see comment above.  kai, mar'12)
	 * @return A scoring function.
	 */
	public ScoringFunction createNewScoringFunction(final Person person);

}
