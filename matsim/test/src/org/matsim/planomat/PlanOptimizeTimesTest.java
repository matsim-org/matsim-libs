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

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.DoubleGene;
import org.jgap.impl.IntegerGene;
import org.matsim.basic.v01.BasicLeg;
import org.matsim.events.Events;
import org.matsim.events.MatsimEventsReader;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.gbl.Gbl;
import org.matsim.network.MatsimNetworkReader;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.planomat.costestimators.CetinCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.CharyparEtAlCompatibleLegTravelTimeEstimator;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.population.Leg;
import org.matsim.population.MatsimPopulationReader;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.population.PopulationReader;
import org.matsim.population.PopulationWriter;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.population.routes.CarRoute;
import org.matsim.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.scoring.CharyparNagelScoringFunctionFactory;
import org.matsim.scoring.ScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.trafficmonitoring.TravelTimeCalculator;
import org.matsim.utils.CRCChecksum;
import org.matsim.utils.misc.Time;

public class PlanOptimizeTimesTest extends MatsimTestCase {

	private enum PlanomatTestRun {

		NOEVENTS_CAR("noevents_car"),
		WITHEVENTS_CAR("withevents_car"),
		NOEVENTS_CAR_PT("noevents_car_pt"),
		WITHEVENTS_CAR_PT("withevents_car_pt");

		private String testIdentifier;

		private PlanomatTestRun(String testIdentifier) {
			this.testIdentifier = testIdentifier;
		}

		public String getTestIdentifier() {
			return testIdentifier;
		}

	}

	private static final Logger log = Logger.getLogger(PlanOptimizeTimesTest.class);

	private NetworkLayer network = null;
	private Facilities facilities = null;
	private Population population = null;

	protected void setUp() throws Exception {

		super.setUp();
		super.loadConfig(this.getClassInputDirectory() + "config.xml");

		log.info("Reading facilities xml file...");
		facilities = (Facilities)Gbl.getWorld().createLayer(Facilities.LAYER_TYPE,null);
		new MatsimFacilitiesReader(facilities).readFile(Gbl.getConfig().facilities().getInputFile());
		log.info("Reading facilities xml file...done.");

		log.info("Reading network xml file...");
		network = (NetworkLayer)Gbl.getWorld().createLayer(NetworkLayer.LAYER_TYPE, null);
		new MatsimNetworkReader(network).readFile(Gbl.getConfig().network().getInputFile());
		log.info("Reading network xml file...done.");

		log.info("Reading plans xml file...");
		population = new Population(Population.NO_STREAMING);
		PopulationReader plansReader = new MatsimPopulationReader(population);
		plansReader.readFile(Gbl.getConfig().plans().getInputFile());
		population.printPlansCount();
		log.info("Reading plans xml file...done.");

	}

	@Override
	protected void tearDown() throws Exception {
		this.network = null;
		this.facilities = null;
		this.population = null;
		super.tearDown();
	}
	
