/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningModule.java
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

package playground.christoph.withinday.replanning.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.multimodalsimengine.router.MultiModalPlansCalcRoute;

public class ReplanningModule extends AbstractMultithreadedModule {

	protected Config config;
	protected Network network;
	protected PersonalizableTravelCost costCalculator;
	protected PersonalizableTravelTime timeCalculator;
	protected LeastCostPathCalculatorFactory factory;
	
	public ReplanningModule(Config config, Network network, 
			PersonalizableTravelCost costCalculator, PersonalizableTravelTime timeCalculator, 
			LeastCostPathCalculatorFactory factory) {
		super(config.global());
		
		this.config = config;
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.factory = factory;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		if (config.multiModal().isMultiModalSimulationEnabled()) {
			return new MultiModalPlansCalcRoute(config.plansCalcRoute(), config.multiModal(), 
					network, costCalculator, timeCalculator, factory);				
		} else return new PlansCalcRoute(config.plansCalcRoute(), network, costCalculator, timeCalculator, factory);
	}
}