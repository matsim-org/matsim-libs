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

import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.scoring.PlanScorer;
import org.matsim.controler.Controler;


/**
 * @author Matthias Feil
 * Initialiser for TimeOptimizer module.
 */

public class TimeOptInitialiser extends MultithreadedModuleA{
	
	private final PlanScorer 				scorer;
	private final LegTravelTimeEstimator	estimator;
	private final ScoringFunctionFactory	factory;
	private final Controler					controler;

	
	public TimeOptInitialiser (Controler controler, LegTravelTimeEstimator estimator, ScoringFunctionFactory factory) {
		this.scorer = new PlanScorer(factory);
		this.estimator = estimator;
		this.factory = factory;
		this.controler = controler;
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {		

		PlanAlgorithm timeOptAlgorithm = new TimeOptimizerPerformanceT (this.controler, this.estimator, this.scorer, this.factory);
		//PlanAlgorithm timeOptAlgorithm = new TimeOptimizer14 (this.estimator, this.scorer);

		return timeOptAlgorithm;
	}
}
