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

package org.matsim.replanning.modules;

import org.apache.log4j.Logger;
import org.matsim.events.Events;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.CRCChecksum;

public class PlanomatModuleTest extends MatsimTestCase {

	private NetworkLayer network = null;
	private Facilities facilities = null;
	private Population population = null;

	private static final Logger log = Logger.getLogger(PlanomatModuleTest.class);

	@Override
	protected void setUp() throws Exception {

		super.setUp();
		super.loadConfig(this.getClassInputDirectory() + "config.xml");

		log.info("Reading facilities xml file...");
		this.facilities = (Facilities)Gbl.createWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(this.facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");

		log.info("Reading network xml file...");
		this.network = new NetworkLayer();
		new MatsimNetworkReader(this.network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		this.population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(this.population, this.network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		this.population.printPlansCount();
		log.info("Reading plans xml file...done.");

	}

	public void testGenerateRandomDemand() {

		final int TEST_PLAN_NR = 0;

		// the planomat can be used to generate random demand with respect to the dimensions that are optimized by it
		// in the following way:
		// - set the population size to 1, so there is no sample of the initial random solutions the best individual would be chosen of
		Gbl.getConfig().planomat().setPopSize(1);
		// - set the number of generations to 0 (so only the random initialization, and no optimization takes place)
		Gbl.getConfig().planomat().setJgapMaxGenerations(0);
		// - set possible modes such that a scenario consisting only of "car" and "pt" modes is generated
		Gbl.getConfig().planomat().setPossibleModes("car,pt");

		Events emptyEvents = new Events();
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(this.network, 900);
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, Gbl.getConfig().charyparNagelScoring());
		
		PlanomatModule testee = new PlanomatModule(this.network, emptyEvents, tTravelEstimator, travelCostEstimator, scoringFunctionFactory);
		
		testee.init();
		for (Person person : this.population) {

			Plan plan = person.getPlans().get(TEST_PLAN_NR);
			testee.handlePlan(plan);
			
		}
		testee.finish();
		
		System.out.println("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(this.population, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		System.out.println("Writing plans file...DONE.");

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Expected checksum: " + Long.toString(expectedChecksum));
		log.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);

	}
}
