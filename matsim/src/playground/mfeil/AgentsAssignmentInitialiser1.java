/* *********************************************************************** *
 * project: org.matsim.*
 * AgentsAssignmentInitialiser.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.matsim.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;



/**
 * @author Matthias Feil
 * Initializes the agentsAssigner.
 */

public class AgentsAssignmentInitialiser1 extends AgentsAssignmentInitialiser {
	
	private final DistanceCoefficients distanceCoefficients;

		
	public AgentsAssignmentInitialiser1 (final Controler controler, 
			final PreProcessLandmarks preProcessRoutingData,
			final LegTravelTimeEstimator estimator,
			final LocationMutatorwChoiceSet locator,
			final PlanAlgorithm timer,
			final ScheduleCleaner cleaner,
			final RecyclingModule module, 
			final double minimumTime,
			final DistanceCoefficients distanceCoefficients) {
		
		super (controler, preProcessRoutingData, estimator, locator, timer,
				cleaner, module, minimumTime);
		this.distanceCoefficients = distanceCoefficients;
	}
	


	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm agentsAssigner;
		
		agentsAssigner = new AgentsAssigner1 (this.controler, this.preProcessRoutingData, this.estimator,
					this.locator, this.timer, this.cleaner, this.module, this.minimumTime, this.distanceCoefficients);
		
		return agentsAssigner;
	}
}
