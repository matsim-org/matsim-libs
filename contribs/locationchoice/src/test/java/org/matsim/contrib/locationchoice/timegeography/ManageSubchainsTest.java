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

package org.matsim.contrib.locationchoice.timegeography;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.locationchoice.Initializer;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ManageSubchainsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	@Test
	void testPrimarySecondaryActivityFound() {
		Initializer initializer = new Initializer();
		initializer.init(utils);
		ManageSubchains manager = new ManageSubchains();

        Plan plan = initializer.getControler().getScenario().getPopulation().getPersons().get(Id.create("1", Person.class)).getSelectedPlan();
		Activity act = PopulationUtils.getFirstActivity( ((Plan) plan) );
		final Activity act1 = act;
		Leg leg = PopulationUtils.getNextLeg(((Plan) plan), act1);
		manager.primaryActivityFound(act, leg);
		assertEquals(act, manager.getSubChains().get(0).getFirstPrimAct());
		final Leg leg1 = leg;

		act = PopulationUtils.getNextActivity(((Plan) plan), leg1);
		final Activity act2 = act;
		manager.secondaryActivityFound(act, PopulationUtils.getNextLeg(((Plan) plan), act2));
		assertEquals(act, manager.getSubChains().get(0).getSlActs().get(0));
	}
}
