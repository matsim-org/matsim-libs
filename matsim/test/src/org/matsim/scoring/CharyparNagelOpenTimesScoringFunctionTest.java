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

package org.matsim.scoring;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.config.Config;
import org.matsim.facilities.Facilities;
import org.matsim.facilities.OpeningTime;
import org.matsim.interfaces.basic.v01.Coord;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.PersonImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class CharyparNagelOpenTimesScoringFunctionTest extends MatsimTestCase {

	private Person person = null;
	private Plan plan = null;

	private static final String UNUSED_OPENTIME_ACTIVITY_TYPE = "no wed and wkday open time activity";
	private static final String ONE_WKDAY_ACTIVITY_TYPE = "one opening interval on wkday activity";
	private static final String TWO_WEDNESDAY_ACTIVITY_TYPE = "two opening intervals on wednesday activity";

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// create facilities, activities in it and open times
		Facilities facilities = new Facilities();

		Coord defaultCoord = new CoordImpl(0.0, 0.0);
		Facility testFacility = facilities.createFacility(new IdImpl(0), defaultCoord);

		ActivityOption noWedAndWkDay = testFacility.createActivity(CharyparNagelOpenTimesScoringFunctionTest.UNUSED_OPENTIME_ACTIVITY_TYPE);
		noWedAndWkDay.addOpeningTime(new OpeningTime(DayType.fri, 8.0 * 3600, 16.0 * 3600));

		ActivityOption wkdayActivity = testFacility.createActivity(CharyparNagelOpenTimesScoringFunctionTest.ONE_WKDAY_ACTIVITY_TYPE);
		wkdayActivity.addOpeningTime(new OpeningTime(DayType.wkday, 7.5 * 3600, 18.0 * 3600));

		ActivityOption wednesdayActivity = testFacility.createActivity(CharyparNagelOpenTimesScoringFunctionTest.TWO_WEDNESDAY_ACTIVITY_TYPE);
		wednesdayActivity.addOpeningTime(new OpeningTime(DayType.wed, 6.0 * 3600, 11.0 * 3600));
		wednesdayActivity.addOpeningTime(new OpeningTime(DayType.wed, 13.0 * 3600, 19.0 * 3600));
		// this one should be ignored
		wednesdayActivity.addOpeningTime(new OpeningTime(DayType.wkday, 4.0 * 3600, 20.0 * 3600));

		// here, we don't test the scoring function itself, but just the method to retrieve opening times
		// we don't really need persons and plans, they're just used to initialize the ScoringFunction object
		this.person = new PersonImpl(new IdImpl(1));
		this.plan = person.createPlan(true);

		Act act = plan.createAct("no type", testFacility);
		act.setStartTime(8.0 * 3600);
		act.setEndTime(16.0 * 3600);
		act.setFacility(testFacility);
	}

	@Override
	protected void tearDown() throws Exception {
		this.plan = null;
		this.person = null;
		super.tearDown();
	}

	public void testGetOpeningInterval() {
		final Config config = loadConfig(null);
		Act act = this.plan.getFirstActivity();

		CharyparNagelOpenTimesScoringFunction testee = new CharyparNagelOpenTimesScoringFunction(this.plan, new CharyparNagelScoringParameters(config.charyparNagelScoring()));

		double[] openInterval = null;

		act.setType(CharyparNagelOpenTimesScoringFunctionTest.UNUSED_OPENTIME_ACTIVITY_TYPE);

		openInterval = testee.getOpeningInterval(act);

		assertEquals(openInterval[0], Time.UNDEFINED_TIME, EPSILON);
		assertEquals(openInterval[1], Time.UNDEFINED_TIME, EPSILON);

		act.setType(CharyparNagelOpenTimesScoringFunctionTest.ONE_WKDAY_ACTIVITY_TYPE);

		openInterval = testee.getOpeningInterval(act);

		assertEquals(openInterval[0], 7.5 * 3600, EPSILON);
		assertEquals(openInterval[1], 18.0 * 3600, EPSILON);

		act.setType(CharyparNagelOpenTimesScoringFunctionTest.TWO_WEDNESDAY_ACTIVITY_TYPE);

		openInterval = testee.getOpeningInterval(act);

		assertEquals(openInterval[0], 6.0 * 3600, EPSILON);
		assertEquals(openInterval[1], 19.0 * 3600, EPSILON);
	}

}
