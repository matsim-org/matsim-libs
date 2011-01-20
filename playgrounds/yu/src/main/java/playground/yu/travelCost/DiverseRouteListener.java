/* *********************************************************************** *
 * project: org.matsim.*
 * DiverseRouteListener.java
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

import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;

import playground.yu.replanning.ControlerWithRemoveOldestPlan;

/**
 * switch TravelCostCalculatorFactory evetually also PersonalizableTravelCost
 * before Replanning only with ReRoute to create diverse routes
 * 
 * @author yu
 * 
 */
public class DiverseRouteListener implements IterationStartsListener,
		IterationEndsListener {

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler ctl = event.getControler();
		int iter = event.getIteration();/* firstIter+1, +2, +3 */
		int firstIter = ctl.getFirstIteration();
		ctl
				.setTravelCostCalculatorFactory(new ParameterizedTravelCostCalculatorFactoryImpl(
						1d - (iter - firstIter - 1) / 3d/* A -> travelTime */,
						0d + (iter - firstIter - 1) / 3d/* B -> travelDistance */));
	}

	/**
	 * changes the value of monetaryDistanceCostRateCar from default value 0 to
	 * -0.00012 by the end of the first iteration
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		Controler ctl = event.getControler();
		int iter = event.getIteration();/* firstIter+1, +2, +3 */
		if (iter == ctl.getFirstIteration()) {
			PlanCalcScoreConfigGroup scoringCfg = ctl.getConfig()
					.charyparNagelScoring();
			scoringCfg.setMonetaryDistanceCostRateCar(-0.00012);
			ctl
					.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactory(
							scoringCfg));
		}
	}

	public static void main(String[] args) {
		Controler controler = new ControlerWithRemoveOldestPlan(args);
		controler.addControlerListener(new DiverseRouteListener());
		controler.setWriteEventsInterval(0);
		controler.setCreateGraphs(false);
		controler.run();
	}

}
