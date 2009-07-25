/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatModuleTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.modules;

import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.Events;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatModuleTest extends MatsimTestCase {

	private ScenarioImpl scenario = null;

	@Override
	protected void setUp() throws Exception {

		super.setUp();
		Config config = super.loadConfig(this.getClassInputDirectory() + "config.xml");

		this.scenario = new ScenarioImpl(config);
		
		new MatsimFacilitiesReader(scenario.getActivityFacilities()).readFile(config.facilities().getInputFile());
		new MatsimNetworkReader(scenario.getNetwork()).readFile(config.network().getInputFile());
		new MatsimPopulationReader(scenario.getPopulation(), scenario.getNetwork()).readFile(config.plans().getInputFile());
	}

	public void testGenerateRandomDemand() {
		Config config = scenario.getConfig();
		final int TEST_PLAN_NR = 0;

		// the planomat can be used to generate random demand with respect to the dimensions that are optimized by it
		// in the following way:
		// - set the population size to 1, so there is no sample of the initial random solutions the best individual would be chosen of
		config.planomat().setPopSize(1);
		// - set the number of generations to 0 (so only the random initialization, and no optimization takes place)
		config.planomat().setJgapMaxGenerations(0);
		// - set possible modes such that a scenario consisting only of "car" and "pt" modes is generated
		config.planomat().setPossibleModes("car,pt");

		Events emptyEvents = new Events();
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(scenario.getNetwork(), config.travelTimeCalculator());
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, config.charyparNagelScoring());
		
		Controler dummyControler = new Controler(this.scenario);
		dummyControler.setLeastCostPathCalculatorFactory(new DijkstraFactory());
		
		PlanomatModule testee = new PlanomatModule(
				dummyControler, 
				emptyEvents, 
				(NetworkLayer) this.scenario.getNetwork(), 
				scoringFunctionFactory, 
				travelCostEstimator, 
				tTravelEstimator);
		
		testee.prepareReplanning();
		for (PersonImpl person : this.scenario.getPopulation().getPersons().values()) {

			PlanImpl plan = person.getPlans().get(TEST_PLAN_NR);
			testee.handlePlan(plan);
			
		}
		testee.finishReplanning();
		
		PopulationWriter plans_writer = new PopulationWriter(this.scenario.getPopulation(), this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "output_plans.xml.gz");
//		assertEquals("different plans files.", expectedChecksum, actualChecksum);
	}
	
	
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.scenario = null;
	}

}
