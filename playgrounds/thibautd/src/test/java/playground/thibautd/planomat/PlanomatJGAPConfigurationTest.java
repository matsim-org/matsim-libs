/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatJGAPConfigurationTest.java
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

import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import org.jgap.BaseGeneticOperator;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.MutationOperator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatJGAPConfigurationTest extends MatsimTestCase {

	private static final Id TEST_PERSON_ID = new IdImpl("100");
	private static final int TEST_PLAN_NR = 0;

	private Scenario scenario = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = super.loadConfig(this.getClassInputDirectory() + "config.xml");
		config.plans().setInputFile(getPackageInputDirectory() + "testPlans.xml");
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(config);
		this.scenario = loader.loadScenario();
	}

	public void testPlanomatJGAPConfiguration() {

		this.runATest(1, 0, BestChromosomesSelector.class, 15, 2, 0.6);

	}

	public void testPlanomatJGAPConfigurationCarPt() {

		this.scenario.getConfig().planomat().setPossibleModes("car,pt");
		this.runATest(1, 0, BestChromosomesSelector.class, 17, 2, 0.6);

	}

	public void testPlanWithoutLegs() {

		Person testPerson = this.scenario.getPopulation().getPersons().get(TEST_PERSON_ID);
		Plan testPlan = testPerson.getPlans().get(TEST_PLAN_NR);
		((PlanImpl) testPlan).removeActivity(2);
		this.runATest(1, 0, BestChromosomesSelector.class, 8, 2, 0.6);

	}

	private void runATest(
			int expectedNumberOfNaturalSelectorsFalse,
			int expectedNumberOfNaturalSelectorsTrue,
			Class expectedNaturalSelectorType,
			int expectedPopulationSize,
			int expectedNumberOfGeneticOperators,
			double expectedCrossoverRate
			) {

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
			planAnalyzeSubtours.setTripStructureAnalysisLayer(scenario.getConfig().planomat().getTripStructureAnalysisLayer());
			planAnalyzeSubtours.run(testPlan);
		}

		// run testee
		long seed = 3812;
		PlanomatJGAPConfiguration jgapConfig = new PlanomatJGAPConfiguration(
				testPlan,
				planAnalyzeSubtours,
				seed,
				128,
				possibleModes,
				planomatConfigGroup);

		// see below for correct functioning of the random number generator
		Double[] randomNumberSequenceA = new Double[1000];
		for (int ii=0; ii < randomNumberSequenceA.length; ii++) {
			randomNumberSequenceA[ii] = jgapConfig.getRandomGenerator().nextDouble();
		}

		// test correct setting of natural selector
		assertEquals(expectedNumberOfNaturalSelectorsFalse, jgapConfig.getNaturalSelectorsSize(false));
		assertEquals(expectedNumberOfNaturalSelectorsTrue, jgapConfig.getNaturalSelectorsSize(true));
		assertEquals(expectedNaturalSelectorType, jgapConfig.getNaturalSelector(false, 0).getClass());

		// test correct setting of population size
		assertEquals(expectedPopulationSize, jgapConfig.getPopulationSize());

		// test correct settings for genetic operators
		assertEquals(expectedNumberOfGeneticOperators, jgapConfig.getGeneticOperators().size());
		Iterator<BaseGeneticOperator> geneticOperatorsIterator = jgapConfig.getGeneticOperators().iterator();
		while (geneticOperatorsIterator.hasNext()) {
			BaseGeneticOperator bgo = geneticOperatorsIterator.next();
			if (bgo instanceof CrossoverOperator) {
				assertEquals(expectedCrossoverRate, ((CrossoverOperator) bgo).getCrossOverRatePercent(), EPSILON);
			} else if (bgo instanceof MutationOperator) {
				assertEquals(expectedPopulationSize, ((MutationOperator) bgo).getMutationRate());
			}
		}

		// test correct setting of random number generator
		jgapConfig = new PlanomatJGAPConfiguration(
				testPlan,
				planAnalyzeSubtours,
				seed,
				128,
				possibleModes,
				planomatConfigGroup);
		Double[] randomNumberSequenceB = new Double[1000];
		for (int ii=0; ii < randomNumberSequenceA.length; ii++) {
			randomNumberSequenceB[ii] = jgapConfig.getRandomGenerator().nextDouble();
		}
		assertTrue(Arrays.deepEquals(randomNumberSequenceA, randomNumberSequenceB));

	}


	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.scenario = null;
	}

}
