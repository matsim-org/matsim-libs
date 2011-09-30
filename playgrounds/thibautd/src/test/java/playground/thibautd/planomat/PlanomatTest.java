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

package playground.thibautd.planomat;

import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.jgap.Chromosome;
import org.jgap.Configuration;
import org.jgap.Gene;
import org.jgap.IChromosome;
import org.jgap.InvalidConfigurationException;
import org.jgap.impl.IntegerGene;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.PersonEvent;
import org.matsim.core.api.experimental.events.handler.PersonEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.PopulationWriter;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.charyparNagel.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.planomat.costestimators.DepartureDelayAverageCalculator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimator;
import org.matsim.planomat.costestimators.LegTravelTimeEstimatorFactory;
import org.matsim.planomat.costestimators.LinearInterpolatingTTCalculator;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatTest extends MatsimTestCase {

	private enum PlanomatTestRun {NOEVENTS_CAR, WITHEVENTS_CAR, NOEVENTS_CAR_PT, WITHEVENTS_CAR_PT;}

	private final static Id TEST_PERSON_ID = new IdImpl("100");

	private static final Logger log = Logger.getLogger(PlanomatTest.class);

	private Scenario scenario;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = super.loadConfig(this.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile(this.getPackageInputDirectory() + "testPlans.xml");
		if (this.getName().equals("testRunDefaultManyModes")) {
			config.plans().setInputFile(this.getInputDirectory() + "input_plans.xml.gz");
		}
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		loader.loadScenario();
		this.scenario = loader.getScenario();
		//store the only existing person
//		Person p = scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		//read the events once to create a complete test population
		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(new ScenarioCreatePersonEventHandler(this.scenario));
		new MatsimEventsReader(events).readFile(this.getClassInputDirectory() + "equil-times-only-1000.events.txt.gz");
		//now overwrite the testee person in the scenario
//		scenario.getPopulation().addPerson(p);// not necessary, as the reference stayed the same!

	}

	@Override
	protected void tearDown() throws Exception {
		this.scenario = null;
		super.tearDown();
	}

	public void testRunDefault() {
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR);
	}

	public void testRunDefaultWithEvents() {
		this.runATestRun(PlanomatTestRun.WITHEVENTS_CAR);
	}

	public void testRunCarPt() {
		this.scenario.getConfig().planomat().setPossibleModes("car,pt");
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR_PT);
	}

	public void testRunCarPtWithEvents() {
		this.scenario.getConfig().planomat().setPossibleModes("car,pt");
		this.runATestRun(PlanomatTestRun.WITHEVENTS_CAR_PT);
	}

	public void testRunDefaultManyModes() {
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR);
	}

	public void testCarAvailabilityAlways() {
		this.scenario.getConfig().planomat().setPossibleModes("car,pt");
		Person p = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		((PersonImpl) p).setCarAvail("always");
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR_PT);
	}

	public void testCarAvailabilityNever() {
		this.scenario.getConfig().planomat().setPossibleModes("car,pt");
		Person p = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		((PersonImpl) p).setCarAvail("never");
		this.runATestRun(PlanomatTestRun.NOEVENTS_CAR_PT);
	}

	private void runATestRun(final PlanomatTestRun testRun) {

		TravelTimeCalculator tTravelEstimator = new TravelTimeCalculator(this.scenario.getNetwork(), this.scenario.getConfig().travelTimeCalculator());
		PersonalizableTravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, this.scenario.getConfig().planCalcScore());
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(this.scenario.getNetwork(), 900);

		EventsManager events = (EventsManager) EventsUtils.createEventsManager();
		events.addHandler(tTravelEstimator);
		events.addHandler(depDelayCalc);

		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(this.scenario.getConfig().plansCalcRoute(), this.scenario.getNetwork(), travelCostEstimator, tTravelEstimator, ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).getModeRouteFactory());

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(tTravelEstimator, depDelayCalc);
//		LegTravelTimeEstimator ltte = legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
//				PlanomatConfigGroup.SimLegInterpretation.CetinCompatible,
//				this.scenario.getConfig().planomat().getRoutingCapability(),
//				plansCalcRoute);

		ScoringFunctionFactory scoringFunctionFactory = new CharyparNagelScoringFunctionFactory(this.scenario.getConfig().planCalcScore());

		log.info("Testing " + testRun.toString() + "...");

		// init Planomat
		Planomat testee = new Planomat(legTravelTimeEstimatorFactory, scoringFunctionFactory, this.scenario.getConfig().planomat(), plansCalcRoute, this.scenario.getNetwork());
		testee.getSeedGenerator().setSeed(this.scenario.getConfig().global().getRandomSeed());

		tTravelEstimator.reset(1);
		depDelayCalc.resetDepartureDelays();
		if (
				PlanomatTestRun.WITHEVENTS_CAR.equals(testRun) ||
				PlanomatTestRun.WITHEVENTS_CAR_PT.equals(testRun)) {

			new MatsimEventsReader(events).readFile(this.getClassInputDirectory() + "equil-times-only-1000.events.txt.gz");

		}

		// init test Plan

		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		// actual test
		testee.run(testPlan);

		// write out the test person and the modified plan into a file
		Population outputPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		outputPopulation.addPerson(testPerson);

		log.info("Writing plans file...");
		new PopulationWriter(outputPopulation, this.scenario.getNetwork()).write(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Writing plans file...DONE.");

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "output_plans.xml.gz");
		assertEquals("different plans files.", expectedChecksum, actualChecksum);

		log.info("Testing " + testRun.toString() + "...done.");
	}

	public void testInitSampleChromosome() {

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		// only plan of that person
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		PlanomatConfigGroup planomatConfigGroup = this.scenario.getConfig().planomat();
		Planomat testee = new Planomat(null, null, planomatConfigGroup, null, this.scenario.getNetwork());

		TreeSet<String> possibleModes = testee.getPossibleModes(testPlan);

		PlanAnalyzeSubtours planAnalyzeSubtours = null;
		if (possibleModes.size() > 0) {
			planAnalyzeSubtours = new PlanAnalyzeSubtours();
			planAnalyzeSubtours.run(testPlan);
		}

		PlanomatJGAPConfiguration jgapConfiguration = new PlanomatJGAPConfiguration(
				testPlan,
				planAnalyzeSubtours,
				4711,
				128,
				possibleModes,
				planomatConfigGroup);

		IChromosome testChromosome = jgapConfiguration.getSampleChromosome();
		assertEquals(3, testChromosome.getGenes().length);
		assertEquals(IntegerGene.class, testChromosome.getGenes()[0].getClass());
		assertEquals(IntegerGene.class, testChromosome.getGenes()[1].getClass());
		assertEquals(IntegerGene.class, testChromosome.getGenes()[2].getClass());

	}

	public void testStepThroughPlan_WriteBack() throws InvalidConfigurationException {

		Plan testPlan = null;
		IChromosome testChromosome = null;
		LegTravelTimeEstimator ltte = null;

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		Person testPerson = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		// only plan of that person
		testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		// init IChromosome (from JGAP)
		PlanAnalyzeSubtours planAnalyzeSubtours = new PlanAnalyzeSubtours();
		planAnalyzeSubtours.setTripStructureAnalysisLayer(scenario.getConfig().planomat().getTripStructureAnalysisLayer());
		planAnalyzeSubtours.run(testPlan);
		int numActs = planAnalyzeSubtours.getSubtourIndexation().length;

		Configuration jgapConfiguration = new Configuration();

		Gene[] testGenes = new Gene[1 + numActs + planAnalyzeSubtours.getNumSubtours()];

		Integer startPlan = Integer.valueOf(31);
		Integer workDur = Integer.valueOf(40);
		Integer homeDur = Integer.valueOf(88);
		Integer modeIndex = Integer.valueOf(0);

		for (int ii=0; ii < testGenes.length; ii++) {
			switch(ii) {
			case 0:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(startPlan);
				break;
			case 1:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(workDur);
				break;
			case 2:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(homeDur);
				break;
			case 3:
				testGenes[ii] = new IntegerGene(jgapConfiguration);
				testGenes[ii].setAllele(modeIndex);
				break;
			}

		}

		testChromosome = new Chromosome(jgapConfiguration, testGenes);

		// init LegTravelTimeEstimator
		PersonalizableTravelTime tTravelEstimator = new LinearInterpolatingTTCalculator(this.scenario.getNetwork(), 900);
		PersonalizableTravelCost travelCostEstimator = new TravelTimeDistanceCostCalculator(tTravelEstimator, this.scenario.getConfig().planCalcScore());
		DepartureDelayAverageCalculator depDelayCalc = new DepartureDelayAverageCalculator(this.scenario.getNetwork(), 900);

		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(this.scenario.getConfig().plansCalcRoute(), this.scenario.getNetwork(), travelCostEstimator, tTravelEstimator, ((PopulationFactoryImpl) this.scenario.getPopulation().getFactory()).getModeRouteFactory());

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(tTravelEstimator, depDelayCalc);
		ltte = legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				testPlan,
				PlanomatConfigGroup.SimLegInterpretation.CharyparEtAlCompatible,
				PlanomatConfigGroup.RoutingCapability.fixedRoute,
				plansCalcRoute,
				this.scenario.getNetwork());

		// run the method
		Planomat testee = new Planomat(legTravelTimeEstimatorFactory, null, this.scenario.getConfig().planomat(), plansCalcRoute, this.scenario.getNetwork());

		double score = testee.stepThroughPlan(Planomat.StepThroughPlanAction.WRITE_BACK, testChromosome, testPlan, null, ltte, null);
		assertEquals(0.0, score, MatsimTestCase.EPSILON);


		// write out the test person and the modified plan into a file
		Population outputPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		outputPopulation.addPerson(testPerson);

		System.out.println("Writing plans file...");
		new PopulationWriter(outputPopulation, this.scenario.getNetwork()).write(this.getOutputDirectory() + "output_plans.xml.gz");
		System.out.println("Writing plans file...DONE.");

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "output_plans.xml.gz");
		log.info("Expected checksum: " + Long.toString(expectedChecksum));
		log.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals(expectedChecksum, actualChecksum);

	}

	private static final class ScenarioCreatePersonEventHandler implements PersonEventHandler{

		private Scenario scenario;

		public ScenarioCreatePersonEventHandler(Scenario scenario) {
			this.scenario = scenario;
		}

		@Override
		public void handleEvent(PersonEvent event) {
			if (!this.scenario.getPopulation().getPersons().containsKey(event.getPersonId()))
			  this.scenario.getPopulation().addPerson(this.scenario.getPopulation().getFactory().createPerson(event.getPersonId()));
		}

		@Override
		public void reset(int iteration) {

		}

	}

}
