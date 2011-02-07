/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.anhorni.locationchoice.run;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.anhorni.locationchoice.analysis.PrintShopAndLeisureLocations;
import playground.anhorni.locationchoice.analysis.TravelDistanceDistribution;
import playground.anhorni.locationchoice.analysis.events.CalcLegDistancesListenerDetailed;
import playground.anhorni.locationchoice.analysis.events.CalcLegTimesListenerDetailed;
import playground.anhorni.locationchoice.analysis.plans.CalculatePlanTravelStats;
import playground.anhorni.locationchoice.run.scoring.ScoreElements;
import playground.anhorni.locationchoice.run.scoring.TRBScoringFunctionFactory;

public class TRBControler extends Controler {

	public TRBControler(String[] args) {
		super(args);
	}

	public TRBControler(Config config) {
		super(config);
	}

	public static void main(String[] args) {
		new TRBControler(args).run();
	}

	@Override
	public void run() {
		TRBScoringFunctionFactory trbScoringFunctionFactory =
			new TRBScoringFunctionFactory(this.config.planCalcScore(), this);
		this.setScoringFunctionFactory(trbScoringFunctionFactory);

		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements("scoreElementsAverages.txt"));
		this.addControlerListener(new CalcLegTimesListenerDetailed("calcLegTimes.txt", false));
		this.addControlerListener(new CalcLegTimesListenerDetailed("calcLegTimes_wayThere.txt", true));
		this.addControlerListener(new CalcLegDistancesListenerDetailed("CalcLegDistances_wayThere.txt"));
		this.addControlerListener(new CalculatePlanTravelStats(true));
		this.addControlerListener(new PrintShopAndLeisureLocations());
		this.addControlerListener(new TravelDistanceDistribution());
		super.run();
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		return super.createRoutingAlgorithm(travelCosts, travelTimes);
	}
}
