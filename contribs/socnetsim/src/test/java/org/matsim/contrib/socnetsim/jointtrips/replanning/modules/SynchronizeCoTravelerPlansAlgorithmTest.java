/* *********************************************************************** *
 * project: org.matsim.*
 * SynchronizeCoTravelerPlansAlgorithmTest.java
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
package org.matsim.contrib.socnetsim.jointtrips.replanning.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.testcases.MatsimTestUtils;

import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.framework.population.JointPlan;
import org.matsim.contrib.socnetsim.framework.population.JointPlanFactory;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;

/**
 * @author thibautd
 */
public class SynchronizeCoTravelerPlansAlgorithmTest {
	private final List<Fixture> fixtures = new ArrayList<Fixture>();

	// /////////////////////////////////////////////////////////////////////////
	// fixtures
	// /////////////////////////////////////////////////////////////////////////
	private static class Fixture {
		final JointPlan jointPlan;
		final Map<Activity, Double> expectedEndTimes;

		public Fixture(
				final JointPlan jp,
				final Map<Activity, Double> expectedEndTimes) {
			this.jointPlan = jp;
			this.expectedEndTimes = expectedEndTimes;
		}
	}

	@AfterEach
	public void clear() {
		fixtures.clear();
	}

	@BeforeEach
	public void createSimpleFixture() {
		final FixtureBuilder builder = new FixtureBuilder();

		final Id<Person> driverId = Id.create( "driver" , Person.class );
		final Id<Person> passengerId1 = Id.create( "p1" , Person.class );
		final Id<Person> passengerId2 = Id.create( "p2" , Person.class );

		final Id<Link> link1 = Id.create( "link1" , Link.class );
		final Id<Link> link2 = Id.create( "link2" , Link.class );
		final Id<Link> link3 = Id.create( "link3" , Link.class );
		final Id<Link> link4 = Id.create( "link4" , Link.class );

		// DRIVER
		builder.startPerson( driverId );

		builder.startActivity( "h" , link1 );
		builder.setCurrentActivityEndTime( 100 );

		builder.startLeg( TransportMode.car , 100 );

		builder.startActivity( JointActingTypes.INTERACTION , link2 );

		builder.startLeg( JointActingTypes.DRIVER , 100 );
		final DriverRoute dr1 = new DriverRoute( link2 , link3 );
		dr1.addPassenger( passengerId1 );
		builder.setCurrentLegRoute( dr1 );

		builder.startActivity( JointActingTypes.INTERACTION , link3 );

		builder.startLeg( JointActingTypes.DRIVER , 100 );
		final DriverRoute dr2 = new DriverRoute( link3 , link4 );
		dr2.addPassenger( passengerId1 );
		dr2.addPassenger( passengerId2 );
		builder.setCurrentLegRoute( dr2 );

		builder.startActivity( JointActingTypes.INTERACTION , link4 );

		builder.startLeg( TransportMode.car , 100 );

		builder.startActivity( "h" , link1 );

		// PASSENGER 1
		builder.startPerson( passengerId1 );

		builder.startActivity( "h" , link3 );
		builder.setCurrentActivityEndTime( 200 );
		builder.setCurrentActivityExpectedEndTime( 0d );

		builder.startLeg( TransportMode.walk , 200 );

		builder.startActivity( JointActingTypes.INTERACTION , link2 );

		builder.startLeg( JointActingTypes.PASSENGER , 200 );
		final PassengerRoute pr1 = new PassengerRoute( link2 , link4 );
		pr1.setDriverId( driverId );
		builder.setCurrentLegRoute( pr1 );

		builder.startActivity( JointActingTypes.INTERACTION , link4 );

		builder.startLeg( TransportMode.car , 200 );

		builder.startActivity( "h" , link1 );

		// PASSENGER 2
		builder.startPerson( passengerId2 );

		builder.startActivity( "h" , link2 );
		builder.setCurrentActivityEndTime( 50 );
		builder.setCurrentActivityExpectedEndTime(  250d );

		builder.startLeg( TransportMode.walk );
		final Route walkRoute = RouteUtils.createGenericRouteImpl(link2, link3);
		walkRoute.setTravelTime( 50 );
		builder.setCurrentLegRoute( walkRoute );

		builder.startActivity( JointActingTypes.INTERACTION , link3 );

		builder.startLeg( JointActingTypes.PASSENGER );
		final PassengerRoute pr2 = new PassengerRoute( link3 , link4 );
		pr2.setDriverId( driverId );
		pr2.setTravelTime( 50 );
		builder.setCurrentLegRoute( pr2 );

		builder.startActivity( JointActingTypes.INTERACTION , link4 );

		builder.startLeg( TransportMode.walk , 50 );

		builder.startActivity( "h" , link2 );

		fixtures.add( builder.build() );
	}

