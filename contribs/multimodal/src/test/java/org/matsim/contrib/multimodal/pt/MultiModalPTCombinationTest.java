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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.testcases.MatsimTestUtils;

public class MultiModalPTCombinationTest {

	private static final Logger log = Logger.getLogger(MultiModalPTCombinationTest.class);
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Two things are tested here:
	 * - Multi-modal simulation can handle TransitAgents (previously, the TransitAgent class did not implement
	 *   the HasPerson interface. As a result, the multi-modal simulation crashed since it could not access
	 *   the person).
	 * - Multi-modal simulaition can handle transit_walk legs (not yet ready...).
	 */
	@Test
	public void testMultiModalPtCombination() {
		
		Fixture f = new Fixture();
		f.init();
		
		Person ptPerson = f.createPersonAndAdd(f.scenario, "0", TransportMode.transit_walk);
		Person walkPerson = f.createPersonAndAdd(f.scenario, "1", TransportMode.walk);
		
		Scenario scenario = f.scenario;
		Config config = scenario.getConfig();
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		MultiModalConfigGroup mmcg = new MultiModalConfigGroup();
		mmcg.setMultiModalSimulationEnabled(true);
		mmcg.setSimulatedModes(TransportMode.walk + "," + TransportMode.transit_walk);
		config.addModule(mmcg);
		
		config.qsim().setEndTime(24*3600);
		
		config.controler().setLastIteration(0);
		// doesn't matter - MultiModalModule sets the mobsim unconditionally. it just can't be something
		// which the ControlerDefaultsModule knows about. Try it, you will get an error. Quite safe.
		config.controler().setMobsim("myMobsim");


		ActivityParams homeParams = new ActivityParams("home");
		homeParams.setTypicalDuration(16*3600);
		config.planCalcScore().addActivityParams(homeParams);
		
		// set default walk speed; according to Weidmann 1.34 [m/s]
		double defaultWalkSpeed = 1.34;
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed);
//		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.transit_walk, defaultWalkSpeed);
		final PlansCalcRouteConfigGroup.ModeRoutingParams pt = new PlansCalcRouteConfigGroup.ModeRoutingParams( TransportMode.pt );
		pt.setTeleportedModeFreespeedFactor( 2.0 );
		config.plansCalcRoute().addParameterSet( pt );

//		config.plansCalcRoute().setNetworkModes(CollectionUtils.stringToSet(TransportMode.car + "," + TransportMode.walk +
//				"," + TransportMode.transit_walk));

        config.travelTimeCalculator().setFilterModes(true);

		Controler controler = new Controler(scenario);
        controler.getConfig().controler().setCreateGraphs(false);
		controler.getConfig().controler().setDumpDataAtEnd(false);
		controler.getConfig().controler().setWriteEventsInterval(0);
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
		Assert.assertEquals(7, ptPlan.getPlanElements().size());

		Plan walkPlan = walkPerson.getSelectedPlan();
		if ( config.plansCalcRoute().isInsertingAccessEgressWalk() ) {
			Assert.assertEquals(7, walkPlan.getPlanElements().size());
		} else {
			Assert.assertEquals(3, walkPlan.getPlanElements().size());
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
			Assert.assertEquals(true, link.getAllowedModes().contains(this.modes.get(driverId)));
			
			String mode = this.modes.get(driverId);
			int count = this.leftCountPerMode.get(mode);
			this.leftCountPerMode.put(mode, count + 1);
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			String mode = this.modes.remove(event.getPersonId());
			if ( mode.contains(TransportMode.access_walk) || mode.contains(TransportMode.egress_walk) ) {
				return ;
			}
			
			double tripTravelTime = event.getTime() - this.departures.remove(event.getPersonId());
			Double modeTravelTime = this.travelTimesPerMode.get(mode);
			if ( modeTravelTime==null ) {
				Logger.getLogger(this.getClass()).warn("mode:" + mode );
				Logger.getLogger(this.getClass()).warn("travelTimesPerMode:" + mode );
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
