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

/**
 * switch TravelCostCalculatorFactory evetually also PersonalizableTravelCost
 * before Replanning only with ReRoute to create diverse routes
 * 
 * @author yu
 * 
 */
public class SingleReRouteSelectedListener implements IterationStartsListener,
		IterationEndsListener {
	private double A;

	public SingleReRouteSelectedListener(double A) {
		this.A = A;
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		Controler ctl = event.getControler();
		if (event.getIteration() > ctl.getFirstIteration()) {
			ctl
					.setTravelCostCalculatorFactory(new ParameterizedTravelCostCalculatorFactoryImpl(
							A/* travelTime */));
		}
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
					.planCalcScore();
			scoringCfg.setMonetaryDistanceCostRateCar(-0.00036);
			ctl
					.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactory(
							scoringCfg));
		}
	}

	// public static void main(String[] args) {
	// Controler controler = new ControlerWithRemoveOldestPlan(args);
	// controler.addControlerListener(new SingleReRouteSelectedListener());
	// controler.setWriteEventsInterval(0);
	// controler.setCreateGraphs(false);
	// controler.run();
	// }

}