	public void testRun() {

		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(network, 900);
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(network, 900);

		Events events = new Events();
		events.addHandler(tTravelEstimator);
		events.addHandler(depDelayCalc);

		LegTravelTimeEstimator ltte = new CetinCompatibleLegTravelTimeEstimator(tTravelEstimator, travelCostEstimator, depDelayCalc, network);
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory();

		PlanOptimizeTimes testee = new PlanOptimizeTimes(ltte, scoringFunctionFactory);

		for (PlanomatTestRun planomatTestRun : PlanomatTestRun.values()) {

			log.info("Testing " + planomatTestRun.getTestIdentifier() + "...");
			
			if (
					PlanomatTestRun.NOEVENTS_CAR_PT.getTestIdentifier().equals(planomatTestRun.getTestIdentifier()) || 
					PlanomatTestRun.WITHEVENTS_CAR_PT.getTestIdentifier().equals(planomatTestRun.getTestIdentifier())) {
				
				  Gbl.getConfig().planomat().setPossibleModes(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt});
			}

			tTravelEstimator.resetTravelTimes();
			depDelayCalc.resetDepartureDelays();
			if (
					PlanomatTestRun.WITHEVENTS_CAR.getTestIdentifier().equals(planomatTestRun.getTestIdentifier()) ||
					PlanomatTestRun.WITHEVENTS_CAR_PT.getTestIdentifier().equals(planomatTestRun.getTestIdentifier())) {

				new MatsimEventsReader(events).readFile(this.getInputDirectory() + "equil-times-only-1000.events.txt.gz");

			}

			// init test Plan
			final String TEST_PERSON_ID = "100";
			final int TEST_PLAN_NR = 0;

			// first person
			Person testPerson = population.getPerson(TEST_PERSON_ID);
			// only plan of that person
			Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

			// actual test
			testee.run(testPlan);

			// write out the test person and the modified plan into a file
			Population outputPopulation = new Population();
			outputPopulation.addPerson(testPerson);

			System.out.println("Writing plans file...");
			PopulationWriter plans_writer = new PopulationWriter(outputPopulation, this.getOutputDirectory() + "output_plans_" + planomatTestRun.getTestIdentifier() + ".xml.gz", "v4");
			plans_writer.write();
			System.out.println("Writing plans file...DONE.");

			// actual test: compare checksums of the files
			final long expectedChecksum = CRCChecksum.getCRCFromGZFile(this.getInputDirectory() + "plans_" + planomatTestRun.getTestIdentifier() + ".xml.gz");
			final long actualChecksum = CRCChecksum.getCRCFromGZFile(this.getOutputDirectory() + "output_plans_" + planomatTestRun.getTestIdentifier() + ".xml.gz");
			log.info("Expected checksum: " + Long.toString(expectedChecksum));
			log.info("Actual checksum: " + Long.toString(actualChecksum));
			assertEquals(expectedChecksum, actualChecksum);

			log.info("Testing " + planomatTestRun.getTestIdentifier() + "...done.");

		}
	}

	public void testInitSampleChromosome() {

		// init test Plan
		final String TEST_PERSON_ID = "100";
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		Configuration jgapConfiguration = new Configuration();

		IChromosome testChromosome = null;

		PlanOptimizeTimes testee = new PlanOptimizeTimes(null, null);

		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.run(testPlan);

		testChromosome = testee.initSampleChromosome(planAnalyzeSubtours, jgapConfiguration);
		assertEquals(3, testChromosome.getGenes().length);
		assertEquals(DoubleGene.class, testChromosome.getGenes()[0].getClass());
		assertEquals(DoubleGene.class, testChromosome.getGenes()[1].getClass());
		assertEquals(IntegerGene.class, testChromosome.getGenes()[2].getClass());

	}

	public void testWriteChromosome2Plan() {

		// writeChromosome2Plan() has 3 arguments:
		Plan testPlan = null;
		IChromosome testChromosome = null;
		LegTravelTimeEstimator ltte = null;

		// init test Plan
		final String TEST_PERSON_ID = "100";
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = population.getPerson(TEST_PERSON_ID);
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
					testGenes[ii] = new DoubleGene(jgapConfiguration);
					testGenes[ii].setAllele(Time.parseTime("07:45:00"));
					break;
				case 1:
					testGenes[ii] = new DoubleGene(jgapConfiguration);
					testGenes[ii].setAllele(Time.parseTime("8:00:01"));
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
		TravelTime tTravelEstimator = new LinearInterpolatingTTCalculator(network, 900);
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(network, 900);
		ltte = new CharyparEtAlCompatibleLegTravelTimeEstimator(tTravelEstimator, travelCostEstimator, depDelayCalc, network);

		// run the method
		PlanOptimizeTimes testee = new PlanOptimizeTimes(ltte, null);

		testee.writeChromosome2Plan(testChromosome, testPlan, planAnalyzeSubtours);

		// write out the test person and the modified plan into a file
		Population outputPopulation = new Population();
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

	public void testGetOriginalRoutes() {
		
		// init test Plan
		final String TEST_PERSON_ID = "100";
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = population.getPerson(TEST_PERSON_ID);
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);
		
		CarRoute expectedRoute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
		expectedRoute.setNodes("2 7 12");
		
		HashMap<Leg, CarRoute> legsRoutes = PlanOptimizeTimes.getLegsRoutes(testPlan);
		
		// this code should changes to the route of the plan leg object, 
		// but should not affect the previously saved routes 
		Leg modifyMe = testPlan.getNextLeg(testPlan.getFirstActivity());
		CarRoute differentRoute = (CarRoute) this.network.getFactory().createRoute(BasicLeg.Mode.car);
		differentRoute.setNodes("2 10 12");
		modifyMe.setRoute(differentRoute);
		
		List<Node> actualRoute = legsRoutes.get(modifyMe).getNodes();
		assertEquals(expectedRoute.getNodes(), actualRoute);
		
	}
	
	public void testGenerateRandomDemand() {

		final int TEST_PLAN_NR = 0;

		// the planomat can be used to generate random demand with respect to the dimensions that are optimized by it
		// in the following way:
		// - set the number of generations to 0 (so only the random initialization, and no optimization takes place), and
		// - set the population size to 1, so there is no sample of the initial random solutions the best individual would be chosen of 
		Gbl.getConfig().planomat().setPopSize(1);
		Gbl.getConfig().planomat().setJgapMaxGenerations(0);
		Gbl.getConfig().planomat().setPossibleModes(new BasicLeg.Mode[]{BasicLeg.Mode.car, BasicLeg.Mode.pt});
		
		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(network, 900);
		TravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator);
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(network, 900);

		LegTravelTimeEstimator ltte = new CetinCompatibleLegTravelTimeEstimator(tTravelEstimator, travelCostEstimator, depDelayCalc, network);
		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory();

		PlanOptimizeTimes testee = new PlanOptimizeTimes(ltte, scoringFunctionFactory);

		for (Person person : this.population) {
			
			Plan plan = person.getPlans().get(TEST_PLAN_NR);
			testee.run(plan);
		}
		
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
