/* *********************************************************************** *
 * project: org.matsim.*
 * TripStructureUtilsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.router.TripStructureUtils.Trip;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class TripStructureUtilsTest {
	private static final PopulationFactory populationFactory =
            ScenarioUtils.createScenario(
	        ConfigUtils.createConfig()).getPopulation().getFactory();
    private static final String dummyType = "dummy interaction";
	private static final StageActivityTypes stageActivities =
		new StageActivityTypesImpl();

	private final List<Fixture> fixtures = new ArrayList<Fixture>();
	private static class Fixture {
		public final Plan plan;
		public final int expectedNActs;
		public final int expectedNLegs;
		public final int expectedNTrips;
		public final String name;

		public Fixture(
				final String name,
				final Plan plan,
				final int expectedNActs,
				final int expectedNLegs,
				final int expectedNTrips) {
			this.plan = plan;
			this.name = name;
			this.expectedNActs = expectedNActs;
			this.expectedNLegs = expectedNLegs;
			this.expectedNTrips = expectedNTrips;
		}
	}

	@After
	public void clean() {
		fixtures.clear();
	}

	@Before
	public void createSimpleFixture() {
		final Plan plan = populationFactory.createPlan();

		final Id<Link> linkId = Id.create( 1, Link.class );
		int nActs = 0;
		int nTrips = 0;
		int nLegs = 0;

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"breakfast",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"snack",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"lunch",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"tea",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"dinner",
					linkId ));

		fixtures.add(
				new Fixture(
					"strict alternation",
					plan,
					nActs,
					nLegs,
					nTrips));
	}

	@Before
	public void createFixtureWithComplexTrips() {
		final Plan plan = populationFactory.createPlan();

		final Id<Link> linkId = Id.create( 1, Link.class );
		int nActs = 0;
		int nTrips = 0;
		int nLegs = 0;

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"breakfast",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					dummyType,
					linkId ));
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some other mode" ) );
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"snack",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					dummyType,
					linkId ));
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some other mode" ) );
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					dummyType,
					linkId ));

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"lunch",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"tea",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					dummyType,
					linkId ));
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					dummyType,
					linkId ));
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"dinner",
					linkId ));

		fixtures.add(
				new Fixture(
					"complex trips",
					plan,
					nActs,
					nLegs,
					nTrips));
	}


	@Before
	public void createFixtureWithSuccessiveActivities() {
		final Plan plan = populationFactory.createPlan();

		final Id<Link> linkId = Id.create( 1, Link.class );
		int nActs = 0;
		int nTrips = 0;
		int nLegs = 0;

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"fry eggs",
					linkId ));
		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"breakfast",
					linkId ));
		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"wash dishes",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"snack",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"lunch",
					linkId ));
		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"coffee",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"tea",
					linkId ));

		nTrips++;
		nLegs++;
		plan.addLeg( populationFactory.createLeg( "some mode" ) );

		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"read",
					linkId ));
		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"dinner",
					linkId ));
		nActs++;
		plan.addActivity(
				populationFactory.createActivityFromLinkId(
					"sleep",
					linkId ));

		fixtures.add(
				new Fixture(
					"successive activities",
					plan,
					nActs,
					nLegs,
					nTrips));
	}

	@Test
	public void testActivities() throws Exception {
		for (Fixture fixture : fixtures) {
			final List<Activity> acts =
				TripStructureUtils.getActivities(
						fixture.plan, 
						StageActivityHandling.ExcludeStageActivities);

			assertEquals(
					"unexpected number of activities in "+acts+" for fixture "+fixture.name,
					fixture.expectedNActs,
					acts.size() );

			for (Activity act : acts) {
				assertFalse(
						"found a dummy act in "+acts+" for fixture "+fixture.name,
						stageActivities.isStageActivity( act.getType() ));
			}
		}
	}

	@Test
	public void testTrips() throws Exception {
		for (Fixture fixture : fixtures) {
			final List<Trip> trips =
				TripStructureUtils.getTrips(fixture.plan);

			assertEquals(
					"unexpected number of trips in "+trips+" for fixture "+fixture.name,
					fixture.expectedNTrips,
					trips.size() );

			for (Trip trip : trips) {
				for (PlanElement pe : trip.getTripElements()) {
					if (pe instanceof Leg) continue;
					assertTrue(
							"found a non-dummy act in "+trip.getTripElements()+" for fixture "+fixture.name,
							stageActivities.isStageActivity( ((Activity) pe).getType() ));
				}

				final int indexOfStart =
					fixture.plan.getPlanElements().indexOf(
							trip.getOriginActivity() );
				final int indexOfEnd =
					fixture.plan.getPlanElements().indexOf(
							trip.getDestinationActivity() );
				final List<PlanElement> inPlan =
					fixture.plan.getPlanElements().subList(
							indexOfStart + 1,
							indexOfEnd );

				assertEquals(
						"trip in Trip is not the same as in plan for fixture "+fixture.name,
						inPlan,
						trip.getTripElements());
			}
		}
	}

	@Test
	public void testLegs() throws Exception {
		for (Fixture fixture : fixtures) {
			final List<Trip> trips =
				TripStructureUtils.getTrips(fixture.plan);

			int countLegs = 0;
			for (Trip trip : trips) {
				countLegs += trip.getLegsOnly().size();
			}

			assertEquals(
					"getLegsOnly() does not returns the right number of legs",
					fixture.expectedNLegs,
					countLegs);
		}
	}

	@Test( expected=NullPointerException.class )
	public void testNPEWhenLocationNullInSubtourAnalysis() {
		// this may sound surprising, but for a long time the algorithm
		// was perfectly fine with that if assertions were disabled...

		final Plan plan = populationFactory.createPlan();
		// link ids are null
		plan.addActivity(
				populationFactory.createActivityFromCoord(
					"type",
						new Coord((double) 0, (double) 0)) );
		plan.addLeg( populationFactory.createLeg( "mode" ) );
		plan.addActivity(
				populationFactory.createActivityFromCoord(
					"type",
						new Coord((double) 0, (double) 0)) );

		TripStructureUtils.getSubtours( plan , EmptyStageActivityTypes.INSTANCE );
	}
}

