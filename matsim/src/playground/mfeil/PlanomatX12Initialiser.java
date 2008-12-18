/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatX12Initialiser.java
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

import org.matsim.controler.Controler;
import org.matsim.locationchoice.constrained.LocationMutatorwChoiceSet;
import org.matsim.network.NetworkLayer;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.replanning.modules.*;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;



/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to initialise the PlanomatX module.
 */

public class PlanomatX12Initialiser extends MultithreadedModuleA{
	
	
	private final NetworkLayer 				network;
	private final Controler					controler;
	private final LegTravelTimeEstimator 	estimator;
	private final PreProcessLandmarks		preProcessRoutingData;
	private final LocationMutatorwChoiceSet locator;
	private final PlanAlgorithm				timer;

	
	
	public PlanomatX12Initialiser (final ControlerMFeil controler) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.estimator = null;
		this.preProcessRoutingData = null;		
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler);
		this.timer = null;
	
	}
	
	public PlanomatX12Initialiser (final ControlerMFeil controler, 
			final PreProcessLandmarks preProcessRoutingData,
			final LegTravelTimeEstimator estimator,
			final LocationMutatorwChoiceSet locator,
			final PlanAlgorithm timer) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.estimator = estimator;
		this.preProcessRoutingData = preProcessRoutingData;	
		this.locator = locator;
		this.timer = timer;
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm planomatXAlgorithm;
		if (this.estimator!=null){
			planomatXAlgorithm = new PlanomatX17 (this.controler, this.preProcessRoutingData, this.estimator,
					this.locator, this.timer);
		}		
		else {
			planomatXAlgorithm =  new PlanomatX17 (this.controler, this.locator);
		}

		return planomatXAlgorithm;
	}
}
