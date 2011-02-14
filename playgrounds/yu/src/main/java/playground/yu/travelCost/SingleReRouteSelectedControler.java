/* *********************************************************************** *
 * project: org.matsim.*
 * SingleReRouteSelectedControler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.yu.travelCost;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;

import playground.yu.replanning.StrategyManagerWithRouteComparison;
import playground.yu.replanning.TightTurnPenaltyControlerListener;

/**
 * @author yu
 * 
 */
public class SingleReRouteSelectedControler extends Controler {

	/**
	 * @param args
	 */
	public SingleReRouteSelectedControler(String arg) {
		super(arg);

	}

	@Override
	protected StrategyManager loadStrategyManager() {
		// return super.loadStrategyManager();
		StrategyManager manager = new StrategyManagerWithRouteComparison(
				network, getControlerIO().getOutputFilename(
						"pathSizeDistribution."));
		StrategyManagerConfigLoader.load(this, manager);
		return manager;
	}

	/*
	 * program arguments: ../matsimTests/diverseRoutes/A1B0.xml
	 * ../matsimTests/diverseRoutes/A2_3B1.xml
	 * ../matsimTests/diverseRoutes/A1_3B2.xml
	 * ../matsimTests/diverseRoutes/A0B3.xml
	 */
	public static void travelTimeCostWeight(String[] args) {
		for (int i = 0; i <= 3; i++) {
			Controler controler = new SingleReRouteSelectedControler(args[i]);
			controler.addControlerListener(new SingleReRouteSelectedListener(
					(double) (3 - i) / (double) 3, i));
			controler.setWriteEventsInterval(0);
			controler.setOverwriteFiles(true);
			controler.run();
		}
	}

	/* ../matsimTests/diverseRoutes/tightTurnPenalty.xml */
	public static void tightTurnPenalty(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler.addControlerListener(new TightTurnPenaltyControlerListener());
		controler.setWriteEventsInterval(0);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	public static void capacityWeightedTravelTimeCost(String[] args) {
		Controler controler = new SingleReRouteSelectedControler(args[0]);
		controler
				.addControlerListener(new LinkCapacityWeightedTravelCostListener());
		controler.setWriteEventsInterval(0);
		controler.setOverwriteFiles(true);
		controler.run();
	}

	public static void main(String[] args) {
		// travelTimeCostWeight(args);
		// tightTurnPenalty(args);
		capacityWeightedTravelTimeCost(args);
	}
}
