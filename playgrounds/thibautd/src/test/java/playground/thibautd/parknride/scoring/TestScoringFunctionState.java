/* *********************************************************************** *
 * project: org.matsim.*
 * TestPenaltyState.java
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
package playground.thibautd.parknride.scoring;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;

import playground.thibautd.parknride.ParkAndRideConstants;

/**
 * @author thibautd
 */
public class TestScoringFunctionState {
	private static final ScoringFunction DUMMY_SCORING = new ScoringFunction() {
		@Override
		public void handleActivity(Activity activity) {}
		@Override
		public void handleLeg(Leg leg) {}
		@Override
		public void agentStuck(double time) {}
		@Override
		public void addMoney(double amount) {}
		@Override
		public void finish() {}
		@Override
		public double getScore() { return 0; }
		@Override
		public void handleEvent(Event event) {
			// TODO Auto-generated method stub
			
		}
	};
	private static final ParkingPenalty DUMMY_PENALTY = new ParkingPenalty() {
		@Override
		public void park(double time, Coord coord) {}
		@Override
		public void unPark(double time) {}
		@Override
		public void finish() {}
		@Override
		public double getPenalty() { return 0; }
		@Override
		public void reset() {}
	};

	private Network network;
	private Link uniqueLink;

	private ParkAndRideScoringFunction scoringFunction;

	@Before
	public void init() {
		initNetwork();
		initScoringFunction();
	}

	private void initScoringFunction() {
		scoringFunction =
			new ParkAndRideScoringFunction(
					DUMMY_SCORING,
					DUMMY_PENALTY,
					null,
					network,
					null);

	}

	private void initNetwork() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario( config );
		network = scenario.getNetwork();


		Node node1 = network.getFactory().createNode( Id.create( 1 , Node.class ) , new Coord((double) 0, (double) 0));
		Node node2 = network.getFactory().createNode( Id.create( 2 , Node.class ) , new Coord((double) 2, (double) 2));
		network.addNode( node1 );
		network.addNode( node2 );
		uniqueLink = network.getFactory().createLink( Id.create( "toto" , Link.class ) , node1 , node2 );
		network.addLink( uniqueLink );
	}

	@Test
	public void testIsParked() throws Exception {
		test( true , false );
	}

	@Test
	public void testMode() throws Exception {
		test( false , true );
	}

	private void test( final boolean testPark , final boolean testMode ) {
		double step = 10;
		double now = step;
		Activity act = new ActivityImpl( "home" , uniqueLink.getCoord() );
		act.setEndTime( now );
		scoringFunction.handleActivity( act );

		Leg leg = new LegImpl( TransportMode.car );
		leg.setDepartureTime( now );
		leg.setTravelTime( step );
		now += step;
		scoringFunction.handleLeg( leg );

		if (testMode) {
			Assert.assertTrue(
					"car leg unidentified!",
					scoringFunction.lastLegWasCar());
		}

		act = new ActivityImpl( "work" , uniqueLink.getCoord() );
		now += step;
		act.setEndTime( now );
		scoringFunction.handleActivity( act );

		if (testPark) {
			Assert.assertTrue(
					"car not parked!",
					scoringFunction.isParked());
		}

		leg.setDepartureTime( now );
		leg.setTravelTime( step );
		now += step;
		scoringFunction.handleLeg( leg );

		act = new ActivityImpl( "work" , uniqueLink.getCoord() );
		now += step;
		act.setEndTime( now );
		scoringFunction.handleActivity( act );

		if (testPark) {
			Assert.assertTrue(
					"car not parked!",
					scoringFunction.isParked());
		}

		leg.setDepartureTime( now );
		leg.setTravelTime( step );
		now += step;
		scoringFunction.handleLeg( leg );

		act = new ActivityImpl( ParkAndRideConstants.PARKING_ACT , uniqueLink.getCoord() );
		act.setEndTime( now );
		scoringFunction.handleActivity( act );

		if (testMode) {
			Assert.assertFalse(
				"car unexpectedly parked!",
				scoringFunction.isParked());
		}

		leg.setMode( TransportMode.pt );
		leg.setDepartureTime( now );
		leg.setTravelTime( step );
		now += step;
		scoringFunction.handleLeg( leg );

		if (testMode) {
			Assert.assertFalse(
					"car leg identified whereas pt!",
					scoringFunction.lastLegWasCar());
		}
	}
	}
