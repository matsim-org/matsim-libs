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

package org.matsim.planomat;

import java.util.Arrays;
import java.util.Iterator;

import org.jgap.BaseGeneticOperator;
import org.jgap.impl.BestChromosomesSelector;
import org.jgap.impl.CrossoverOperator;
import org.jgap.impl.MutationOperator;
import org.matsim.api.basic.v01.TransportMode;
import org.matsim.core.api.experimental.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.population.algorithms.PlanAnalyzeSubtours;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatJGAPConfigurationTest extends MatsimTestCase {

	private ScenarioImpl scenario;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Config config = super.loadConfig(this.getInputDirectory() + "config.xml");
		config.plans().setInputFile(getPackageInputDirectory() + "testPlans.xml");
		ScenarioLoader loader = new ScenarioLoader(config);
		loader.loadScenario();
		this.scenario = loader.getScenario();
	}

	public void testPlanomatJGAPConfiguration() {

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		PersonImpl testPerson = this.scenario.getPopulation().getPersons().get(new IdImpl("100"));
		// only plan of that person
		PlanImpl testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		Planomat testee = new Planomat(null, null, this.scenario.getConfig().planomat());

		TransportMode[] possibleModes = testee.getPossibleModes(testPlan);
		
		PlanAnalyzeSubtours planAnalyzeSubtours = null;
		if (possibleModes.length > 0) {
			planAnalyzeSubtours = new PlanAnalyzeSubtours();
			planAnalyzeSubtours.run(testPlan);
		}

		// run testee
		long seed = 3810;
		PlanomatJGAPConfiguration jgapConfig = new PlanomatJGAPConfiguration(
				testPlan, 
				planAnalyzeSubtours, 
				seed,
				128,
				possibleModes);

		// see below for correct functioning of the random number generator
		Double[] randomNumberSequenceA = new Double[1000];
		for (int ii=0; ii < randomNumberSequenceA.length; ii++) {
			randomNumberSequenceA[ii] = jgapConfig.getRandomGenerator().nextDouble();
		}
		
		// test correct setting of natural selector
		assertEquals(1, jgapConfig.getNaturalSelectorsSize(false));
		assertEquals(0, jgapConfig.getNaturalSelectorsSize(true));
		assertEquals(BestChromosomesSelector.class, jgapConfig.getNaturalSelector(false, 0).getClass());

		// test correct setting of population size
		int expectedPopSize = 14;
		assertEquals(expectedPopSize, jgapConfig.getPopulationSize());

		// test correct settings for genetic operators
		assertEquals(2, jgapConfig.getGeneticOperators().size());
		Iterator geneticOperatorsIterator = jgapConfig.getGeneticOperators().iterator();
		while (geneticOperatorsIterator.hasNext()) {
			BaseGeneticOperator bgo = (BaseGeneticOperator) geneticOperatorsIterator.next();
			if (bgo.getClass().equals(CrossoverOperator.class)) {
				assertEquals(0.6d, ((CrossoverOperator) bgo).getCrossOverRatePercent(), EPSILON);
			} else if (bgo.getClass().equals(MutationOperator.class)) {
				assertEquals(expectedPopSize, ((MutationOperator) bgo).getMutationRate());
			}
		}
		
		// test correct setting of random number generator
		jgapConfig = new PlanomatJGAPConfiguration(
				testPlan, 
				planAnalyzeSubtours, 
				seed,
				128,
				possibleModes);

		Double[] randomNumberSequenceB = new Double[1000];
		for (int ii=0; ii < randomNumberSequenceA.length; ii++) {
			randomNumberSequenceB[ii] = jgapConfig.getRandomGenerator().nextDouble();
		}
		assertTrue(Arrays.deepEquals(randomNumberSequenceA, randomNumberSequenceB));
	}

	public void testPlanomatJGAPConfigurationCarPt() {

		// init test Plan
		final int TEST_PLAN_NR = 0;

		// first person
		PersonImpl testPerson = this.scenario.getPopulation().getPersons().get(new IdImpl("100"));
		// only plan of that person
		PlanImpl testPlan = testPerson.getPlans().get(TEST_PLAN_NR);

		Planomat testee = new Planomat(null, null, this.scenario.getConfig().planomat());

		TransportMode[] possibleModes = testee.getPossibleModes(testPlan);
		
		PlanAnalyzeSubtours planAnalyzeSubtours = null;
		if (possibleModes.length > 0) {
			planAnalyzeSubtours = new PlanAnalyzeSubtours();
			planAnalyzeSubtours.run(testPlan);
		}

		// run testee
		long seed = 3812;
		PlanomatJGAPConfiguration jgapConfig = new PlanomatJGAPConfiguration(
				testPlan, 
				planAnalyzeSubtours, 
				seed,
				128,
				possibleModes);

		// see below for correct functioning of the random number generator
		Double[] randomNumberSequenceA = new Double[1000];
		for (int ii=0; ii < randomNumberSequenceA.length; ii++) {
			randomNumberSequenceA[ii] = jgapConfig.getRandomGenerator().nextDouble();
		}

		// test correct setting of natural selector
		assertEquals(1, jgapConfig.getNaturalSelectorsSize(false));
		assertEquals(0, jgapConfig.getNaturalSelectorsSize(true));
		assertEquals(BestChromosomesSelector.class, jgapConfig.getNaturalSelector(false, 0).getClass());

		// test correct setting of population size
		int expectedPopSize = 16;
		assertEquals(expectedPopSize, jgapConfig.getPopulationSize());

		// test correct settings for genetic operators
		assertEquals(2, jgapConfig.getGeneticOperators().size());
		Iterator geneticOperatorsIterator = jgapConfig.getGeneticOperators().iterator();
		while (geneticOperatorsIterator.hasNext()) {
			BaseGeneticOperator bgo = (BaseGeneticOperator) geneticOperatorsIterator.next();
			if (bgo.getClass().equals(CrossoverOperator.class)) {
				assertEquals(0.6d, ((CrossoverOperator) bgo).getCrossOverRatePercent(), EPSILON);
			} else if (bgo.getClass().equals(MutationOperator.class)) {
				assertEquals(expectedPopSize, ((MutationOperator) bgo).getMutationRate());
			}
		}

		// test correct setting of random number generator
		jgapConfig = new PlanomatJGAPConfiguration(
				testPlan, 
				planAnalyzeSubtours, 
				seed,
				128,
				possibleModes);
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
