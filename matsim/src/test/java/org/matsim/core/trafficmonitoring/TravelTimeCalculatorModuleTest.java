
/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorModuleTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 package org.matsim.core.trafficmonitoring;

import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

 public class TravelTimeCalculatorModuleTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	 @Test
	 void testOneTravelTimeCalculatorForAll() {
		Config config = ConfigUtils.createConfig();
		config.travelTimeCalculator().setSeparateModes(false);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Node node0 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(0), new Coord(0, 0));
		Node node1 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(1), new Coord(1, 0));
		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		Id<Link> linkId = Id.createLinkId(0);
		Link link = scenario.getNetwork().getFactory().createLink(linkId, node0, node1);
		scenario.getNetwork().addLink(link);
		var eventsManagerModule = new AbstractModule() {

			@Override
			public void install() {
				bind(EventsManager.class).to(EventsManagerImpl.class).in(Singleton.class);
			}
		};
		com.google.inject.Injector injector = Injector.createInjector(config, new TravelTimeCalculatorModule(), eventsManagerModule, new ScenarioByInstanceModule(scenario));
		TravelTimeCalculator testee = injector.getInstance(TravelTimeCalculator.class);
		EventsManager events = injector.getInstance(EventsManager.class);
		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(0), linkId));
		events.processEvent(new LinkLeaveEvent(2.0, Id.createVehicleId(0), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(2.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));

		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(1), linkId));
		events.processEvent(new LinkLeaveEvent(8.0, Id.createVehicleId(1), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(8.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));

		assertThat(testee.getLinkTravelTimes().getLinkTravelTime(link, 0.0,null,null), is(5.0));
	}


	 @Test
	 void testOneTravelTimeCalculatorPerMode() {
		Config config = ConfigUtils.createConfig();

//		config.travelTimeCalculator().setAnalyzedModesAsString("car,bike" );
		config.routing().setNetworkModes( new LinkedHashSet<>( Arrays.asList( TransportMode.car, TransportMode.bike ) ) );
		// (this is now newly taken from the router network modes. kai, feb'19)

		config.travelTimeCalculator().setSeparateModes(true);
		Scenario scenario = ScenarioUtils.createScenario(config);
		Node node0 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(0), new Coord(0, 0));
		Node node1 = scenario.getNetwork().getFactory().createNode(Id.createNodeId(1), new Coord(1, 0));
		scenario.getNetwork().addNode(node0);
		scenario.getNetwork().addNode(node1);
		Id<Link> linkId = Id.createLinkId(0);
		Link link = scenario.getNetwork().getFactory().createLink(linkId, node0, node1);
		scenario.getNetwork().addLink(link);
		var eventsManagerModule = new AbstractModule() {

			@Override
			public void install() {
				bind(EventsManager.class).to(EventsManagerImpl.class).in(Singleton.class);
			}
		};
		com.google.inject.Injector injector = Injector.createInjector(config, new TravelTimeCalculatorModule(), eventsManagerModule, new ScenarioByInstanceModule(scenario));
		TravelTimeCalculator car = injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named("car")));
		TravelTimeCalculator bike = injector.getInstance(Key.get(TravelTimeCalculator.class, Names.named("bike")));
		EventsManager events = injector.getInstance(EventsManager.class);
		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(0), linkId));
		events.processEvent(new LinkLeaveEvent(2.0, Id.createVehicleId(0), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(2.0, Id.createPersonId(0), linkId, Id.createVehicleId(0), "car", 0.0));

		events.processEvent(new VehicleEntersTrafficEvent(0.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));
		events.processEvent(new LinkEnterEvent(0.0, Id.createVehicleId(1), linkId));
		events.processEvent(new LinkLeaveEvent(8.0, Id.createVehicleId(1), linkId));
		events.processEvent(new VehicleLeavesTrafficEvent(8.0, Id.createPersonId(1), linkId, Id.createVehicleId(1), "bike", 0.0));

		assertThat(car.getLinkTravelTimes().getLinkTravelTime(link, 0.0, null, null ), is(2.0));
		assertThat(bike.getLinkTravelTimes().getLinkTravelTime(link, 0.0, null, null ), is(8.0));
	}

}
