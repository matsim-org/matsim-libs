/* *********************************************************************** *
 * project: org.matsim.*
 * TimeOptInitialiser.java
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
package playground.mfeil;

import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;


/**
 * @author Matthias Feil
 * Initialiser for time opt module.
 */

public class TimeOptInitialiser extends MultithreadedModuleA{
	
	private final ScoringFunctionFactory 	factory;

	
	public TimeOptInitialiser () {
		this.factory = new CharyparNagelScoringFunctionFactory();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		

		PlanAlgorithm timeOptAlgorithm = new TimeOptimizer (this.factory);

		return timeOptAlgorithm;
	}
}
