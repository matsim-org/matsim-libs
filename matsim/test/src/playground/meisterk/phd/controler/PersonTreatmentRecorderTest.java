/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTreatmentRecorderTest.java
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

package playground.meisterk.phd.controler;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

public class PersonTreatmentRecorderTest extends MatsimTestCase {

	private static final int DEFAULT_PERSON_NUMBER = 1;

	private Scenario sc = null;

	protected void setUp() throws Exception {
		super.setUp();

		this.sc = new ScenarioImpl() ;
		Population pop = this.sc.getPopulation() ;
		PopulationFactory pf = pop.getFactory() ;
		for (int personId : new int[]{1, 2, 3}) {
			Person person = pf.createPerson(new IdImpl(personId));
			for (double d : new double[]{180.0, 180.1, 180.5, 169.9}) {
				Plan plan = pf.createPlan();
				plan.setScore(d);
				person.addPlan(plan);
			}
			pop.addPerson(person);
		}

	}

	@Override
	protected void tearDown() throws Exception {
		this.sc = null;
		super.tearDown();
	}

	public void testGetRankOfSelectedPlan() {

		PersonTreatmentRecorder testee = new PersonTreatmentRecorder();

		Person person = this.sc.getPopulation().getPersons().get(new IdImpl(DEFAULT_PERSON_NUMBER));

		for (int i=0; i<3; i++) {
			Plan plan = person.getPlans().get(i);
			plan.setSelected(true);
			int rank = testee.getRankOfSelectedPlan(person);
			switch(i) {
			case 0:
				assertEquals("Wrong rank.", 2, rank);
				break;
			case 1:
				assertEquals("Wrong rank.", 1, rank);
				break;
			case 2:
				assertEquals("Wrong rank.", 0, rank);
				break;
			case 3:
				assertEquals("Wrong rank.", 3, rank);
				break;
			}
		}

	}

	public void testIsSelectedPlanTheBestPlan() {

		Map<Double, Long> expectedResults = new LinkedHashMap<Double, Long>();
		/*
		 * TODO Intuitively, I should use Double.POSITIVE_INFINITY here (instead of Double.MAX_VALUE), but that doesn't work...
		 */
		expectedResults.put(Double.MAX_VALUE, 100000L);
		expectedResults.put(2.0, 55012L);
		expectedResults.put(0.0, 24881L);

		for (Double brainExpBeta : expectedResults.keySet()) {
			this.sc.getConfig().charyparNagelScoring().setBrainExpBeta(brainExpBeta);
			long cntBest = 0;
			PersonTreatmentRecorder testee = new PersonTreatmentRecorder();
			Person person = this.sc.getPopulation().getPersons().get(new IdImpl(DEFAULT_PERSON_NUMBER));
			for (int j = 0; j < 100000; j++) {
				Plan plan = person.getPlans().get(2);
				plan.setSelected(true);
				if (testee.isSelectedPlanTheBestPlan(person, this.sc.getConfig().charyparNagelScoring())) {
					cntBest++;
				}
			}
			assertEquals("wrong results for brainExpBeta=" + brainExpBeta + ". ", expectedResults.get(brainExpBeta).longValue(), cntBest);
		}

	}

	public void testGetScoreDifference() {

		PersonTreatmentRecorder testee = new PersonTreatmentRecorder();

		Person person = this.sc.getPopulation().getPersons().get(new IdImpl(DEFAULT_PERSON_NUMBER));

		for (int i=0; i<3; i++) {
			Plan plan = person.getPlans().get(i);
			plan.setSelected(true);
			Double scoreDifference = testee.getAbsoluteScoreDifference(person);
			switch(i) {
			case 0:
				assertEquals("Wrong scoreDifference.", 10.1, scoreDifference, MatsimTestCase.EPSILON);
				break;
			case 1:
				assertEquals("Wrong scoreDifference.", 0.1, scoreDifference, MatsimTestCase.EPSILON);
				break;
			case 2:
				assertEquals("Wrong scoreDifference.", 0.4, scoreDifference, MatsimTestCase.EPSILON);
				break;
			case 3:
				assertEquals("Wrong scoreDifference.", null, scoreDifference);
				break;
			}
		}
	}

