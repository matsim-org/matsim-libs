/* *********************************************************************** *
 * project: org.matsim.*
 * PlanOptimizeTimesTest.java
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

package org.matsim.planomat;

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.IntegerGene;
import org.matsim.basic.v01.IdImpl;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.interfaces.basic.v01.Id;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.interfaces.core.v01.Population;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.PopulationImpl;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.CRCChecksum;

public class PlanomatTest extends MatsimTestCase {

	private enum PlanomatTestRun {NOEVENTS_CAR, WITHEVENTS_CAR, NOEVENTS_CAR_PT, WITHEVENTS_CAR_PT;}

	private final static Id TEST_PERSON_ID = new IdImpl("100");
	
	private static final Logger log = Logger.getLogger(PlanomatTest.class);

	private NetworkLayer network = null;
	private Facilities facilities = null;
	private Population population = null;

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

	public void testRunDefault() {
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR);
	}

	public void testRunDefaultWithEvents() {
		this.runATestRun(PlanomatTestRun.WITHEVENTS_CAR);
	}

	public void testRunCarPt() {
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR_PT);
	}

	public void testRunCarPtWithEvents() {
		this.runATestRun(PlanomatTestRun.WITHEVENTS_CAR_PT);
	}

	public void testRunDefaultManyModes() {

		Gbl.getConfig().plans().setInputFile(this.getInputDirectory() + "input_plans.xml.gz");

		log.info("Reading plans xml file...");
		this.population = new PopulationImpl(PopulationImpl.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(this.population, this.network);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		this.population.printPlansCount();
		log.info("Reading plans xml file...done.");

		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR);
	}

	private void runATestRun(final PlanomatTestRun testRun) {

		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(this.network, 900);
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(this.network, 900);

		Events events = new Events();
		events.addHandler(tTravelEstimator);
		events.addHandler(depDelayCalc);

		LegTravelTimeEstimator ltte = new CetinCompatibleLegTravelTimeEstimator(tTravelEstimator, travelCostEstimator, depDelayCalc, this.network);
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(Gbl.getConfig().charyparNagelScoring());

		Planomat testee = new Planomat(ltte, scoringFunctionFactory);
		testee.getSeedGenerator().setSeed(Gbl.getConfig().global().getRandomSeed());

		log.info("Testing " + testRun.toString() + "...");

		if (
				PlanomatTestRun.NOEVENTS_CAR_PT.equals(testRun) ||
				PlanomatTestRun.WITHEVENTS_CAR_PT.equals(testRun)) {

			Gbl.getConfig().planomat().setPossibleModes("car,pt");
		}

		tTravelEstimator.resetTravelTimes();
		depDelayCalc.resetDepartureDelays();
		if (
				PlanomatTestRun.WITHEVENTS_CAR.equals(testRun) ||
				PlanomatTestRun.WITHEVENTS_CAR_PT.equals(testRun)) {

			new MatsimEventsReader(events).readFile(this.getClassInputDirectory() + "equil-times-only-1000.events.txt.gz");

		}

		// init test Plan
		
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = this.population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		// actual test
		testee.run(testPlan);

		// write out the test person and the modified plan into a file
		Population outputPopulation = new PopulationImpl();
		outputPopulation.addPerson(testPerson);

		System.out.println("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(outputPopulation, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		System.out.println("Writing plans file...DONE.");

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Expected checksum: " + Long.toString(expectedChecksum));
		log.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);

		log.info("Testing " + testRun.toString() + "...done.");
	}

	public void testInitSampleChromosome() {

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = this.population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		Configuration jgapConfiguration = new Configuration();

		IChromosome testChromosome = null;

		Planomat testee = new Planomat(null, null);

		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(testPlan);

		testChromosome = testee.initSampleChromosome(testPlan, planAnalyzeSubtours, jgapConfiguration);
		assertEquals(2, testChromosome.getGenes().length);
		assertEquals(IntegerGene.class, testChromosome.getGenes()[0].getClass());
		assertEquals(IntegerGene.class, testChromosome.getGenes()[1].getClass());

	}

	public void testWriteChromosome2Plan() {

		// writeChromosome2Plan() has 3 arguments:
		Plan testPlan = null;
		IChromosome testChromosome = null;
		LegTravelTimeEstimator ltte = null;

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = this.population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		// init IChromosome (from JGAP)
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(testPlan);
		int numActs = planAnalyzeSubtours.getSubtourIndexation().length;

		Configuration jgapConfiguration = new Configuration();

		try {
			Gene[] testGenes = new Gene[numActs + planAnalyzeSubtours.getNumSubtours()];

			for (int ii=0; ii < testGenes.length; ii++) {
				switch(ii) {
				case 0:
					testGenes[ii] = new IntegerGene(jgapConfiguration);
					testGenes[ii].setAllele(31);
					break;
				case 1:
					testGenes[ii] = new IntegerGene(jgapConfiguration);
					testGenes[ii].setAllele(32);
					break;
				case 2:
					testGenes[ii] = new IntegerGene(jgapConfiguration);
					testGenes[ii].setAllele(0);
					break;
				}

			}

			testChromosome = new Chromosome(jgapConfiguration, testGenes);

		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}

		// init LegTravelTimeEstimator
		TravelTime tTravelEstimator = new LinearInterpolatingTTCalculator(this.network, 900);
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(this.network, 900);
		ltte = new CharyparEtAlCompatibleLegTravelTimeEstimator(tTravelEstimator, travelCostEstimator, depDelayCalc, this.network);

		// run the method
		Planomat testee = new Planomat(ltte, null);

		testee.writeChromosome2Plan(testChromosome, testPlan, planAnalyzeSubtours);

		// write out the test person and the modified plan into a file
		Population outputPopulation = new PopulationImpl();
		outputPopulation.addPerson(testPerson);

		System.out.println("Writing plans file...");
		PopulationWriter plans_writer = new PopulationWriter(outputPopulation, this.getOutputDirectory() + "output_plans.xml.gz", "v4");
		plans_writer.write();
		System.out.println("Writing plans file...DONE.");

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Expected checksum: " + Long.toString(expectedChecksum));
		log.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);

	}


	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.population = null;
		this.network = null;
		this.facilities = null;
	}

}
