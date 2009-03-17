/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityScoringFunctionTest.java
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

package playground.meisterk.org.matsim.scoring.ktiYear3;

import org.matsim.basic.v01.IdImpl;
import org.matsim.basic.v01.BasicOpeningTime.DayType;
import org.matsim.facilities.FacilitiesImpl;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.interfaces.core.v01.Activity;
import org.matsim.interfaces.core.v01.ActivityOption;
import org.matsim.interfaces.core.v01.Coord;
import org.matsim.interfaces.core.v01.Facilities;
import org.matsim.interfaces.core.v01.Facility;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.ActImpl;
import org.matsim.population.PersonImpl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.geometry.CoordImpl;
import org.matsim.utils.misc.Time;

public class ActivityScoringFunctionTest extends MatsimTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetOpeningInterval() {

		ActivityOption activityOption = null;
		DayType day = DayType.wkday;
		
		Facilities facilities = new FacilitiesImpl();
		
		Coord dummyCoord = new CoordImpl(0.0, 0.0);
		Facility facility = facilities.createFacility(new IdImpl("123"), dummyCoord);
		activityOption = facility.createActivityOption("work_sector2");
		activityOption.addOpeningTime(new OpeningTimeImpl(day, Time.parseTime("11:00:00"), Time.parseTime("23:00:00")));
		activityOption = facility.createActivityOption("leisure_gastro");
		activityOption.addOpeningTime(new OpeningTimeImpl(day, Time.parseTime("11:00:00"), Time.parseTime("14:00:00")));
		activityOption.addOpeningTime(new OpeningTimeImpl(day, Time.parseTime("17:00:00"), Time.parseTime("23:00:00")));

		Person dummyPerson = new PersonImpl(new IdImpl("999"));
		Plan dummyPlan = dummyPerson.createPlan(true);
		ActivityScoringFunction testee = new ActivityScoringFunction(dummyPlan, null);

		Activity testActivity = null;
		double[] openingInterval = null;
		
		testActivity = new ActImpl("work_sector2", facility);
		openingInterval = testee.getOpeningInterval(testActivity);
		assertEquals(Time.parseTime("11:00:00"), openingInterval[0]);
		assertEquals(Time.parseTime("23:00:00"), openingInterval[1]);
		
		testActivity = new ActImpl("leisure", facility);
		openingInterval = testee.getOpeningInterval(testActivity);
		assertEquals(Time.parseTime("11:00:00"), openingInterval[0]);
		assertEquals(Time.parseTime("23:00:00"), openingInterval[1]);
	}

}
