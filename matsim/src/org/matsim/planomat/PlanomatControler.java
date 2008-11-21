/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControler.java
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

package org.matsim.planomat;

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;

public class PlanomatControler extends Controler {

	public PlanomatControler(Config config) {
		super(config);
	}

	public PlanomatControler(String[] args) {
		super(args);
	}

	@Override
	protected void setup() {
		double endTime = this.config.simulation().getEndTime() > 0 ? this.config.simulation().getEndTime() : 30*3600;
		if (super.travelTimeCalculator == null) {
			super.travelTimeCalculator = super.config.controler().getTravelTimeCalculator(super.network, (int)endTime);
		}
		super.legTravelTimeEstimator = initLegTravelTimeEstimator();
		super.setup();
	}
	
	private LegTravelTimeEstimator initLegTravelTimeEstimator() {

		LegTravelTimeEstimator estimator = null;

		int timeBinSize = 900;
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(super.network, timeBinSize);
		this.events.addHandler(tDepDelayCalc);

		estimator = Gbl.getConfig().planomat().getLegTravelTimeEstimator(
				super.travelTimeCalculator, 
				super.travelCostCalculator, 
				tDepDelayCalc, 
				super.network);
		
		return estimator;
	}

	public static void main(final String[] args) {
		final Controler controler = new PlanomatControler(args);
		controler.run();
	}

}
