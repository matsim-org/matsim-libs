/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice;

import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.locationchoice.random.RandomLocationMutator;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.testcases.MatsimTestCase;


public class RandomLocationMutatorTest  extends MatsimTestCase {

	private MutableScenario scenario;

	private RandomLocationMutator initialize() {
		Initializer initializer = new Initializer();
		initializer.init(this);
		scenario = (MutableScenario) initializer.getControler().getScenario();
		return new RandomLocationMutator(scenario, new Random(1111));
	}

	/*
	 * TODO: Construct scenario with knowledge to compare plans before and after loc. choice
	 */
	public void testHandlePlan() {
		RandomLocationMutator randomlocationmutator = this.initialize();
		randomlocationmutator.run(scenario.getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan());
	}
}