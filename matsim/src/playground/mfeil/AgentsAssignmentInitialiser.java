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

public class AgentsAssignmentInitialiser extends MultithreadedModuleA{
	
	
	private final NetworkLayer 				network;
	private final Controler					controler;
	private final LegTravelTimeEstimator 	estimator;
	private final PreProcessLandmarks		preProcessRoutingData;
	private final LocationMutatorwChoiceSet locator;
	private final PlanAlgorithm				timer;
	private final ScheduleCleaner				cleaner;
	private final RecyclingModule			module;
	private final double					minimumTime;

		
	public AgentsAssignmentInitialiser (final Controler controler, 
			final PreProcessLandmarks preProcessRoutingData,
			final LegTravelTimeEstimator estimator,
			final LocationMutatorwChoiceSet locator,
			final PlanAlgorithm timer,
			final ScheduleCleaner cleaner,
			final RecyclingModule module, 
			final double minimumTime) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.estimator = estimator;
		this.preProcessRoutingData = preProcessRoutingData;	
		this.locator = locator;
		this.timer = timer;
		this.cleaner = cleaner;
		this.module = module;
		this.minimumTime = minimumTime;
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm agentsAssigner;
		
		agentsAssigner = new AgentsAssigner (this.controler, this.preProcessRoutingData, this.estimator,
					this.locator, this.timer, this.cleaner, this.module, this.minimumTime);
		
		return agentsAssigner;
	}
}
