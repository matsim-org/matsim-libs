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

package playground.jhackney.scoring;

import org.matsim.population.Plan;
import org.matsim.scoring.ScoringFunction;
import org.matsim.scoring.ScoringFunctionFactory;

/**
 * A factory to create {@link CharyparNagelScoringFunction}s.
 *
 * @author mrieser
 */
public class CharyparNagelScoringFunctionFactory implements ScoringFunctionFactory {

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.scoring.ScoringFunctionFactory#getNewScoringFunction(org.matsim.demandmodeling.plans.Plan)
	 */
	public playground.jhackney.scoring.CharyparNagelScoringFunction getNewScoringFunction(final Plan plan) {
		return new playground.jhackney.scoring.CharyparNagelScoringFunction(plan);
	}

}


