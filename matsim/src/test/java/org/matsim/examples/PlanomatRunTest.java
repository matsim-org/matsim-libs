/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControlerTest.java
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

package org.matsim.examples;

import java.util.HashMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.planomat.utils.SelectedPlansScoreTest;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatRunTest extends MatsimTestCase {

	private Config config;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(this.getInputDirectory() + "config.xml");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.config = null;
	}

	public void testMainDefault() {
		this.runControlerTest(this.config);
	}

	public void testMainCarPt() {
		this.runControlerTest(this.config);
	}

	private void runControlerTest(final Config config) {

		String plansInputFile = config.plans().getInputFile();

		HashMap<Id,Double> expectedScores = new HashMap<Id,Double>();

		Scenario expectedScenario = new ScenarioImpl(config);
		expectedScenario.getConfig().plans().setInputFile(this.getInputDirectory() + "plans.xml.gz");
		new ScenarioLoaderImpl(expectedScenario).loadScenario();
		for (Person person : expectedScenario.getPopulation().getPersons().values()) {
			expectedScores.put(person.getId(), person.getSelectedPlan().getScore());
		}

		config.plans().setInputFile(plansInputFile);

		Controler testee = new Controler(config);
		testee.addControlerListener(new SelectedPlansScoreTest(expectedScores, 10));
		testee.setCreateGraphs(false);
		testee.setWriteEventsInterval(0);
		testee.run();

	}

}
