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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author thibautd
 */
public class TripStructureUtilsTest {
	private static final Logger log = LogManager.getLogger( TripStructureUtilsTest.class ) ;
	private static final PopulationFactory populationFactory =
            ScenarioUtils.createScenario(
	        ConfigUtils.createConfig()).getPopulation().getFactory();
    private static final String dummyType = "dummy interaction";
	private static final String WITH_ACCESS_EGRESS = "with access/egress";

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

	@AfterEach
	public void clean() {
		fixtures.clear();
	}

	@BeforeEach
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

	@BeforeEach
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


	@BeforeEach
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

	@BeforeEach
	public void createFixtureWithAccessEgress() {
		final Plan plan = populationFactory.createPlan() ;

		Id<Link> linkId = Id.createLinkId( 1 );
		int nActs = 0;
		int nTrips = 0;
		int nLegs = 0;

		nActs++ ;
		plan.addActivity( populationFactory.createActivityFromLinkId( "home", linkId ) );

		nTrips++ ;

		nLegs++ ;
		plan.addLeg( populationFactory.createLeg( "walk mode" ) );

//		nActs++ ; // stage activities are not counted here
		plan.addActivity( populationFactory.createActivityFromLinkId( TripStructureUtils.createStageActivityType( "access mode" ), linkId ) );

		nLegs++ ;
		plan.addLeg( populationFactory.createLeg( "access mode" ) );

//		nActs++ ;
		plan.addActivity( populationFactory.createActivityFromLinkId( TripStructureUtils.createStageActivityType( "access mode" ), linkId ) );

		nLegs++ ;
		plan.addLeg( populationFactory.createLeg( "walk mode" ) );

//		nActs++ ;
		plan.addActivity( populationFactory.createActivityFromLinkId( TripStructureUtils.createStageActivityType( "main mode" ), linkId ) );

		nLegs++ ;
		plan.addLeg( populationFactory.createLeg( "main mode" ) );

//		nActs++ ;
		plan.addActivity( populationFactory.createActivityFromLinkId( TripStructureUtils.createStageActivityType( "main mode" ), linkId ) );

		nLegs++ ;
		plan.addLeg( populationFactory.createLeg( "walk mode" ) );

		nActs++ ;
		plan.addActivity( populationFactory.createActivityFromLinkId( "work", linkId ) );

		fixtures.add(  new Fixture( WITH_ACCESS_EGRESS, plan, nActs, nLegs, nTrips ) );
	}


	@Test
	void testActivities() throws Exception {
		for (Fixture fixture : fixtures) {
			final List<Activity> acts =
				TripStructureUtils.getActivities(
						fixture.plan,
						StageActivityHandling.ExcludeStageActivities);

			assertEquals(
					fixture.expectedNActs,
					acts.size(),
					"unexpected number of activities in "+acts+" for fixture "+fixture.name );

			for (Activity act : acts) {
				assertFalse(
						StageActivityTypeIdentifier.isStageActivity( act.getType() ),
						"found a dummy act in "+acts+" for fixture "+fixture.name);
			}
		}
	}

	@Test
	void testTrips() throws Exception {
		for (Fixture fixture : fixtures) {
			final List<Trip> trips =
				TripStructureUtils.getTrips(fixture.plan);

			assertEquals(
					fixture.expectedNTrips,
					trips.size(),
					"unexpected number of trips in "+trips+" for fixture "+fixture.name );

			for (Trip trip : trips) {
				for (PlanElement pe : trip.getTripElements()) {
					if (pe instanceof Leg) continue;
					assertTrue(
							StageActivityTypeIdentifier.isStageActivity( ((Activity) pe).getType() ),
							"found a non-dummy act in "+trip.getTripElements()+" for fixture "+fixture.name);
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
						inPlan,
						trip.getTripElements(),
						"trip in Trip is not the same as in plan for fixture "+fixture.name);
			}
		}
	}

