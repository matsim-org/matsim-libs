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
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;


/**
 * @author Matthias Feil
 * Initialiser for TimeOptimizer module.
 */

public class TimeModeChoicerInitialiser extends AbstractMultithreadedModule{
	
	private final Controler							controler;
	private final DepartureDelayAverageCalculator 	tDepDelayCalc;

	
	public TimeModeChoicerInitialiser (Controler controler) {
		super(controler.getConfig().global());
		this.controler = controler;		
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(
				controler.getNetwork(),
				controler.getConfig().travelTimeCalculator().getTraveltimeBinSize());
		this.controler.getEvents().addHandler(tDepDelayCalc);
	}

	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {		

		PlanAlgorithm timeOptAlgorithm = new TimeModeChoicer (this.controler, this.tDepDelayCalc);
		return timeOptAlgorithm;
	}
}
