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

import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * @author Matthias Feil
 * Initialiser for TimeOptimizer module.
 */

public class TimeOptimizerInitialiser extends AbstractMultithreadedModule{
	
	private final Controler							controler;
	private final DepartureDelayAverageCalculator 	tDepDelayCalc;
	private final NetworkImpl 						network;

	
	public TimeOptimizerInitialiser (Controler controler) {
		super(controler.getConfig().global());
		this.network = controler.getNetwork();
		this.init(network);
		this.controler = controler;
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network, controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
	}
	
	private void init(final NetworkImpl network) {
		this.network.connect();
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {		

		//PlanAlgorithm timeOptAlgorithm = new TimeOptimizerPerformanceT (this.controler, this.estimator, this.scorer, this.factory);
		PlanAlgorithm timeOptAlgorithm = new TimeOptimizer (this.controler, this.tDepDelayCalc);

		return timeOptAlgorithm;
	}
}