	@Test
	void testLegs() throws Exception {
		for (Fixture fixture : fixtures) {
			final List<Trip> trips =
				TripStructureUtils.getTrips(fixture.plan);

			int countLegs = 0;
			for (Trip trip : trips) {
				countLegs += trip.getLegsOnly().size();
			}

			assertEquals(
					fixture.expectedNLegs,
					countLegs,
					"getLegsOnly() does not returns the right number of legs");
		}
	}


	@Test
	void testNPEWhenLocationNullInSubtourAnalysis() {
		assertThrows(NullPointerException.class, () -> {
			// this may sound surprising, but for a long time the algorithm
			// was perfectly fine with that if assertions were disabled...

			final Plan plan = populationFactory.createPlan();

			// link ids are null
			plan.addActivity(
					populationFactory.createActivityFromCoord(
							"type",
							new Coord((double) 0, (double) 0)));
			plan.addLeg(populationFactory.createLeg("mode"));
			plan.addActivity(
					populationFactory.createActivityFromCoord(
							"type",
							new Coord((double) 0, (double) 0)));

			TripStructureUtils.getSubtours(plan);
		});
	}

	@Test
	void testSubtourCoords() {

		final Plan plan = populationFactory.createPlan();

		// link ids are null
		plan.addActivity(populationFactory.createActivityFromCoord("type", new Coord(0,  0)) );
		plan.addLeg( populationFactory.createLeg( "mode" ) );
		plan.addActivity(populationFactory.createActivityFromCoord("type", new Coord( 50, 50)) );
		plan.addLeg( populationFactory.createLeg( "mode" ) );
		plan.addActivity(populationFactory.createActivityFromCoord("type", new Coord( 10, 10)) );
		plan.addLeg( populationFactory.createLeg( "mode" ) );
		plan.addActivity(populationFactory.createActivityFromCoord("type", new Coord( 0, 0)) );

		Collection<TripStructureUtils.Subtour> st = TripStructureUtils.getSubtours(plan, 14);

		st.forEach(System.out::println);

		System.out.println("---");

		assert st.size() == 1;

		// distance between 10,10 and 0,0 is sqrt(200), above this threshold there will be one more subtour
		st = TripStructureUtils.getSubtours(plan, 15);

		st.forEach(System.out::println);

		assert st.size() == 2;

	}

	@Test
	void testFindTripAtPlanElement() {
		Fixture theFixture = null ;
		for( Fixture fixture : fixtures ){
			if ( fixture.name.equals( WITH_ACCESS_EGRESS ) ){
				theFixture = fixture;
				log.info( "" );
				for( PlanElement planElement : fixture.plan.getPlanElements() ){
					log.info( planElement );
				}
				log.info( "" );
			}
		}
		{
			Fixture f0 = theFixture ;
			final Leg leg = (Leg) f0.plan.getPlanElements().get( 3 );
			{
				Trip trip = TripStructureUtils.findTripAtPlanElement( leg, f0.plan );
				log.info( "" );
				log.info( "Trip=" );
				for( PlanElement tripElement : trip.getTripElements() ){
					log.info( tripElement );
				}
				log.info( "" );
				Assertions.assertEquals( 9, trip.getTripElements().size() );
				Assertions.assertEquals( 5, trip.getLegsOnly().size() );
			}
			{
				Trip trip = TripStructureUtils.findTripAtPlanElement( leg, f0.plan, TripStructureUtils::isStageActivityType ) ;
				log.info( "" );
				log.info( "Trip=" );
				for( PlanElement tripElement : trip.getTripElements() ){
					log.info( tripElement );
				}
				log.info( "" );
				Assertions.assertEquals( 9, trip.getTripElements().size() );
				Assertions.assertEquals( 5, trip.getLegsOnly().size() );
			}
			{
				Trip trip = TripStructureUtils.findTripAtPlanElement( leg, f0.plan, TripStructureUtils.createStageActivityType(leg.getMode())::equals ) ;
				log.info( "" );
				log.info( "Trip=" );
				for( PlanElement tripElement : trip.getTripElements() ){
					log.info( tripElement );
				}
				log.info( "" );
				Assertions.assertEquals( 5, trip.getTripElements().size() );
				Assertions.assertEquals( 3, trip.getLegsOnly().size() );
			}
		}

	}
}

