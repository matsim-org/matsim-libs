/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.core.router.costcalculators;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.NetworkRoutingModule;
import org.matsim.core.router.TripRouterFactoryBuilderWithDefaults;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

public class RandomizingTimeDistanceTravelDisutilityTest {

	@Test
	void testRoutesForDifferentSigmas() {

		{
			Set<String> routes = new HashSet<>();
			for (int i = 0; i<=5; i++) {
				NetworkRoute route = computeRoute(0.);
				routes.add(route.getLinkIds().toString());
			}
			System.out.println("Route (sigma = 0.0): " + routes.toString());
			Assertions.assertEquals(1, routes.size(), "There should only be a single route in the sigma = 0 case.");
		}

		{
			Set<String> routes = new HashSet<>();
			for (int i = 0; i<=5; i++) {
				NetworkRoute route = computeRoute(3.);
				routes.add(route.getLinkIds().toString());
			}
			System.out.println("Route (sigma = 3.0): " + routes.toString());
			Assertions.assertEquals(2, routes.size(), "There should be two routes in the sigma = 3 case.");
		}
	}

	public NetworkRoute computeRoute(double sigma) {
		Fixture f = new Fixture();
//		PlanCalcScoreConfigGroup planCalcScoreCfg = new PlanCalcScoreConfigGroup();
		Config config = ConfigUtils.createConfig();
		ScoringConfigGroup planCalcScoreCfg = config.scoring();
		ModeParams modeParams = new ModeParams(TransportMode.car);
		modeParams.setMonetaryDistanceRate(-0.1);
		planCalcScoreCfg.addModeParams(modeParams);
		config.routing().setRoutingRandomness( sigma );

		RandomizingTimeDistanceTravelDisutilityFactory factory = new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, config);
                TravelTimeCalculator.Builder builder = new TravelTimeCalculator.Builder(f.s.getNetwork());
		TravelTimeCalculator calculator = builder.build();

		TravelTime travelTime = calculator.getLinkTravelTimes();
		TravelDisutility disutility = factory.createTravelDisutility(travelTime);
		LeastCostPathCalculator router = TripRouterFactoryBuilderWithDefaults.createDefaultLeastCostPathCalculatorFactory(f.s).createPathCalculator(f.s.getNetwork(), disutility, travelTime);

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Activity fromAct = PopulationUtils.createActivityFromCoord("h", new Coord(0, 0));
		fromAct.setLinkId(Id.create("1", Link.class));
		Activity toAct = PopulationUtils.createActivityFromCoord("h", new Coord(0, 3000));
		toAct.setLinkId(Id.create("3", Link.class));

		final NetworkRoutingModule routingModule = new NetworkRoutingModule(
		            TransportMode.car,
		            f.s.getPopulation().getFactory(),
		            f.s.getNetwork(),
		            router);
		Facility fromFacility = FacilitiesUtils.toFacility( fromAct, f.s.getActivityFacilities() );
		Facility toFacility = FacilitiesUtils.toFacility( toAct, f.s.getActivityFacilities() );
		List<? extends PlanElement> result = routingModule.calcRoute(DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, 7.0*3600, person)) ;
		Assertions.assertEquals(1, result.size() );
		Leg leg = (Leg) result.get(0) ;
		return (NetworkRoute) leg.getRoute();
	}

	private static class Fixture {
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			Network net = this.s.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node n1 = nf.createNode(Id.create("1", Node.class), new Coord(0, 0));
			Node n2 = nf.createNode(Id.create("2", Node.class), new Coord(0, 1000));
			Node n3 = nf.createNode(Id.create("3", Node.class), new Coord(0, 2000));
			Node n4 = nf.createNode(Id.create("4", Node.class), new Coord(0, 3000));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			net.addNode(n4);
			Link l1 = nf.createLink(Id.create("1", Link.class), n1, n2);
			Link l2a = nf.createLink(Id.create("2a", Link.class), n2, n3);
			Link l2b = nf.createLink(Id.create("2b", Link.class), n2, n3);
			Link l3 = nf.createLink(Id.create("3", Link.class), n3, n4);
			l1.setFreespeed(10.0);
			l2a.setFreespeed(5.0);
			l2b.setFreespeed(20.0);
			l3.setFreespeed(10.0);
			l1.setLength(1000.0);
			l2a.setLength(1000.0);
			l2b.setLength(2000.0);
			l3.setLength(1000.0);
			net.addLink(l1);
			net.addLink(l2a);
			net.addLink(l2b);
			net.addLink(l3);
		}
	}
}
