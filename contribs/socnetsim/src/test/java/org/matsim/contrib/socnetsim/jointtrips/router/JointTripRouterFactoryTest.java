/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripRouterFactoryTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.jointtrips.router;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.population.algorithms.PlanAlgorithm;

import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author thibautd
 */
public class JointTripRouterFactoryTest {
	private static final Logger log =
		Logger.getLogger(JointTripRouterFactoryTest.class);

	private JointTripRouterFactory factory;
	private Scenario scenario;

	@Before
	public void initFixtures() {
		this.scenario = createScenario();
		this.factory = createFactory( scenario );
	}

	private static Scenario createScenario() {
		Id<Node> node1 = Id.create( "node1" , Node.class );
		Id<Node> node2 = Id.create( "node2" , Node.class );
		Id<Node> node3 = Id.create( "node3" , Node.class );
		Id<Node> node4 = Id.create( "node4" , Node.class );

		Id<Link> link1 = Id.create( "link1" , Link.class );
		Id<Link> link2 = Id.create( "link2" , Link.class );
		Id<Link> link3 = Id.create( "link3" , Link.class );

		Scenario sc = ScenarioUtils.createScenario(
				ConfigUtils.createConfig() );
		NetworkImpl net = (NetworkImpl) sc.getNetwork();
		Node node1inst = net.createAndAddNode( node1 , new Coord((double) 0, (double) 1));
		Node node2inst = net.createAndAddNode( node2 , new Coord((double) 0, (double) 2));
		Node node3inst = net.createAndAddNode( node3 , new Coord((double) 0, (double) 3));
		Node node4inst = net.createAndAddNode( node4 , new Coord((double) 0, (double) 4));

		net.createAndAddLink( link1 , node1inst , node2inst , 1 , 1 , 1 , 1 );
		net.createAndAddLink( link2 , node2inst , node3inst , 1 , 1 , 1 , 1 );
		net.createAndAddLink( link3 , node3inst , node4inst , 1 , 1 , 1 , 1 );

		Population pop = sc.getPopulation();
		Id<Person> driverId = Id.create( "driver" , Person.class );
		Id<Person> passengerId = Id.create( "passenger" , Person.class );

		// driver
		Person pers = PersonImpl.createPerson(driverId);
		PlanImpl plan = new PlanImpl( pers );
		pers.addPlan( plan );
		pers.setSelectedPlan( plan );
		pop.addPerson( pers );

		plan.createAndAddActivity( "home" , link1 ).setEndTime( 32454 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link1 ).setMaximumDuration( 0 );
		Leg dLeg = plan.createAndAddLeg( JointActingTypes.DRIVER );
		plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 ).setMaximumDuration( 0 );
		plan.createAndAddLeg( TransportMode.car );
		plan.createAndAddActivity( "home" , link3 );

		DriverRoute dRoute = new DriverRoute( link1 , link3 );
		dRoute.addPassenger( passengerId );
		dLeg.setRoute( dRoute );

		// passenger
		pers = PersonImpl.createPerson(passengerId);
		plan = new PlanImpl( pers );
		pers.addPlan( plan );
		pers.setSelectedPlan( plan );
		pop.addPerson( pers );

		ActivityImpl a = plan.createAndAddActivity( "home" , link1 );
		a.setEndTime( 1246534 );
		a.setCoord(new Coord((double) 0, (double) 1));
		plan.createAndAddLeg( TransportMode.walk );
		a = plan.createAndAddActivity( JointActingTypes.INTERACTION , link1 );
		a.setMaximumDuration( 0 );
		a.setCoord(new Coord((double) 0, (double) 2));
		Leg pLeg = plan.createAndAddLeg( JointActingTypes.PASSENGER );
		a = plan.createAndAddActivity( JointActingTypes.INTERACTION , link3 );
		a.setMaximumDuration( 0 );
		a.setCoord(new Coord((double) 0, (double) 3));
		plan.createAndAddLeg( TransportMode.walk );
		a = plan.createAndAddActivity( "home" , link3 );
		a.setCoord(new Coord((double) 0, (double) 4));

		PassengerRoute pRoute = new PassengerRoute( link1 , link3 );
		pRoute.setDriverId( driverId );
		pLeg.setRoute( pRoute );

		return sc;
	}

	private static JointTripRouterFactory createFactory( final Scenario scenario ) {
		return new JointTripRouterFactory(
				scenario,
				new TravelDisutilityFactory () {
					@Override
					public TravelDisutility createTravelDisutility(
							TravelTime timeCalculator,
							PlanCalcScoreConfigGroup cnScoringGroup) {
						return new RandomizingTimeDistanceTravelDisutility.Builder().createTravelDisutility(timeCalculator, cnScoringGroup);
					}
				},
				new FreeSpeedTravelTime(),
				new DijkstraFactory(),
				null);
	}

	@Test
	public void testPassengerRoute() throws Exception {
		final PlanAlgorithm planRouter =
			new JointPlanRouterFactory( (ActivityFacilities) null ).createPlanRoutingAlgorithm(
					factory.get() );
		for (Person pers : scenario.getPopulation().getPersons().values()) {
			final Plan plan = pers.getSelectedPlan();
			boolean toRoute = false;
			Id driver = null;

			for (PlanElement pe : plan.getPlanElements()) {
				if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.PASSENGER ) ) {
					driver = ((PassengerRoute) ((Leg) pe).getRoute()).getDriverId();
					toRoute = true;
					break;
				}
			}

			if (toRoute) {
				log.debug( "testing passenger route on plan of "+plan.getPerson().getId() );
				planRouter.run( plan );

				for (PlanElement pe : plan.getPlanElements()) {
					if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.PASSENGER ) ) {
						final Id actualDriver = ((PassengerRoute) ((Leg) pe).getRoute()).getDriverId();

						Assert.assertEquals(
								"wrong driver Id",
								driver,
								actualDriver);
					}
				}

			}
		}
	}

	@Test
	public void testDriverRoute() throws Exception {
		final PlanAlgorithm planRouter =
			new JointPlanRouterFactory( (ActivityFacilities) null ).createPlanRoutingAlgorithm(
					factory.get() );
		for (Person pers : scenario.getPopulation().getPersons().values()) {
			final Plan plan = pers.getSelectedPlan();
			final List<Id> passengerIds = new ArrayList<Id>();
			boolean toRoute = false;

			for (PlanElement pe : plan.getPlanElements()) {
				if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.DRIVER ) ) {
					passengerIds.addAll( ((DriverRoute) ((Leg) pe).getRoute()).getPassengersIds());
					toRoute = true;
					break;
				}
			}

			if (toRoute) {
				log.debug( "testing driver route on plan of "+plan.getPerson().getId() );
				planRouter.run( plan );

				for (PlanElement pe : plan.getPlanElements()) {
					if ( pe instanceof Leg && ((Leg) pe).getMode().equals(  JointActingTypes.DRIVER ) ) {
						final Collection<Id<Person>> actualPassengers = ((DriverRoute) ((Leg) pe).getRoute()).getPassengersIds();

						Assert.assertEquals(
								"wrong number of passengers",
								passengerIds.size(),
								actualPassengers.size());

						Assert.assertTrue(
								"wrong passengers ids: "+actualPassengers+" is not "+passengerIds,
								passengerIds.containsAll( actualPassengers ));
					}
				}

			}
		}
	}
}

