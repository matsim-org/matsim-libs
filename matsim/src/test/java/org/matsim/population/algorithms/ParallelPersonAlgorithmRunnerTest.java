/* *********************************************************************** *
 * project: org.matsim.*
 * ParallelPersonAlgorithmRunnerTest.java
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

package org.matsim.population.algorithms;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.AbstractPersonAlgorithm;
import org.matsim.core.population.algorithms.ParallelPersonAlgorithmUtils;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * Tests the class {@link ParallelPersonAlgorithmUtils}.
 *
 * @author mrieser
 */
public class ParallelPersonAlgorithmRunnerTest {

	/**
	 * Tests that the specified number of threads is allocated.
	 *
	 * @author mrieser
	 */
	@Test
	void testNumberOfThreads() {
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		PersonAlgorithmTester algo = new PersonAlgorithmTester();
		PersonAlgoProviderTester tester = new PersonAlgoProviderTester(algo);
		ParallelPersonAlgorithmUtils.run(population, 2, tester);
		Assertions.assertEquals(2, tester.counter);

		PersonAlgoProviderTester tester2 = new PersonAlgoProviderTester(algo);
		ParallelPersonAlgorithmUtils.run(population, 4, tester2);
		Assertions.assertEquals(4, tester2.counter);
	}

	/**
	 * Tests that all persons in the population are handled when using the threads.
	 *
	 * @author mrieser
	 */
	@Test
	void testNofPersons() {
		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Population population = scenario.getPopulation();
		for (int i = 0; i < 100; i++) {
			Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			population.addPerson(person);
		}
		final PersonAlgorithmTester tester = new PersonAlgorithmTester();
		ParallelPersonAlgorithmUtils.run(population, 2, tester);

		Assertions.assertEquals(100, tester.personIds.size());

		// test that all 100 different persons got handled, and not 1 person 100 times
		int sum = 0;
		int sumRef = 0;
		// build the sum of the personId's
		for (int i = 0, n = population.getPersons().size(); i < n; i++) {
			sumRef += i;
			sum += Integer.parseInt(population.getPersons().get(tester.personIds.get(i)).getId().toString());
		}
		Assertions.assertEquals(sumRef, sum);
	}

	@Test
	void testCrashingAlgorithm() {
		try {
			MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
			Population population = scenario.getPopulation();
			for (int i = 0; i < 10; i++) {
				Person person = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
				population.addPerson(person);
			}
			ParallelPersonAlgorithmUtils.run(population, 2, new AbstractPersonAlgorithm() {
				@Override
				public void run(Person person) {
					person.getPlans().get(0).setScore(null); // this will result in an IndexOutOfBoundsException
				}
			});
			Assertions.fail("Expected Exception, got none.");
		} catch (RuntimeException e) {
			LogManager.getLogger(ParallelPersonAlgorithmRunnerTest.class).info("Catched expected exception.", e);
		}
	}

	/**
	 * A helper class for {@link #testNumberOfThreads}.
	 *
	 * @author mrieser
	 */
	private static class PersonAlgoProviderTester implements ParallelPersonAlgorithmUtils.PersonAlgorithmProvider {
		protected int counter = 0;
		private final AbstractPersonAlgorithm algo;

		protected PersonAlgoProviderTester(final AbstractPersonAlgorithm algo) {
			this.algo = algo;
		}
		@Override
		public AbstractPersonAlgorithm getPersonAlgorithm() {
			this.counter++;
			return this.algo;
		}
	}

	/**
	 * A helper class for {@link #testNofPersons}.
	 *
	 * @author mrieser
	 */
	private static class PersonAlgorithmTester extends AbstractPersonAlgorithm {
		protected final ArrayList<Id<Person>> personIds = new ArrayList<>(100);

		public PersonAlgorithmTester() {
			// make constructor public
		}
		@Override
		public void run(final Person person) {
			handlePerson(person);
		}
		private synchronized void handlePerson(final Person person) {
			this.personIds.add(person.getId());
		}
	}
}
