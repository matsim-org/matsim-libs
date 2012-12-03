/* *********************************************************************** *
 * project: org.matsim.*
 * RemoveJointTripsTest.java
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;

import playground.thibautd.socnetsim.population.JointActingTypes;

/**
 * @author thibautd
 */
public class RemoveJointTripsTest {
	/*
	 * "network" (the same link ids are used in both directions):
	 *
	 *   home      1         2        3        4       work
	 * |--------|--------|--------|--------|--------|--------|
	 */
	private final Id homeLink = new IdImpl( "home" );
	private final Id link1 = new IdImpl( "1" );
	private final Id link2 = new IdImpl( "2" );
	private final Id link3 = new IdImpl( "3" );
	private final Id link4 = new IdImpl( "4" );
	private final Id workLink = new IdImpl( "work" );

	private PlanImpl testPlan;

	@Before
	public void init() {
		testPlan = new PlanImpl();

		testPlan.createAndAddActivity( "home" , homeLink );
		Leg leg = testPlan.createAndAddLeg( "car" );
		NetworkRoute route = new LinkNetworkRouteImpl( homeLink , link2 );
		route.setLinkIds( homeLink , Arrays.asList( link1 ) , link2 );
		leg.setRoute ( route );

		testPlan.createAndAddActivity( JointActingTypes.PICK_UP , link2 );
		leg = testPlan.createAndAddLeg( "car" );
		route = new LinkNetworkRouteImpl( link2 , link3 );
		route.setLinkIds( link2 , new ArrayList<Id>() , link3 );
		leg.setRoute ( route );

		testPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link3 );
		leg = testPlan.createAndAddLeg( "car" );
		route = new LinkNetworkRouteImpl( link2 , workLink );
		route.setLinkIds( link3 , Arrays.asList( link4 ) , workLink );
		leg.setRoute ( route );

		testPlan.createAndAddActivity( "work" , workLink );
		leg = testPlan.createAndAddLeg( "car" );
		route = new LinkNetworkRouteImpl( workLink , link3 );
		route.setLinkIds( workLink , Arrays.asList( link4 ) , link3 );
		leg.setRoute ( route );

		testPlan.createAndAddActivity( JointActingTypes.PICK_UP , link3 );
		leg = testPlan.createAndAddLeg( "car" );
		route = new LinkNetworkRouteImpl( link3 , link2 );
		route.setLinkIds( link3 , new ArrayList<Id>() , link2 );
		leg.setRoute ( route );

		testPlan.createAndAddActivity( JointActingTypes.DROP_OFF , link2 );
		leg = testPlan.createAndAddLeg( "car" );
		route = new LinkNetworkRouteImpl( link2 , homeLink );
		route.setLinkIds( link2 , Arrays.asList( link1 ) , homeLink );
		leg.setRoute ( route );

		testPlan.createAndAddActivity( "home" , homeLink );
	}

	@Test
	public void testRoutes() {
		RemoveJointTrips.removeJointTrips( testPlan );

		for (PlanElement pe : testPlan.getPlanElements()) {
			if (pe instanceof Leg) {
				Route route = ((Leg) pe).getRoute();
				Assert.assertNull(
						"unexpected route "+route+" in "+pe+" in "+testPlan.getPlanElements(),
						route);
			}
		}
	}

	@Test
	public void testLength() {
		Assert.assertEquals(
				"unexpected initial size for "+testPlan.getPlanElements(),
				13,
				testPlan.getPlanElements().size());

		RemoveJointTrips.removeJointTrips( testPlan );

		Assert.assertEquals(
				"unexpected initial size after removal for "+testPlan.getPlanElements(),
				5,
				testPlan.getPlanElements().size());
	}
}

