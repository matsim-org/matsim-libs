/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionFactory.java
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

import org.matsim.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.interfaces.core.v01.Plan;

/**
 * Generates {@link CharyparNagelOpenTimesScoringFunction}s.
 * 
 * @author meisterk
 *
 */
public class CharyparNagelOpenTimesScoringFunctionFactory implements ScoringFunctionFactory {

	private final CharyparNagelScoringParameters params;
	
	public CharyparNagelOpenTimesScoringFunctionFactory(final CharyparNagelScoringConfigGroup config) {
		this.params = new CharyparNagelScoringParameters(config);
	}

	public ScoringFunction getNewScoringFunction(Plan plan) {
		return new CharyparNagelOpenTimesScoringFunction(plan, this.params);
	}

	
	
}