	public void testGetCountsString() {

		Map<String, Set<Person>> personTreatment = new TreeMap<String, Set<Person>>();
		for (String planStrategyName : new String[]{"swim", "hike", "fly"}) {
			Set<Person> persons = new HashSet<Person>();
			personTreatment.put(planStrategyName, persons);
			if (planStrategyName.equals("fly")) {
				Person person = this.sc.getPopulation().getPersons().get(new IdImpl(DEFAULT_PERSON_NUMBER));
				Plan plan = person.getPlans().get(0);
				plan.setSelected(true);
				persons.add(person);
			} else if (planStrategyName.equals("hike")) {
				Person person = this.sc.getPopulation().getPersons().get(new IdImpl(2));
				Plan plan = person.getPlans().get(1);
				plan.setSelected(true);
				persons.add(person);
			} else if (planStrategyName.equals("swim")) {
				Person person = this.sc.getPopulation().getPersons().get(new IdImpl(3));
				Plan plan = person.getPlans().get(2);
				plan.setSelected(true);
				persons.add(person);
			}
		}


		PersonTreatmentRecorder testee = new PersonTreatmentRecorder();
		String actualString = testee.getCountsString(personTreatment, 3);
		assertEquals("\t0\t0\t1\t0\t0\t1\t0\t0\t1\t0\t0\t0", actualString);
	}

	public void testGetScoreDifferencesString() {
		Map<String, Set<Person>> personTreatment = new TreeMap<String, Set<Person>>();

		Person person;
		Plan plan;

		for (String planStrategyName : new String[]{"hike", "swim"}) {
			Set<Person> persons = new HashSet<Person>();
			personTreatment.put(planStrategyName, persons);
			if (planStrategyName.equals("hike")) {
				person = this.sc.getPopulation().getPersons().get(new IdImpl(DEFAULT_PERSON_NUMBER));
				plan = person.getPlans().get(2);
				plan.setSelected(true);
				persons.add(person);
			} else if (planStrategyName.equals("swim")) {
				person = this.sc.getPopulation().getPersons().get(new IdImpl(2));
				plan = person.getPlans().get(2);
				plan.setScore(200.0);
				plan.setSelected(true);
				persons.add(person);

				person = this.sc.getPopulation().getPersons().get(new IdImpl(3));
				plan = person.getPlans().get(2);
				plan.setScore(250.0);
				plan.setSelected(true);
				persons.add(person);

			}
		}

		PersonTreatmentRecorder testee = new PersonTreatmentRecorder();
		String actualString = testee.getScoreDifferencesString(personTreatment);
		assertEquals("\t0.4\t44.9", actualString);
	}

	public void testGetExpBetaSelectorString() {
		this.sc.getConfig().charyparNagelScoring().setBrainExpBeta(Double.MAX_VALUE);

		Map<String, Set<Person>> personTreatment = new TreeMap<String, Set<Person>>();

		Person person;
		Plan plan;

		for (String planStrategyName : new String[]{"hike", "swim"}) {
			Set<Person> persons = new HashSet<Person>();
			personTreatment.put(planStrategyName, persons);
			if (planStrategyName.equals("hike")) {
				person = this.sc.getPopulation().getPersons().get(new IdImpl(DEFAULT_PERSON_NUMBER));
				plan = person.getPlans().get(2);
				plan.setSelected(true);
				persons.add(person);
			} else if (planStrategyName.equals("swim")) {
				person = this.sc.getPopulation().getPersons().get(new IdImpl(2));
				plan = person.getPlans().get(2);
				plan.setSelected(true);
				persons.add(person);

				person = this.sc.getPopulation().getPersons().get(new IdImpl(3));
				plan = person.getPlans().get(1);
				plan.setSelected(true);
				persons.add(person);

			}
		}

		PersonTreatmentRecorder testee = new PersonTreatmentRecorder();
		String actualString = testee.getExpBetaSelectorString(personTreatment, this.sc.getConfig().charyparNagelScoring());
		assertEquals("\t1\t0.5", actualString);

	}
	
}
