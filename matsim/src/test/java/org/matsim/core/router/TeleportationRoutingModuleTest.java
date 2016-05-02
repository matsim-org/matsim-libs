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

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.scenario.ScenarioUtils;

import junit.framework.Assert;

/**
 * @author mrieser
 */
public class TeleportationRoutingModuleTest {

	@Test
	public void testRouteLeg() {
		PopulationFactory populationFactory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
		RouteFactoryImpl routeFactory = new RouteFactoryImpl();
		Person person = PopulationUtils.createPerson(Id.create(1, Person.class));
		Leg leg = new LegImpl(TransportMode.walk);
		Activity fromAct = new ActivityImpl("h", new Coord(0, 0));
		Activity toAct = new ActivityImpl("h", new Coord(1000, 0));

		TeleportationRoutingModule router =
				new TeleportationRoutingModule(
						"mode",
						populationFactory,
						routeFactory, 10.0, 1.0);
		double tt = router.routeLeg(person, leg, fromAct, toAct, 7.0 * 3600);
		Assert.assertEquals(100.0, tt, 10e-7);
		Assert.assertEquals(100.0, leg.getTravelTime(), 10e-7);
		Assert.assertEquals(100.0, leg.getRoute().getTravelTime(), 10e-7);

        router =
				new TeleportationRoutingModule(
						"mode",
						populationFactory,
						routeFactory,
						20.0,
						1.0);
		tt = router.routeLeg(person, leg, fromAct, toAct, 7.0 * 3600);
		Assert.assertEquals(50.0, tt, 10e-7);
		Assert.assertEquals(50.0, leg.getTravelTime(), 10e-7);
		Assert.assertEquals(50.0, leg.getRoute().getTravelTime(), 10e-7);

		Activity otherToAct = new ActivityImpl("h", new Coord(1000, 1000));
		double manhattanBeelineDistanceFactor = Math.sqrt(2.0);
		router =
                new TeleportationRoutingModule(
						"mode",
						populationFactory,
						routeFactory,
						10.0,
						manhattanBeelineDistanceFactor);
		tt = router.routeLeg(person, leg, fromAct, otherToAct, 7.0 * 3600);
		Assert.assertEquals(200.0, tt, 10e-7);
		Assert.assertEquals(200.0, leg.getTravelTime(), 10e-7);
		Assert.assertEquals(200.0, leg.getRoute().getTravelTime(), 10e-7);
	}
}
