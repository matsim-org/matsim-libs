/* *********************************************************************** *
 * project: org.matsim.*
 * ReplacePlanElementsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.withinday.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ReplacePlanElementsTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * @author cdobler
	 */
	@Test
	void testReplaceActivity() {
		Plan plan = createSamplePlan();
		Activity oldActivity = (Activity)plan.getPlanElements().get(0);
		Activity newActivity = PopulationUtils.createActivityFromCoord("s", new Coord((double) 200, (double) 200));

		// expect rpe to return false if the plan or one of the activities is null
		assertEquals( WithinDayAgentUtils.replaceActivityBlindly(null, oldActivity, newActivity), false);
		assertEquals( WithinDayAgentUtils.replaceActivityBlindly(plan, null, newActivity), false);
		assertEquals( WithinDayAgentUtils.replaceActivityBlindly(plan, oldActivity, null), false);

		// old activity has to be part of the plan
		assertEquals( WithinDayAgentUtils.replaceActivityBlindly(plan, newActivity, newActivity), false);

		// replace activity successful
		assertEquals( WithinDayAgentUtils.replaceActivityBlindly(plan, oldActivity, newActivity), true);

		// check whether activity has really be replaced
		assertEquals(plan.getPlanElements().get(0).equals(newActivity), true);
	}

	/**
	 * @author cdobler
	 */
	@Test
	void testReplaceLeg() {
		Plan plan = createSamplePlan();
		Leg oldLeg = (Leg)plan.getPlanElements().get(1);
		Leg newLeg = PopulationUtils.createLeg(TransportMode.walk);

		// expect rpe to return false if the plan or one of the legs is null
		assertEquals( WithinDayAgentUtils.replaceLegBlindly(null, oldLeg, newLeg), false);
		assertEquals( WithinDayAgentUtils.replaceLegBlindly(plan, null, newLeg), false);
		assertEquals( WithinDayAgentUtils.replaceLegBlindly(plan, oldLeg, null), false);

		// old leg has to be part of the plan
		assertEquals( WithinDayAgentUtils.replaceLegBlindly(plan, newLeg, newLeg), false);

		// replace leg successful
		assertEquals( WithinDayAgentUtils.replaceLegBlindly(plan, oldLeg, newLeg), true);

		// check whether leg has really be replaced
		assertEquals(plan.getPlanElements().get(1).equals(newLeg), true);
	}

	/**
	 * @author cdobler
	 */
	private static Plan createSamplePlan() {
		Plan plan = PopulationUtils.createPlan(PopulationUtils.getFactory().createPerson(Id.create(1, Person.class)));

		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord(0, 0));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "w", new Coord((double) 100, (double) 100));
		PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		PopulationUtils.createAndAddActivityFromCoord(plan, "h", new Coord((double) 0, (double) 0));

		return plan;
	}
}
