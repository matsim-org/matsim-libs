/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

/**
 * @author mrieser
 */
public class TeleportationRoutingModuleTest {

	@Test
	void testRouteLeg() {
		final Scenario scenario = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		PopulationFactory populationFactory = scenario.getPopulation().getFactory();
		RouteFactories routeFactory = new RouteFactories();
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
//		Activity fromAct = PopulationUtils.createActivityFromCoord("h", new Coord(0, 0));
		Facility fromAct = scenario.getActivityFacilities().getFactory().createActivityFacility( Id.create( "h", ActivityFacility.class ),
				new Coord(0,0), Id.createLinkId( "h" ) ) ;
//		Activity toAct = PopulationUtils.createActivityFromCoord("h", new Coord(1000, 0));
		Facility toAct = scenario.getActivityFacilities().getFactory().createActivityFacility( Id.create( "h", ActivityFacility.class ),
				new Coord(1000,0), Id.createLinkId( "h" ) ) ;

		TeleportationRoutingModule router =
				new TeleportationRoutingModule(
						"mode",
						scenario, 10.0, 1.0);
		double tt = router.routeLeg(person, leg, fromAct, toAct, 7.0 * 3600);
		Assertions.assertEquals(100.0, tt, 10e-7);
		Assertions.assertEquals(100.0, leg.getTravelTime().seconds(), 10e-7);
		Assertions.assertEquals(100.0, leg.getRoute().getTravelTime().seconds(), 10e-7);

        router =
				new TeleportationRoutingModule(
						"mode",
					  scenario,
						20.0,
						1.0);
		tt = router.routeLeg(person, leg, fromAct, toAct, 7.0 * 3600);
		Assertions.assertEquals(50.0, tt, 10e-7);
		Assertions.assertEquals(50.0, leg.getTravelTime().seconds(), 10e-7);
		Assertions.assertEquals(50.0, leg.getRoute().getTravelTime().seconds(), 10e-7);

//		Activity otherToAct = PopulationUtils.createActivityFromCoord("h", new Coord(1000, 1000));
		Facility otherToAct = scenario.getActivityFacilities().getFactory().createActivityFacility( Id.create( "h", ActivityFacility.class ),
				new Coord(1000,1000), Id.createLinkId( "h" ) ) ;
		double manhattanBeelineDistanceFactor = Math.sqrt(2.0);
		router =
                new TeleportationRoutingModule(
						"mode",
				scenario,
						10.0,
						manhattanBeelineDistanceFactor);
		tt = router.routeLeg(person, leg, fromAct, otherToAct, 7.0 * 3600);
		Assertions.assertEquals(200.0, tt, 10e-7);
		Assertions.assertEquals(200.0, leg.getTravelTime().seconds(), 10e-7);
		Assertions.assertEquals(200.0, leg.getRoute().getTravelTime().seconds(), 10e-7);
	}
}
