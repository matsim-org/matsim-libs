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
package playground.thibautd.socnetsim.replanning.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import playground.thibautd.socnetsim.population.DriverRoute;
import playground.thibautd.socnetsim.population.JointActingTypes;
import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.population.JointPlanFactory;
import playground.thibautd.socnetsim.population.PassengerRoute;

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

	@After
	public void clear() {
		fixtures.clear();
	}

	@Before
	public void createSimpleFixture() {
		final PopulationFactory popFact = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getPopulation().getFactory();
		final Map<Id, Plan> plans = new HashMap<Id, Plan>();
		final Map<Activity, Double> expectedEndTimes = new HashMap<Activity, Double>();

		final Id driverId = new IdImpl( "driver" );
		final Id passengerId1 = new IdImpl( "p1" );
		final Id passengerId2 = new IdImpl( "p2" );

		final Id link1 = new IdImpl( "link1" );
		final Id link2 = new IdImpl( "link2" );
		final Id link3 = new IdImpl( "link3" );
		final Id link4 = new IdImpl( "link4" );

		// DRIVER
		final Person person1 = popFact.createPerson( driverId );
		final Plan plan1 = popFact.createPlan();
		plan1.setPerson( person1 );
		person1.addPlan( plan1 );
		plans.put( driverId , plan1 );

		final Activity originD = popFact.createActivityFromLinkId( "h" , link1 );
		originD.setEndTime( 100 );
		plan1.addActivity( originD );

		final Leg leg1D = popFact.createLeg( TransportMode.car );
		leg1D.setTravelTime( 100 );
		plan1.addLeg( leg1D );

		final Activity puD = popFact.createActivityFromLinkId( JointActingTypes.PICK_UP , link2 );
		plan1.addActivity( puD );

		final Leg leg2D = popFact.createLeg( JointActingTypes.DRIVER );
		final DriverRoute dr1 = new DriverRoute( link2 , link3 );
		dr1.addPassenger( passengerId1 );
		leg2D.setRoute( dr1 );
		leg2D.setTravelTime( 100 );
		plan1.addLeg( leg2D );

		final Activity pu2D = popFact.createActivityFromLinkId( JointActingTypes.PICK_UP , link3 );
		plan1.addActivity( pu2D );

		final Leg leg3D = popFact.createLeg( JointActingTypes.DRIVER );
		final DriverRoute dr2 = new DriverRoute( link3 , link4 );
		dr2.addPassenger( passengerId1 );
		dr2.addPassenger( passengerId2 );
		leg3D.setRoute( dr2 );
		dr2.setTravelTime( 100 );
		plan1.addLeg( leg3D );

		final Activity doD = popFact.createActivityFromLinkId( JointActingTypes.DROP_OFF , link4 );
		plan1.addActivity( doD );

		final Leg leg4D = popFact.createLeg( TransportMode.car );
		leg4D.setTravelTime( 100 );
		plan1.addLeg( leg4D );

		final Activity destD = popFact.createActivityFromLinkId( "h" , link1 );
		plan1.addActivity( destD );

		// PASSENGER 1
		final Person person2 = popFact.createPerson( passengerId1 );
		final Plan plan2 = popFact.createPlan();
		plan2.setPerson( person2 );
		person2.addPlan( plan2 );
		plans.put( passengerId1 , plan2 );

		final Activity originP1 = popFact.createActivityFromLinkId( "h" , link3 );
		originP1.setEndTime( 200 );
		plan2.addActivity( originP1 );
		expectedEndTimes.put( originP1 , 0d );

		final Leg leg1P1 = popFact.createLeg( TransportMode.walk );
		leg1P1.setTravelTime( 200 );
		plan2.addLeg( leg1P1 );

		final Activity puP1 = popFact.createActivityFromLinkId( JointActingTypes.PICK_UP , link2 );
		plan2.addActivity( puP1 );

		final Leg leg2P1 = popFact.createLeg( JointActingTypes.PASSENGER );
		final PassengerRoute pr1 = new PassengerRoute( link2 , link4 );
		pr1.setDriverId( driverId );
		leg2P1.setRoute( pr1 );
		leg2P1.setTravelTime( 200 );
		plan2.addLeg( leg2P1 );

		final Activity doP1 = popFact.createActivityFromLinkId( JointActingTypes.DROP_OFF , link4 );
		plan2.addActivity( doP1 );

		final Leg leg3P1 = popFact.createLeg( TransportMode.car );
		leg3P1.setTravelTime( 200 );
		plan2.addLeg( leg3P1 );

		final Activity destP1 = popFact.createActivityFromLinkId( "h" , link1 );
		plan2.addActivity( destP1 );

		// PASSENGER 2
		final Person person3 = popFact.createPerson( passengerId2 );
		final Plan plan3 = popFact.createPlan();
		plan3.setPerson( person3 );
		person3.addPlan( plan3 );
		plans.put( passengerId2 , plan3 );

		final Activity originP2 = popFact.createActivityFromLinkId( "h" , link2 );
		originP1.setEndTime( 50 );
		plan3.addActivity( originP2 );
		expectedEndTimes.put( originP2 , 250d );

		final Leg leg1P2 = popFact.createLeg( TransportMode.walk );
		leg1P2.setRoute( new GenericRouteImpl( link2 , link3 ) );
		leg1P2.getRoute().setTravelTime( 50 );
		plan3.addLeg( leg1P2 );

		final Activity puP2 = popFact.createActivityFromLinkId( JointActingTypes.PICK_UP , link3 );
		plan3.addActivity( puP2 );

		final Leg leg2P2 = popFact.createLeg( JointActingTypes.PASSENGER );
		final PassengerRoute pr2 = new PassengerRoute( link3 , link4 );
		leg2P2.setRoute( pr2 );
		pr2.setDriverId( driverId );
		pr2.setTravelTime( 50 );
		plan3.addLeg( leg2P2 );

		final Activity doP2 = popFact.createActivityFromLinkId( JointActingTypes.DROP_OFF , link4 );
		plan3.addActivity( doP2 );

		final Leg leg3P2 = popFact.createLeg( TransportMode.walk );
		leg3P2.setTravelTime( 50 );
		plan3.addLeg( leg3P2 );

		final Activity destP2 = popFact.createActivityFromLinkId( "h" , link2 );
		plan3.addActivity( destP2 );

		fixtures.add(
				new Fixture(
					new JointPlanFactory().createJointPlan( plans ),
					expectedEndTimes) );
	}

	// /////////////////////////////////////////////////////////////////////////
	// tests
	// /////////////////////////////////////////////////////////////////////////
	@Test
	public void testDepartureTimes() throws Exception {
		final SynchronizeCoTravelerPlansAlgorithm testee = new SynchronizeCoTravelerPlansAlgorithm( EmptyStageActivityTypes.INSTANCE );
		for ( Fixture fixture : fixtures ) {
			testee.run( fixture.jointPlan );

			for ( Plan p : fixture.jointPlan.getIndividualPlans().values() ) {
				for ( Activity activity : TripStructureUtils.getActivities( p , EmptyStageActivityTypes.INSTANCE ) ) {
					final Double endTime = fixture.expectedEndTimes.remove( activity );
					if ( endTime == null ) continue;

					Assert.assertEquals(
							"unexpected end time for "+activity,
							endTime.doubleValue(),
							activity.getEndTime(),
							MatsimTestUtils.EPSILON);
				}
			}

			Assert.assertTrue(
					"some activities were not found: "+fixture.expectedEndTimes.keySet(),
					fixture.expectedEndTimes.isEmpty() );
		}
	}
}

