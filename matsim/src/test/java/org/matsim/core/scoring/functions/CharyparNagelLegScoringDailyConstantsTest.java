/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparNagelLegScoringPtChangeTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScenarioConfigGroup;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.vehicles.Vehicle;

/**
 * @author ikaddoura
 */
public class CharyparNagelLegScoringDailyConstantsTest {
	/**
	 * Tests whether daily constants are considered in the scoring.
	 */
	@Test
	void test1() throws Exception {
		final Network network = createNetwork();
		final CharyparNagelLegScoring scoring1 = createScoringOnlyConstants( network );
		final CharyparNagelLegScoring scoring2 = createDefaultPlusConstants( network );

		final double legTravelTime1 = 123.;
		final double legTravelTime2 = 456.;
		final double legTravelTime3 = 789.;

		{
			final Leg leg = PopulationUtils.createLeg(TransportMode.car);
			leg.setDepartureTime( 0 );
			leg.setTravelTime( legTravelTime1 );

			final Event endFirstAct =  new ActivityEndEvent(
					leg.getDepartureTime().seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), Id.create( 1, ActivityFacility.class ), "start");
			scoring1.handleEvent( endFirstAct );
			scoring2.handleEvent( endFirstAct );

			final Event departure = new PersonDepartureEvent(
					leg.getDepartureTime().seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), leg.getMode(), leg.getMode());
			scoring1.handleEvent( departure );
			scoring2.handleEvent( departure );

			final Event enterVehicle = new PersonEntersVehicleEvent(
					leg.getDepartureTime().seconds() + 100, Id.create( 1, Person.class ), Id.create( 1, Vehicle.class ));
			scoring1.handleEvent( enterVehicle );
			scoring2.handleEvent( enterVehicle );

