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

package org.matsim.replanning.modules;

import org.matsim.events.Events;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.Planomat;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.scoring.ScoringFunctionFactory;

/**
 * This class is just a multithreading wrapper for instances of the
 * optimizing plan algorithm which is usually called "planomat".
 *
 * @author meisterk
 */
public class PlanomatModule extends MultithreadedModuleA {

	private NetworkLayer network;
	private Events events;
	private TravelTime travelTime;
	private TravelCost travelCost;
	private ScoringFunctionFactory sf;

	private DepartureDelayAverageCalculator tDepDelayCalc = null;

	public PlanomatModule(NetworkLayer network, Events events,
			TravelTime travelTime, TravelCost travelCost,
			ScoringFunctionFactory sf) {
		super();
		this.network = network;
		this.events = events;
		this.travelTime = travelTime;
		this.travelCost = travelCost;
		this.sf = sf;

		this.tDepDelayCalc = new DepartureDelayAverageCalculator(
				this.network,
				Gbl.getConfig().controler().getTraveltimeBinSize());
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
		LegTravelTimeEstimator legTravelTimeEstimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
				this.travelTime, 
				this.travelCost, 
				this.tDepDelayCalc, 
				this.network);

		PlanAlgorithm planomatInstance = null;
		planomatInstance = new Planomat(
				legTravelTimeEstimator, 
				this.sf);

		return planomatInstance;

	}

}
