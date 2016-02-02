/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelOpenTimesScoringFunctionTest.java
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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityFacilityImpl;
import org.matsim.facilities.ActivityOptionImpl;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.testcases.MatsimTestCase;

public class CharyparNagelOpenTimesScoringFunctionTest extends MatsimTestCase {

	private Person person = null;
	private PlanImpl plan = null;
	private ActivityFacilities facilities = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// create facilities, activities in it and open times
		this.facilities = new ActivityFacilitiesImpl();

		Coord defaultCoord = new Coord(0.0, 0.0);
		ActivityFacilityImpl testFacility = ((ActivityFacilitiesImpl) facilities).createAndAddFacility(Id.create(0, ActivityFacility.class), defaultCoord);

		ActivityOptionImpl ao = testFacility.createAndAddActivityOption("shop");
		ao.addOpeningTime(new OpeningTimeImpl(6.0 * 3600, 11.0 * 3600));
		ao.addOpeningTime(new OpeningTimeImpl(13.0 * 3600, 19.0 * 3600));

		// here, we don't test the scoring function itself, but just the method to retrieve opening times
		// we don't really need persons and plans, they're just used to initialize the ScoringFunction object
		this.person = PopulationUtils.createPerson(Id.create(1, Person.class));
		this.plan = new PlanImpl();
		this.person.addPlan(this.plan);

		ActivityImpl act = plan.createAndAddActivity("shop");
		act.setFacilityId(testFacility.getId());
		act.setStartTime(8.0 * 3600);
		act.setEndTime(16.0 * 3600);
	}

	@Override
	protected void tearDown() throws Exception {
		this.plan = null;
		this.person = null;
		this.facilities = null;
		super.tearDown();
	}

	public void testGetOpeningInterval() {
		final Config config = loadConfig(null);
		Activity act = this.plan.getFirstActivity();

		CharyparNagelOpenTimesActivityScoring testee =
				new CharyparNagelOpenTimesActivityScoring(
						CharyparNagelScoringParameters.getBuilder(
								config.planCalcScore(),
								config.planCalcScore().getScoringParameters( null ),
								config.scenario()).create(), this.facilities);

		double[] openInterval = null;

		openInterval = testee.getOpeningInterval(act);

		assertEquals(6.0 * 3600, openInterval[0], EPSILON);
		assertEquals(19.0 * 3600, openInterval[1], EPSILON);
	}

}