			final Event leaveVehicle = new PersonLeavesVehicleEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
					.seconds(), Id.create( 1, Person.class ), Id.create( 1, Vehicle.class ));
			scoring1.handleEvent( leaveVehicle );
			scoring2.handleEvent( leaveVehicle );

			final Event arrival = new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
					.seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), leg.getMode());
			scoring1.handleEvent( arrival );
			scoring2.handleEvent( arrival );

			scoring1.handleLeg(leg);
			scoring1.finish();

			scoring2.handleLeg(leg);
			scoring2.finish();
		}

		{
			final Leg leg = PopulationUtils.createLeg(TransportMode.car);
			leg.setDepartureTime( 0 );
			leg.setTravelTime( legTravelTime2 );

			final Event endFirstAct =  new ActivityEndEvent(
					leg.getDepartureTime().seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), Id.create( 1, ActivityFacility.class ), "start");
			scoring1.handleEvent( endFirstAct );
			scoring2.handleEvent( endFirstAct );

			final Event departure = new PersonDepartureEvent(
					leg.getDepartureTime().seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), leg.getMode(), leg.getMode());
			scoring1.handleEvent( departure );
			scoring2.handleEvent( departure );

			final Event enterVehicle = new PersonEntersVehicleEvent(
					leg.getDepartureTime().seconds() + 100, Id.create( 1, Person.class ), Id.create( 1, Vehicle.class ));
			scoring1.handleEvent( enterVehicle );
			scoring2.handleEvent( enterVehicle );

			final Event leaveVehicle = new PersonLeavesVehicleEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
					.seconds(), Id.create( 1, Person.class ), Id.create( 1, Vehicle.class ));
			scoring1.handleEvent( leaveVehicle );
			scoring2.handleEvent( leaveVehicle );

			final Event arrival = new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
					.seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), leg.getMode());
			scoring1.handleEvent( arrival );
			scoring2.handleEvent( arrival );

			scoring1.handleLeg(leg);
			scoring1.finish();

			scoring2.handleLeg(leg);
			scoring2.finish();
		}

		{
			final Leg leg = PopulationUtils.createLeg(TransportMode.bike);
			leg.setDepartureTime( 0 );
			leg.setTravelTime( legTravelTime3 );

			final Event endFirstAct =  new ActivityEndEvent(
					leg.getDepartureTime().seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), Id.create( 1, ActivityFacility.class ), "start");
			scoring1.handleEvent( endFirstAct );
			scoring2.handleEvent( endFirstAct );

			final Event departure = new PersonDepartureEvent(
					leg.getDepartureTime().seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), leg.getMode(), leg.getMode());
			scoring1.handleEvent( departure );
			scoring2.handleEvent( departure );

			final Event enterVehicle = new PersonEntersVehicleEvent(
					leg.getDepartureTime().seconds() + 100, Id.create( 1, Person.class ), Id.create( 1, Vehicle.class ));
			scoring1.handleEvent( enterVehicle );
			scoring2.handleEvent( enterVehicle );

			final Event leaveVehicle = new PersonLeavesVehicleEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
					.seconds(), Id.create( 1, Person.class ), Id.create( 1, Vehicle.class ));
			scoring1.handleEvent( leaveVehicle );
			scoring2.handleEvent( leaveVehicle );

			final Event arrival = new PersonArrivalEvent(leg.getDepartureTime().seconds() + leg.getTravelTime()
					.seconds(), Id.create( 1, Person.class ), Id.create( 1, Link.class ), leg.getMode());
			scoring1.handleEvent( arrival );
			scoring2.handleEvent( arrival );

			scoring1.handleLeg(leg);
			scoring1.finish();

			scoring2.handleLeg(leg);
			scoring2.finish();
		}

		Assertions.assertEquals(
				-12345.678,
				scoring1.getScore(),
				MatsimTestUtils.EPSILON,
				"wrong score; daily constants are not accounted for in the scoring." );

		double defaultScore = (legTravelTime1 + legTravelTime2) * new ScoringConfigGroup().getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.
				+ legTravelTime3 * new ScoringConfigGroup().getModes().get(TransportMode.bike).getMarginalUtilityOfTraveling() / 3600.;
		Assertions.assertEquals(
				-12345.678 + defaultScore,
				scoring2.getScore(),
				MatsimTestUtils.EPSILON,
				"wrong score; daily constants are not accounted for in the scoring." );
	}

	private CharyparNagelLegScoring createDefaultPlusConstants(Network network) {

		final ScoringConfigGroup conf = new ScoringConfigGroup();

		conf.getModes().get(TransportMode.car).setDailyUtilityConstant(-10000.);
		conf.getModes().get(TransportMode.car).setDailyMonetaryConstant(-2345.);

		conf.getModes().get(TransportMode.bike).setDailyUtilityConstant(-.078);
		conf.getModes().get(TransportMode.bike).setDailyMonetaryConstant(-0.6);

		final ScenarioConfigGroup scenarioConfig = new ScenarioConfigGroup();

		return new CharyparNagelLegScoring(
				new ScoringParameters.Builder(conf, conf.getScoringParameters(null), scenarioConfig).build(),
				network, new TransitConfigGroup().getTransitModes());
	}

	private static CharyparNagelLegScoring createScoringOnlyConstants(final Network network) {

		final ScoringConfigGroup conf = new ScoringConfigGroup();

		conf.getModes().get(TransportMode.car).setMarginalUtilityOfTraveling(0.);
		conf.getModes().get(TransportMode.car).setDailyUtilityConstant(-10000.);
		conf.getModes().get(TransportMode.car).setDailyMonetaryConstant(-2345.);

		conf.getModes().get(TransportMode.bike).setMarginalUtilityOfTraveling(0.);
		conf.getModes().get(TransportMode.bike).setDailyUtilityConstant(-.078);
		conf.getModes().get(TransportMode.bike).setDailyMonetaryConstant(-0.6);

		final ScenarioConfigGroup scenarioConfig = new ScenarioConfigGroup();

		return new CharyparNagelLegScoring(
				new ScoringParameters.Builder(conf, conf.getScoringParameters(null), scenarioConfig).build(),
				network, new TransitConfigGroup().getTransitModes());
	}

	private static Network createNetwork() {
		final Network network = ScenarioUtils.createScenario( ConfigUtils.createConfig() ).getNetwork();

		final Node node1 = network.getFactory().createNode( Id.create( 1, Node.class ) , new Coord((double) 0, (double) 0));
		network.addNode( node1 );
		final Node node2 = network.getFactory().createNode( Id.create( 2, Node.class ) , new Coord((double) 1, (double) 1));
		network.addNode( node2 );
		network.addLink( network.getFactory().createLink( Id.create( 1, Link.class ) , node1 , node2 ) );

		return network;
	}
}

