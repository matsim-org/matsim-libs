/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatOptimizeTimes.java
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

package org.matsim.core.replanning.modules;

import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.planomat.Planomat;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * This class is just a multithreading wrapper for instances of the
 * optimizing plan algorithm which is usually called "planomat".
 *
 * @author meisterk
 */
public class PlanomatModule extends AbstractMultithreadedModule {

	private final NetworkLayer network;
	private final Events events;
	private final TravelCost travelCost;
	private final TravelTime travelTime;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final Controler controler;
	private final PlanomatConfigGroup config;
	
	private DepartureDelayAverageCalculator tDepDelayCalc = null;

	public PlanomatModule(
			Controler controler, 
			Events events, 
			NetworkLayer network,
			ScoringFunctionFactory scoringFunctionFactory,
			TravelCost travelCost, 
			TravelTime travelTime) {
		super();
		this.controler = controler;
		this.events = events;
		this.network = network;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.travelCost = travelCost;
		this.travelTime = travelTime;

		this.config = controler.getConfig().planomat();
		
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(
				this.network,
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.events.addHandler(tDepDelayCalc);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {

		/*
		 * TODO In principle it is not required to generate a new instance of a LegTravelTimeEstimator for each planomat instance.
		 * But as long as there are unsynchronized write operations in some code used by the LegTravelTimeEstimator, it is the easiest solution
		 * to use a new instance for each thread.
		 * The code which contains unsynchronized write operations is the router which is used to calculate free speed travel times (for 'pt'-mode legs).
		 * Synchronization would make this code very slow as there are probably very many calls of the router. As it is only a temporary solution,
		 * we simply create one instance per thread.
		 */
		
		PlansCalcRoute routingAlgorithm = (PlansCalcRoute) this.controler.getRoutingAlgorithm(this.travelCost, this.travelTime);
		
		
		
		
		LegTravelTimeEstimator legTravelTimeEstimator = this.config.getLegTravelTimeEstimator(
				this.travelTime, 
				this.tDepDelayCalc, 
				routingAlgorithm);

		PlanAlgorithm planomatInstance =  new Planomat(legTravelTimeEstimator, this.scoringFunctionFactory, this.config);

		return planomatInstance;

	}

}
