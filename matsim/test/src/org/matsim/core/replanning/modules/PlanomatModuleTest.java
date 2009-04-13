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

import org.apache.log4j.Logger;
import org.matsim.core.api.facilities.Facilities;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.facilities.MatsimFacilitiesReader;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationReader;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatModuleTest extends MatsimTestCase {

	private Config config = null;
	private NetworkLayer network = null;
	private Facilities facilities = null;
	private Population population = null;
	
	private static final Logger log = Logger.getLogger(PlanomatModuleTest.class);

	@Override
	protected void setUp() throws Exception {

		super.setUp();
		this.config = super.loadConfig(this.getClassInputDirectory() + "config.xml");

		log.info("Reading facilities xml file...");
		this.facilities = (Facilities)Gbl.createWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(this.facilities).readFile(config.facilities().getInputFile());
		log.info("Reading facilities xml file...done.");

		log.info("Reading network xml file...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(config.network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		this.population = new PopulationImpl();
		PopulationReader plansReader = new MatsimPopulationReader(this.population, this.network);
		plansReader.readFile(config.plans().getInputFile());
		log.info("Reading plans xml file...done.");

	}

	public void testGenerateRandomDemand() {

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
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(this.network, 900);
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(config.charyparNagelScoring());
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, config.charyparNagelScoring());
		
		PlanomatModule testee = new PlanomatModule(this.network, emptyEvents, tTravelEstimator, travelCostEstimator, scoringFunctionFactory);
		
		testee.prepareReplanning();
		for (Person person : this.population.getPersons().values()) {

			Plan plan = person.getPlans().get(TEST_PLAN_NR);
			testee.handlePlan(plan);
			
		}
		testee.finishReplanning();
		
		System.out.println("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(this.population, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		System.out.println("Writing plans file...DONE.");

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "output_plans.xml.gz");
		assertEquals("different plans files.", expectedChecksum, actualChecksum);

	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.network = null;
		this.facilities = null;
	}

}
