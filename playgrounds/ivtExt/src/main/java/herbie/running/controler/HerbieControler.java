/* *********************************************************************** *
 * project: org.matsim.*
 * KTIControler.java
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

package herbie.running.controler;

import herbie.running.config.HerbieConfigGroup;
import herbie.running.controler.listeners.CalcLegTimesKTIListener;
import herbie.running.controler.listeners.KtiPopulationPreparation;
import herbie.running.controler.listeners.LegDistanceDistributionWriter;
import herbie.running.controler.listeners.ScoreElements;
import herbie.running.scoring.HerbieScoringFunctionFactory;
import herbie.running.scoring.HerbieTravelCostCalculatorFactory;

import org.matsim.core.controler.Controler;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.locationchoice.facilityload.FacilitiesLoadCalculator;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Controler for the Herbie project.
 */
public class HerbieControler extends Controler {

	protected static final String SCORE_ELEMENTS_FILE_NAME = "scoreElementsAverages.txt";
	protected static final String CALC_LEG_TIMES_KTI_FILE_NAME = "calcLegTimesKTI.txt";
	protected static final String LEG_DISTANCE_DISTRIBUTION_FILE_NAME = "legDistanceDistribution.txt";
	protected static final String LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME = "legTravelTimeDistribution.txt";

	private final HerbieConfigGroup ktiConfigGroup = new HerbieConfigGroup();

	public HerbieControler(String[] args) {
		super(args);
		super.config.addModule(HerbieConfigGroup.GROUP_NAME, this.ktiConfigGroup);
	}

	@Override
	protected void loadData() {
		super.loadData();
		this.network = this.scenarioData.getNetwork();
		this.population = this.scenarioData.getPopulation();
		this.scenarioLoaded = true;
	}

	@Override
	protected void setUp() {
		HerbieScoringFunctionFactory herbieScoringFunctionFactory = new HerbieScoringFunctionFactory(
				super.config,
				this.ktiConfigGroup,
				this.getFacilityPenalties(),
				this.getFacilities());
		this.setScoringFunctionFactory(herbieScoringFunctionFactory);

		HerbieTravelCostCalculatorFactory costCalculatorFactory = new HerbieTravelCostCalculatorFactory();
		this.setTravelCostCalculatorFactory(costCalculatorFactory);

		super.setUp();
	}


	@Override
	protected void loadControlerListeners() {
		super.loadControlerListeners();
		// the scoring function processes facility loads
		this.addControlerListener(new FacilitiesLoadCalculator(this.getFacilityPenalties()));
		this.addControlerListener(new ScoreElements(SCORE_ELEMENTS_FILE_NAME));
		this.addControlerListener(new CalcLegTimesKTIListener(CALC_LEG_TIMES_KTI_FILE_NAME, LEG_TRAVEL_TIME_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new LegDistanceDistributionWriter(LEG_DISTANCE_DISTRIBUTION_FILE_NAME));
		this.addControlerListener(new KtiPopulationPreparation(this.ktiConfigGroup));
	}

	@Override
	public PlanAlgorithm createRoutingAlgorithm(final PersonalizableTravelCost travelCosts, final PersonalizableTravelTime travelTimes) {
		PlanAlgorithm router = null;
		router = super.createRoutingAlgorithm(travelCosts, travelTimes);
		return router;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if ((args == null) || (args.length == 0)) {
			System.out.println("No argument given!");
			System.out.println("Usage: Controler config-file [dtd-file]");
			System.out.println();
		} else {
			final Controler controler = new HerbieControler(args);
			controler.run();
		}
		System.exit(0);
	}
}
