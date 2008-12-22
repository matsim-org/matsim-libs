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



/**
 * @author Matthias Feil
 * Replacing the PlanomatOptimizeTimes class to initialise the PlanomatX module.
 */

public class PlanomatX12Initialiser extends MultithreadedModuleA{
	
	
	private final NetworkLayer 				network;
	private final Controler					controler;
	private final PreProcessLandmarks		preProcessRoutingData;
	private final LocationMutatorwChoiceSet locator;

	
	
	public PlanomatX12Initialiser (final ControlerMFeil controler) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.preProcessRoutingData = null;		
		this.locator = new LocationMutatorwChoiceSet(controler.getNetwork(), controler);
	
	}
	
	public PlanomatX12Initialiser (final ControlerMFeil controler, 
			final PreProcessLandmarks preProcessRoutingData,
			final LocationMutatorwChoiceSet locator) {
		
		this.network = controler.getNetwork();
		this.controler = controler;
		this.init(network);
		this.preProcessRoutingData = preProcessRoutingData;	
		this.locator = locator;
	}
	
	private void init(final NetworkLayer network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm planomatXAlgorithm;
		if (this.preProcessRoutingData!=null){
			planomatXAlgorithm = new PlanomatX18 (this.controler, this.preProcessRoutingData, 
					this.locator);
		}		
		else {
			planomatXAlgorithm =  new PlanomatX18 (this.controler, this.locator);
		}

		return planomatXAlgorithm;
	}
}
