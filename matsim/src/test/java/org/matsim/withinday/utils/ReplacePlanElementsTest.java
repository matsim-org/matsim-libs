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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestCase;

public class ReplacePlanElementsTest extends MatsimTestCase {
	
	/**
	 * @author cdobler
	 */
	public void testReplaceActivity() {
		Plan plan = createSamplePlan();
		Activity oldActivity = (Activity)plan.getPlanElements().get(0);
		Activity newActivity = PopulationUtils.createActivityFromCoord("s", new Coord((double) 200, (double) 200));
		
		// expect rpe to return false if the plan or one of the activities is null
		assertEquals(EditPlans.replaceActivityBlindly(null, oldActivity, newActivity), false);
		assertEquals(EditPlans.replaceActivityBlindly(plan, null, newActivity), false);
		assertEquals(EditPlans.replaceActivityBlindly(plan, oldActivity, null), false);
		
		// old activity has to be part of the plan
		assertEquals(EditPlans.replaceActivityBlindly(plan, newActivity, newActivity), false);

		// replace activity successful
		assertEquals(EditPlans.replaceActivityBlindly(plan, oldActivity, newActivity), true);
		
		// check whether activity has really be replaced
		assertEquals(plan.getPlanElements().get(0).equals(newActivity), true);
	}
	
	/**
	 * @author cdobler
	 */
	public void testReplaceLeg() {
		Plan plan = createSamplePlan();
		Leg oldLeg = (Leg)plan.getPlanElements().get(1);
		Leg newLeg = PopulationUtils.createLeg(TransportMode.walk);
		
		// expect rpe to return false if the plan or one of the legs is null
		assertEquals(EditPlans.replaceLegBlindly(null, oldLeg, newLeg), false);
		assertEquals(EditPlans.replaceLegBlindly(plan, null, newLeg), false);
		assertEquals(EditPlans.replaceLegBlindly(plan, oldLeg, null), false);
		
		// old leg has to be part of the plan
		assertEquals(EditPlans.replaceLegBlindly(plan, newLeg, newLeg), false);
		
		// replace leg successful
		assertEquals(EditPlans.replaceLegBlindly(plan, oldLeg, newLeg), true);
		
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