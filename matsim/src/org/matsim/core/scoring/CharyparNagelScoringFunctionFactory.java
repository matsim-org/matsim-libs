/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelScoringFunctionFactory.java
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

import org.matsim.core.api.population.Plan;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;

/**
 * A factory to create scoring functions as described by D. Charypar and K. Nagel.
 * 
 * <blockquote>
 *  <p>Charypar, D. und K. Nagel (2005) <br>
 *  Generating complete all-day activity plans with genetic algorithms,<br>
 *  Transportation, 32 (4) 369–397.</p>
 * </blockquote>
 *
 * @author mrieser
 */
public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	private org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory factory=null;
	
	public CharyparNagelScoringFunctionFactory(final CharyparNagelScoringConfigGroup config) {
		this.factory = new org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory(config);
	}
	
	public ScoringFunction getNewScoringFunction(final Plan plan) {
		return this.factory.getNewScoringFunction(plan);
	}

}
