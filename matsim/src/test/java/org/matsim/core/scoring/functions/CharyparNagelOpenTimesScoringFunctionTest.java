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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.OpeningTimeImpl;
import org.matsim.testcases.MatsimTestCase;

public class CharyparNagelOpenTimesScoringFunctionTest extends MatsimTestCase {

	private Person person = null;
	private ActivityFacilities facilities = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		final Config config = ConfigUtils.createConfig();
		final Scenario scenario = ScenarioUtils.createScenario( config );
		this.facilities = scenario.getActivityFacilities() ;

		// create facilities, activities in it and open times
		final ActivityFacilitiesFactory facilitiesFactory = this.facilities.getFactory();

		Coord defaultCoord = new Coord(0.0, 0.0);
		ActivityFacility testFacility = facilitiesFactory.createActivityFacility(Id.create(0, ActivityFacility.class), defaultCoord) ;
		this.facilities.addActivityFacility(testFacility);

		ActivityOption ao = facilitiesFactory.createActivityOption("shop") ;
		testFacility.addActivityOption(ao);
		ao.addOpeningTime(new OpeningTimeImpl(6.0 * 3600, 11.0 * 3600));
		ao.addOpeningTime(new OpeningTimeImpl(13.0 * 3600, 19.0 * 3600));

		// here, we don't test the scoring function itself, but just the method to retrieve opening times
		// we don't really need persons and plans, they're just used to initialize the ScoringFunction object
		final PopulationFactory pf = scenario.getPopulation().getFactory();
		this.person = pf.createPerson(Id.create(1, Person.class));
		
		Plan plan = pf.createPlan() ;
		this.person.addPlan(plan);
		
		Activity act = pf.createActivityFromCoord("shop", defaultCoord ) ;
		plan.addActivity(act);
		act.setFacilityId(testFacility.getId()); 
		act.setStartTime(8.0 * 3600);
		act.setEndTime(16.0 * 3600);
	}

	@Override
	protected void tearDown() throws Exception {
		this.person = null;
		this.facilities = null;
		super.tearDown();
	}

	public void testGetOpeningInterval() {
		final Config config = loadConfig(null);
		Activity act =  (Activity) person.getSelectedPlan().getPlanElements().get(0) ;

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
