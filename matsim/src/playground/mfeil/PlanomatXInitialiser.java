/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatXInitialiser.java
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

import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.scoring.*;

/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to initialise the PlanomatX module.
 */

public class PlanomatXInitialiser extends MultithreadedModuleA{
	
	private final LegTravelTimeEstimator 	estimator;
	private final PreProcessLandmarks 		preProcessRoutingData;
	private final NetworkLayer 				network;
	private final TravelCost 				travelCostCalc;
	private final TravelTime 				travelTimeCalc;
	private final ScoringFunctionFactory 	factory;

	
	public PlanomatXInitialiser (final ControlerTest controlerTest, final LegTravelTimeEstimator estimator) {
		
		this.estimator = estimator;
		preProcessRoutingData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		network = controlerTest.getNetwork();
		preProcessRoutingData.run(network);
		travelCostCalc = controlerTest.getTravelCostCalculator();
		travelTimeCalc = controlerTest.getTravelTimeCalculator();
		//factory = Gbl.getConfig().planomat().getScoringFunctionFactory();//TODO @MF: Check whether this is correct (Same scoring function as for Planomat)!
		factory = new CharyparNagelScoringFunctionFactory();
		}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		PlanAlgorithm planomatXAlgorithm = null;
		planomatXAlgorithm =  new PlanomatX4 (this.estimator, this.network, this.travelCostCalc, 
				this.travelTimeCalc, this.preProcessRoutingData, this.factory);

		return planomatXAlgorithm;
	}
}
