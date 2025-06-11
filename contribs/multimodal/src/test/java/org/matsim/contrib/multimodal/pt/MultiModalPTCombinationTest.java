/* *********************************************************************** *
 * project: org.matsim.*
 * MultiModalControlerListenerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.pt;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.multimodal.MultiModalModule;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.testcases.MatsimTestUtils;

public class MultiModalPTCombinationTest {

	private static final Logger log = LogManager.getLogger(MultiModalPTCombinationTest.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Two things are tested here:
	 * - Multi-modal simulation can handle TransitAgents (previously, the TransitAgent class did not implement
	 *   the HasPerson interface. As a result, the multi-modal simulation crashed since it could not access
	 *   the person).
	 * - Multi-modal simulation can handle transit_walk legs (not yet ready...).
	 * ---> in nov'19 we are trying to replace transit_walk by normal walk and routingMode=pt, so this test is
	 * probably no longer very useful, there is no more special walk mode for pt agents - gl-nov'19
	 */
	@Test
	void testMultiModalPtCombination() {

		Fixture f = new Fixture();
		f.init();

//		Person ptPerson = f.createPersonAndAdd(f.scenario, "0", TransportMode.transit_walk);
		Person ptPerson = f.createPersonAndAdd(f.scenario, "0", TransportMode.walk, TransportMode.pt);
		Person walkPerson = f.createPersonAndAdd(f.scenario, "1", TransportMode.walk, TransportMode.walk);

		Scenario scenario = f.scenario;
		Config config = scenario.getConfig();
		config.controller().setOutputDirectory(utils.getOutputDirectory());
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		MultiModalConfigGroup mmcg = new MultiModalConfigGroup();
		mmcg.setMultiModalSimulationEnabled(true);
		mmcg.setSimulatedModes(TransportMode.walk + "," + TransportMode.transit_walk);//TODO: is this still useful if no agent can still use transit_walk?
		config.addModule(mmcg);

		config.qsim().setEndTime(24*3600);

		config.controller().setLastIteration(0);
		// doesn't matter - MultiModalModule sets the mobsim unconditionally. it just can't be something
		// which the ControlerDefaultsModule knows about. Try it, you will get an error. Quite safe.
		config.controller().setMobsim("myMobsim");


		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*3600);
		config.scoring().addActivityParams(homeParams);

		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		config.routing().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);
		final RoutingConfigGroup.TeleportedModeParams pt = new RoutingConfigGroup.TeleportedModeParams( TransportMode.pt );
		pt.setTeleportedModeFreespeedFactor( 2.0 );
		config.routing().addParameterSet( pt );

        config.travelTimeCalculator().setFilterModes(true);

		Controler controler = new Controler(scenario);
        controler.getConfig().controller().setCreateGraphs(false);
		controler.getConfig().controller().setDumpDataAtEnd(false);
		controler.getConfig().controller().setWriteEventsInterval(0);
//		controler.setOverwriteFiles(true);

        controler.addOverridingModule(new MultiModalModule());

        LinkModeChecker linkModeChecker = new LinkModeChecker(scenario.getNetwork());
		controler.getEvents().addHandler(linkModeChecker);

		controler.run();

		/*
		 * Assume that the agent's plan was changed from "home-pt-home" to
		 * "home-transit_walk-pt_interact-pt-pt_interact-transit_walk-home"
		 */
		Plan ptPlan = ptPerson.getSelectedPlan();
		Assertions.assertEquals(7, ptPlan.getPlanElements().size(), ptPlan.getPlanElements().toString());

		Plan walkPlan = walkPerson.getSelectedPlan();
		if ( !config.routing().getAccessEgressType().equals(RoutingConfigGroup.AccessEgressType.none) ) {
			Assertions.assertEquals(7, walkPlan.getPlanElements().size(), walkPlan.getPlanElements().toString());
		} else {
			Assertions.assertEquals(3, walkPlan.getPlanElements().size(), walkPlan.getPlanElements().toString());
		}

		/*
		 * These tests fail since the TransitRouter (?) does not create NetworkRoutes.
		 * As a result, the multi-modal simulation removes the pt agent from the simulation.
		 */
		// assume that the transit_walk legs have network routes
