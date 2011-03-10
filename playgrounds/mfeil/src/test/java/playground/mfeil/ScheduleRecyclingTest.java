/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mfeil;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.testcases.MatsimTestCase;

import playground.mfeil.FilesForTests.Initializer;

@Ignore("failing, unmaintained")
public class ScheduleRecyclingTest extends MatsimTestCase{

	private static final Logger log = Logger.getLogger(PlanomatXTest.class);
	private Initializer initializer;
	final String TEST_PERSON_ID = "10";
	private ScheduleRecycling testee;
	private ScenarioImpl scenario_input;

	@Override
	protected void setUp() throws Exception {

		super.setUp();

		this.initializer = new Initializer();
		this.initializer.init(this);
		this.initializer.run();

		this.scenario_input = this.initializer.getControler().getScenario();

		ActivityTypeFinder finder = new ActivityTypeFinder (this.initializer.getControler());

		this.testee = new ScheduleRecycling (this.initializer.getControler(), finder);
	}

	@Ignore("failing, unmaintained")
	public void testAll (){
		log.info("Starting test...");
		this.testee.prepareReplanning();
		for (Person person : this.scenario_input.getPopulation().getPersons().values()){
			if (person.getId().toString().equals("1")||
					person.getId().toString().equals("2")||
					person.getId().toString().equals("3")||
					person.getId().toString().equals("4")||
					person.getId().toString().equals("5"))continue;
			this.testee.handlePlan(person.getSelectedPlan());
		}
		this.testee.finishReplanning();

		// Compare the two plans; <1 because of double rounding errors
		assertEquals(this.testee.coefficients.getSingleCoef("primActsDistance"),0.0);
		assertEquals(this.testee.coefficients.getSingleCoef("homeLocationDistance"),2.5);
		assertEquals(this.testee.coefficients.getSingleCoef("age"),1.0);
		log.info("done.");
	}

}
