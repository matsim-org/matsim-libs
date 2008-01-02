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

import org.matsim.controler.Controler;
import org.matsim.events.handler.EventHandlerI;
import org.matsim.gbl.Gbl;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.planomat.costestimators.MyRecentEventsBasedEstimator;
import org.matsim.replanning.StrategyManager;
import org.matsim.router.util.TravelTimeI;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;

public class PlanomatControler extends Controler {

	private TravelTimeI linkTravelTimeCalculatorForPlanomat = null;
	private LegTravelTimeEstimator legTravelTimeEstimator = null;
	private DepartureDelayAverageCalculator tDepDelayCalc = null;

	@Override
	protected void setupIteration(int iteration) {
		super.setupIteration(iteration);
		this.tDepDelayCalc.reset(iteration);
		((EventHandlerI) this.linkTravelTimeCalculatorForPlanomat).reset(iteration);
	}

	private LegTravelTimeEstimator initLegTravelTimeEstimator(TravelTimeI linkTravelTimeCalculator) {

		LegTravelTimeEstimator estimator = null;

		/* it would be nice to load the estimator via reflection,		 * but if we just use make instead of Eclipse (as usual on a remote server)		 * only classes occurring in the code are compiled, so we do it a classic way. */		String estimatorName = PlanomatConfig.getLegTravelTimeEstimatorName();
		if (estimatorName.equalsIgnoreCase("MyRecentEventsBasedEstimator")) {

			estimator = new MyRecentEventsBasedEstimator();		
			super.events.addHandler((EventHandlerI) estimator);

		} else if (estimatorName.equalsIgnoreCase("CetinCompatibleLegTravelTimeEstimator")) {
			estimator = new CetinCompatibleLegTravelTimeEstimator(linkTravelTimeCalculator, this.tDepDelayCalc);
		} else if (estimatorName.equalsIgnoreCase("CharyparEtAlCompatibleLegTravelTimeEstimator")) {
			estimator = new CharyparEtAlCompatibleLegTravelTimeEstimator(linkTravelTimeCalculator, this.tDepDelayCalc);
		} else {
			Gbl.errorMsg("Invalid name of implementation of LegTravelTimeEstimatorI: " + estimatorName);
		}

		return estimator;
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManager();
		PlanomatStrategyManagerConfigLoader.load(
				Gbl.getConfig(), 
				manager, 
				network, 
				super.travelCostCalculator, 
				super.travelTimeCalculator, 
				this.legTravelTimeEstimator
		);
		return manager;
	}

	@Override
	protected void startup() {
		super.startup();
				this.linkTravelTimeCalculatorForPlanomat = PlanomatControler.initTravelTimeIForPlanomat(this.network);		super.events.addHandler((EventHandlerI) this.linkTravelTimeCalculatorForPlanomat);

		int timeBinSize = 900;
		this.tDepDelayCalc = new DepartureDelayAverageCalculator(this.network, timeBinSize);		super.events.addHandler(tDepDelayCalc);				this.legTravelTimeEstimator = this.initLegTravelTimeEstimator(this.linkTravelTimeCalculatorForPlanomat);
	}
	
	private static TravelTimeI initTravelTimeIForPlanomat(NetworkLayer network) {
		
		TravelTimeI linkTravelTimeEstimator = null;
		
		String travelTimeIName = PlanomatConfig.getLinkTravelTimeEstimatorName();
		
		if (travelTimeIName.equalsIgnoreCase("org.matsim.demandmodeling.events.algorithms.TravelTimeCalculator")) {
			linkTravelTimeEstimator = new TravelTimeCalculatorArray(network);
		} else if (travelTimeIName.equalsIgnoreCase("org.matsim.playground.meisterk.planomat.LinearInterpolatingTTCalculator")) {
			linkTravelTimeEstimator = new LinearInterpolatingTTCalculator(network);
		} else {
			Gbl.errorMsg("Invalid name of implementation of TravelTimeI: " + travelTimeIName);
		}
		
		return linkTravelTimeEstimator;
	} 
	
	public static void main(String[] args) {
		final Controler controler = new PlanomatControler();

		controler.run(args);
		System.exit(0);
	}

}