	@BeforeEach
	public void createFixtureWithPotentiallyNegativeEndTimes() {
		final FixtureBuilder builder = new FixtureBuilder();

		final Id<Person> driverId = Id.create( "driver" , Person.class );
		final Id<Person> passengerId1 = Id.create( "p1" , Person.class );
		final Id<Person> passengerId2 = Id.create( "p2" , Person.class );

		final Id<Link> link1 = Id.create( "link1" , Link.class );
		final Id<Link> link2 = Id.create( "link2" , Link.class );
		final Id<Link> link3 = Id.create( "link3" , Link.class );
		final Id<Link> link4 = Id.create( "link4" , Link.class );

		// DRIVER
		builder.startPerson( driverId );

		builder.startActivity( "h" , link1 );
		builder.setCurrentActivityEndTime( 100 );

		builder.startLeg( TransportMode.car , 100 );

		builder.startActivity( JointActingTypes.INTERACTION , link2 );

		builder.startLeg( JointActingTypes.DRIVER , 100 );
		final DriverRoute dr1 = new DriverRoute( link2 , link3 );
		dr1.addPassenger( passengerId1 );
		builder.setCurrentLegRoute( dr1 );

		builder.startActivity( JointActingTypes.INTERACTION , link3 );

		builder.startLeg( JointActingTypes.DRIVER , 100 );
		final DriverRoute dr2 = new DriverRoute( link3 , link4 );
		dr2.addPassenger( passengerId1 );
		dr2.addPassenger( passengerId2 );
		builder.setCurrentLegRoute( dr2 );

		builder.startActivity( JointActingTypes.INTERACTION , link4 );

		builder.startLeg( TransportMode.car , 100 );

		builder.startActivity( "h" , link1 );

		// PASSENGER 1
		builder.startPerson( passengerId1 );

		builder.startActivity( "h" , link3 );
		builder.setCurrentActivityEndTime( 100 );
		builder.setCurrentActivityExpectedEndTime( 0d );

		builder.startLeg( TransportMode.walk , 2000 );

		builder.startActivity( JointActingTypes.INTERACTION , link2 );

		builder.startLeg( JointActingTypes.PASSENGER , 200 );
		final PassengerRoute pr1 = new PassengerRoute( link2 , link4 );
		pr1.setDriverId( driverId );
		builder.setCurrentLegRoute( pr1 );

		builder.startActivity( JointActingTypes.INTERACTION , link4 );

		builder.startLeg( TransportMode.car , 200 );

		builder.startActivity( "h" , link1 );

		// PASSENGER 2
		builder.startPerson( passengerId2 );

		builder.startActivity( "h" , link2 );
		builder.setCurrentActivityEndTime( 50 );
		builder.setCurrentActivityExpectedEndTime( 0 );

		builder.startLeg( TransportMode.walk );
		final Route walkRoute = RouteUtils.createGenericRouteImpl(link2, link3);
		walkRoute.setTravelTime( 500000000 );
		builder.setCurrentLegRoute( walkRoute );

		builder.startActivity( JointActingTypes.INTERACTION , link3 );

		builder.startLeg( JointActingTypes.PASSENGER );
		final PassengerRoute pr2 = new PassengerRoute( link3 , link4 );
		pr2.setDriverId( driverId );
		pr2.setTravelTime( 50 );
		builder.setCurrentLegRoute( pr2 );

		builder.startActivity( JointActingTypes.INTERACTION , link4 );

		builder.startLeg( TransportMode.walk , 50 );

		builder.startActivity( "h" , link2 );

		fixtures.add( builder.build() );
	}

	// to help creation of fixtures
	private static class FixtureBuilder {
		final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();
		private final Map<Id<Person>, Plan> plans = new HashMap< >();
		private Plan currentPlan = null;
		private Activity currentActivity = null;
		private Leg currentLeg = null;

		private final Map<Activity, Double> expectedEndTimes = new HashMap<Activity, Double>();

		public void startPerson(final Id id) {
			final Person person = popFact.createPerson( id );
			this.currentPlan = popFact.createPlan();
			currentPlan.setPerson( person );
			person.addPlan( currentPlan );
			plans.put( id , currentPlan );
		}

		public void startActivity(final String type, final Id link) {
			currentLeg = null;
			currentActivity = popFact.createActivityFromLinkId( type , link );
			currentPlan.addActivity( currentActivity );
		}

		public void setCurrentActivityEndTime(final double endTime) {
			currentActivity.setEndTime( endTime );
		}

		public void setCurrentActivityExpectedEndTime(final double expectedEndTime) {
			expectedEndTimes.put( currentActivity , expectedEndTime );
		}

		public void startLeg(final String transportMode, double tt) {
			startLeg( transportMode );
			currentLeg.setTravelTime( tt );
		}

		public void startLeg(final String transportMode) {
			currentActivity = null;
			currentLeg = popFact.createLeg( transportMode );
			currentPlan.addLeg( currentLeg );
		}

		public void setCurrentLegRoute(final Route route) {
			currentLeg.setRoute( route );
		}

		public Fixture build() {
			return new Fixture(
						new JointPlanFactory().createJointPlan( plans ),
						expectedEndTimes);
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	void testDepartureTimes() throws Exception {
		final SynchronizeCoTravelerPlansAlgorithm testee = new SynchronizeCoTravelerPlansAlgorithm(TimeInterpretation.create(ConfigUtils.createConfig()));
		for ( Fixture fixture : fixtures ) {
			testee.run( fixture.jointPlan );

			for ( Plan p : fixture.jointPlan.getIndividualPlans().values() ) {
				for ( Activity activity : TripStructureUtils.getActivities( p , StageActivityHandling.StagesAsNormalActivities ) ) {
					final Double endTime = fixture.expectedEndTimes.remove( activity );
					if ( endTime == null ) continue;

					Assertions.assertEquals(
							endTime.doubleValue(), activity.getEndTime().seconds(),
							MatsimTestUtils.EPSILON,
							"unexpected end time for "+activity);
				}
			}

			Assertions.assertTrue(
					fixture.expectedEndTimes.isEmpty(),
					"some activities were not found: "+fixture.expectedEndTimes.keySet() );
		}
	}


}
