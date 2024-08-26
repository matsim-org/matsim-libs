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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.jointtrips.population.DriverRoute;
import org.matsim.contrib.socnetsim.jointtrips.population.JointActingTypes;
import org.matsim.contrib.socnetsim.jointtrips.population.PassengerRoute;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterModule;
import org.matsim.core.router.costcalculators.TravelDisutilityModule;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorModule;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.facilities.ActivityFacilities;

import com.google.inject.Provider;
import com.google.inject.name.Names;

/**
 * @author thibautd
 */
public class JointTripRouterFactoryTest {
	private static final Logger log =
		LogManager.getLogger(JointTripRouterFactoryTest.class);

	private Provider<TripRouter> factory;
	private Scenario scenario;

	@BeforeEach
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
		sc.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		Network net = (Network) sc.getNetwork();
		final Id<Node> id5 = node1;
		Node node1inst = NetworkUtils.createAndAddNode(net, id5, new Coord((double) 0, (double) 1));
		final Id<Node> id6 = node2;
		Node node2inst = NetworkUtils.createAndAddNode(net, id6, new Coord((double) 0, (double) 2));
		final Id<Node> id7 = node3;
		Node node3inst = NetworkUtils.createAndAddNode(net, id7, new Coord((double) 0, (double) 3));
		final Id<Node> id8 = node4;
		Node node4inst = NetworkUtils.createAndAddNode(net, id8, new Coord((double) 0, (double) 4));
		final Id<Link> id2 = link1;
		final Node fromNode = node1inst;
		final Node toNode = node2inst;

		NetworkUtils.createAndAddLink(net,id2, fromNode, toNode, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Id<Link> id3 = link2;
		final Node fromNode1 = node2inst;
		final Node toNode1 = node3inst;
		NetworkUtils.createAndAddLink(net,id3, fromNode1, toNode1, (double) 1, (double) 1, (double) 1, (double) 1 );
		final Id<Link> id4 = link3;
		final Node fromNode2 = node3inst;
		final Node toNode2 = node4inst;
		NetworkUtils.createAndAddLink(net,id4, fromNode2, toNode2, (double) 1, (double) 1, (double) 1, (double) 1 );

		Population pop = sc.getPopulation();
		Id<Person> driverId = Id.create( "driver" , Person.class );
		Id<Person> passengerId = Id.create( "passenger" , Person.class );
		final Id<Person> id = driverId;

		// driver
		Person pers = PopulationUtils.getFactory().createPerson(id);
		Plan plan = PopulationUtils.createPlan(pers);
		pers.addPlan( plan );
		pers.setSelectedPlan( plan );
		pop.addPerson( pers );
		final Id<Link> linkId = link1;

		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", linkId).setEndTime( 32454 );
		Leg leg5 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		final Id<Link> linkId1 = link1;
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId1).setMaximumDuration( 0 );
		Leg leg4 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.DRIVER ) ;
		Leg dLeg = leg4;
		final Id<Link> linkId2 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId2).setMaximumDuration( 0 );
		Leg leg3 = PopulationUtils.createAndAddLeg( plan, TransportMode.car );
		final Id<Link> linkId3 = link3;
		PopulationUtils.createAndAddActivityFromLinkId(plan, "home", linkId3);

		DriverRoute dRoute = new DriverRoute( link1 , link3 );
		dRoute.addPassenger( passengerId );
		dLeg.setRoute( dRoute );
		final Id<Person> id1 = passengerId;

		// passenger
		pers = PopulationUtils.getFactory().createPerson(id1);
		plan = PopulationUtils.createPlan(pers);
		pers.addPlan( plan );
		pers.setSelectedPlan( plan );
		pop.addPerson( pers );
		final Id<Link> linkId4 = link1;

		Activity a = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", linkId4);
		a.setEndTime( 1246534 );
		a.setCoord(new Coord((double) 0, (double) 1));
		Leg leg2 = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		final Id<Link> linkId5 = link1;
		a = PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId5);
		a.setMaximumDuration( 0 );
		a.setCoord(new Coord((double) 0, (double) 2));
		Leg leg1 = PopulationUtils.createAndAddLeg( plan, JointActingTypes.PASSENGER );
		Leg pLeg = leg1;
		final Id<Link> linkId6 = link3;
		a = PopulationUtils.createAndAddActivityFromLinkId(plan, JointActingTypes.INTERACTION, linkId6);
		a.setMaximumDuration( 0 );
		a.setCoord(new Coord((double) 0, (double) 3));
		Leg leg = PopulationUtils.createAndAddLeg( plan, TransportMode.walk );
		final Id<Link> linkId7 = link3;
		a = PopulationUtils.createAndAddActivityFromLinkId(plan, "home", linkId7);
		a.setCoord(new Coord((double) 0, (double) 4));

		PassengerRoute pRoute = new PassengerRoute( link1 , link3 );
		pRoute.setDriverId( driverId );
		pLeg.setRoute( pRoute );

		return sc;
	}

	private static Provider<TripRouter> createFactory(final Scenario scenario) {
		com.google.inject.Injector injector = Injector.createInjector(
				scenario.getConfig(),
				AbstractModule.override(Collections.singleton(new AbstractModule() {
					@Override
					public void install() {
						bind(EventsManager.class).toInstance(EventsUtils.createEventsManager(scenario.getConfig()));
						install(new ScenarioByInstanceModule(scenario));
						install(new TripRouterModule());
						install(new TravelTimeCalculatorModule());
						install(new TravelDisutilityModule());
						install(new TimeInterpretationModule());
						bind(Integer.class).annotatedWith(Names.named("iteration")).toInstance(0);
					}
				}), new JointTripRouterModule()));

		return injector.getProvider(TripRouter.class);
	}

	@Test
	void testPassengerRoute() throws Exception {
		final PlanAlgorithm planRouter =
			new JointPlanRouterFactory( (ActivityFacilities) null, TimeInterpretation.create(ConfigUtils.createConfig()) ).createPlanRoutingAlgorithm(
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

						Assertions.assertEquals(
								driver,
								actualDriver,
								"wrong driver Id");
					}
				}

			}
		}
	}

	@Test
	void testDriverRoute() throws Exception {
		final PlanAlgorithm planRouter =
			new JointPlanRouterFactory( (ActivityFacilities) null, TimeInterpretation.create(ConfigUtils.createConfig()) ).createPlanRoutingAlgorithm(
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

						Assertions.assertEquals(
								passengerIds.size(),
								actualPassengers.size(),
								"wrong number of passengers");

						Assertions.assertTrue(
								passengerIds.containsAll( actualPassengers ),
								"wrong passengers ids: "+actualPassengers+" is not "+passengerIds);
					}
				}

			}
		}
	}
}