//		Assert.assertEquals(true, ((Leg) plan.getPlanElements().get(1)).getRoute() instanceof NetworkRoute);
//		Assert.assertEquals(true, ((Leg) plan.getPlanElements().get(5)).getRoute() instanceof NetworkRoute);

		// assume that the number of arrival events is correct
//		Assert.assertEquals(4, linkModeChecker.arrivalCount);

		// assume that the number of link left events is correct
//		Assert.assertEquals(8, linkModeChecker.linkLeftCount);
	}



	private static class LinkModeChecker implements BasicEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler,
			PersonArrivalEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

		private final Network network;
		private final Map<Id<Person>, String> modes = new HashMap<>();
		private final Map<Id<Person>, Double> departures = new HashMap<>();
		final Map<String, Integer> leftCountPerMode = new HashMap<>();
		final Map<String, Double> travelTimesPerMode = new HashMap<>();

		private Vehicle2DriverEventHandler delegate = new Vehicle2DriverEventHandler();

		public LinkModeChecker(Network network) {
			this.network = network;

			leftCountPerMode.put(TransportMode.pt, 0);
			leftCountPerMode.put(TransportMode.car, 0);
			leftCountPerMode.put(TransportMode.walk, 0);
			leftCountPerMode.put(TransportMode.transit_walk, 0);

			travelTimesPerMode.put(TransportMode.pt, 0.0);
			travelTimesPerMode.put(TransportMode.car, 0.0);
			travelTimesPerMode.put(TransportMode.walk, 0.0);
			travelTimesPerMode.put(TransportMode.transit_walk, 0.0);
		}

		@Override
		public void reset(int iteration) {
			delegate.reset(iteration);
			// nothing else to do here
		}

		@Override
		public void handleEvent(PersonDepartureEvent event) {
			this.modes.put(event.getPersonId(), event.getLegMode());
			this.departures.put(event.getPersonId(), event.getTime());
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			Link link = this.network.getLinks().get(event.getLinkId());
			Id<Person> driverId = delegate.getDriverOfVehicle(event.getVehicleId());

			if (!link.getAllowedModes().contains(this.modes.get(driverId))) {
				log.error("Found mode " + this.modes.get(driverId) + " on link " + link.getId());
			}

			// assume that the agent is allowed to travel on the link
			Assertions.assertEquals(true, link.getAllowedModes().contains(this.modes.get(driverId)));

			String mode = this.modes.get(driverId);
			int count = this.leftCountPerMode.get(mode);
			this.leftCountPerMode.put(mode, count + 1);
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			String mode = this.modes.remove(event.getPersonId());
			if ( mode.contains(TransportMode.non_network_walk ) || mode.contains(TransportMode.non_network_walk ) ) {
				return ;
			}

			double tripTravelTime = event.getTime() - this.departures.remove(event.getPersonId());
			Double modeTravelTime = this.travelTimesPerMode.get(mode);
			if ( modeTravelTime==null ) {
				LogManager.getLogger(this.getClass()).warn("mode:" + mode );
				LogManager.getLogger(this.getClass()).warn("travelTimesPerMode:" + mode );
			}
			this.travelTimesPerMode.put(mode, modeTravelTime + tripTravelTime);
		}

		@Override
		public void handleEvent(Event event) {
			log.info(event.toString());
		}

		@Override
		public void handleEvent(VehicleLeavesTrafficEvent event) {
			delegate.handleEvent(event);
		}

		@Override
		public void handleEvent(VehicleEntersTrafficEvent event) {
			delegate.handleEvent(event);
		}
	}
}
