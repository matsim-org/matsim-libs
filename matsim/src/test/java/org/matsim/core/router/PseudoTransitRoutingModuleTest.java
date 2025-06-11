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

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

public class PseudoTransitRoutingModuleTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRouteLeg() {
		final Fixture f = new Fixture();
		FreespeedTravelTimeAndDisutility freespeed = new FreespeedTravelTimeAndDisutility(-6.0/3600, +6.0/3600, 0.0);
		LeastCostPathCalculator routeAlgo = new Dijkstra(f.s.getNetwork(), freespeed, freespeed);

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Leg leg = PopulationUtils.createLeg(TransportMode.pt);
		Activity fromAct = PopulationUtils.createActivityFromCoord("h", new Coord(0, 0));
		fromAct.setLinkId(Id.create("1", Link.class));
		Activity toAct = PopulationUtils.createActivityFromCoord("h", new Coord(0, 3000));
		toAct.setLinkId(Id.create("3", Link.class));

		{
			TeleportedModeParams params = new TeleportedModeParams("mode") ;
			params.setTeleportedModeFreespeedFactor(2.);
			params.setBeelineDistanceFactor(1.);
			double tt = new FreespeedFactorRoutingModule(
					"mode", f.s.getPopulation().getFactory(),
					f.s.getNetwork(), routeAlgo, params).routeLeg(person, leg, fromAct, toAct, 7.0*3600);
			Assertions.assertEquals(400.0, tt, 1e-8);
			Assertions.assertEquals(400.0, leg.getTravelTime().seconds(), 1e-8);
//			Assert.assertTrue(leg.getRoute() instanceof GenericRouteImpl);
			Assertions.assertEquals(3000.0, leg.getRoute().getDistance(), 1e-8);
		}{
			TeleportedModeParams params = new TeleportedModeParams("mode") ;
			params.setTeleportedModeFreespeedFactor(3.);
			params.setBeelineDistanceFactor(2.);
			double tt = new FreespeedFactorRoutingModule(
					"mode", f.s.getPopulation().getFactory(),
					f.s.getNetwork(), routeAlgo, params).routeLeg(person, leg, fromAct, toAct, 7.0*3600);
			Assertions.assertEquals(600.0, tt, 1e-8);
			Assertions.assertEquals(600.0, leg.getTravelTime().seconds(), 1e-8);
			Assertions.assertEquals(6000.0, leg.getRoute().getDistance(), 1e-8);
		}{
			// the following test is newer than the ones above.  I wanted to test the freespeed limit.  But could not do it in the same way
			// above since it is not in FreespeedTravelTimeAndDisutility.  Could have modified that disutility.  But preferred to test in context.
			// Thus the more complicated injector thing.  kai, nov'16

			TeleportedModeParams params = new TeleportedModeParams("mode") ;
			params.setTeleportedModeFreespeedFactor(2.);
			params.setBeelineDistanceFactor(1.);
			params.setTeleportedModeFreespeedLimit(5.);
			f.s.getConfig().routing().addModeRoutingParams(params);
			f.s.getConfig().controller().setOutputDirectory(utils.getOutputDirectory());

			com.google.inject.Injector injector = Injector.createInjector(f.s.getConfig(), new AbstractModule() {
				@Override public void install() {
					install(new NewControlerModule());
					install(new ControlerDefaultCoreListenersModule());
					install(new ControlerDefaultsModule()) ;
					install(new ScenarioByInstanceModule(f.s));
				}
			});

			TripRouter tripRouter = injector.getInstance(TripRouter.class) ;

			Facility fromFacility = FacilitiesUtils.toFacility(fromAct, f.s.getActivityFacilities() ) ;
			Facility toFacility = FacilitiesUtils.toFacility(toAct, f.s.getActivityFacilities() );

			List<? extends PlanElement> result = tripRouter.calcRoute("mode", fromFacility, toFacility, 7.0*3600., person, new AttributesImpl()) ;
			Gbl.assertIf( result.size()==1);
			Leg newLeg = (Leg) result.get(0) ;

			Assertions.assertEquals(800.0, newLeg.getTravelTime().seconds(), 1e-8);
//			Assert.assertTrue(leg.getRoute() instanceof GenericRouteImpl);
			Assertions.assertEquals(3000.0, newLeg.getRoute().getDistance(), 1e-8);
		}
	}

	private static class Fixture {
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			s.getConfig().controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
			s.getConfig().routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);
			TeleportedModeParams walk = new TeleportedModeParams(TransportMode.walk);
			walk.setBeelineDistanceFactor(1.3);
			walk.setTeleportedModeSpeed(3.0 / 3.6);
			s.getConfig().routing().addModeRoutingParams(walk);

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
			Link l2 = nf.createLink(Id.create("2", Link.class), n2, n3);
			Link l3 = nf.createLink(Id.create("3", Link.class), n3, n4);
			l1.setFreespeed(10.0);
			l2.setFreespeed(10.0);
			l3.setFreespeed(10.0);
			l1.setLength(1000.0);
			l2.setLength(1000.0);
			l3.setLength(1000.0);
			net.addLink(l1);
			net.addLink(l2);
			net.addLink(l3);
		}
	}
}
