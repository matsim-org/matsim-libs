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

import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.modules.*;
import org.matsim.core.scoring.PlanScorer;
import org.matsim.core.scoring.ScoringFunctionFactory;


/**
 * @author Matthias Feil
 * Initialiser for TimeOptimizer module.
 */

public class TmcInitialiser extends AbstractMultithreadedModule{
	
	private final PlanScorer 				scorer;
	private final LegTravelTimeEstimator	estimator;

	
	public TmcInitialiser (Controler controler, ScoringFunctionFactory factory) {
		this.scorer = new PlanScorer(factory);
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(
				controler.getNetwork(), 
				controler.getTraveltimeBinSize());
		this.estimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
				controler.getTravelTimeCalculator(), 
				controler.getTravelCostCalculator(), 
				tDepDelayCalc, 
				controler.getNetwork());
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {		

		PlanAlgorithm timeOptAlgorithm = new TimeModeChoicer1 (this.estimator, this.scorer);
		return timeOptAlgorithm;
	}
}
