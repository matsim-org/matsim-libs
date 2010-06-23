/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.core.sim.integration;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.testcases.utils.EventsLogger;

import playground.mrieser.core.sim.features.DefaultNetworkFeature;
import playground.mrieser.core.sim.features.NetworkFeature;
import playground.mrieser.core.sim.features.StatusFeature;
import playground.mrieser.core.sim.impl.ActivityHandler;
import playground.mrieser.core.sim.impl.CarDepartureHandler;
import playground.mrieser.core.sim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.sim.impl.LegHandler;
import playground.mrieser.core.sim.impl.PlanSimulationImpl;
import playground.mrieser.core.sim.impl.TeleportationHandler;
import playground.mrieser.core.sim.network.api.SimLink;
import playground.mrieser.core.sim.network.api.SimNetwork;
import playground.mrieser.core.sim.network.queueNetwork.QueueNetworkCreator;

/**
 * @author mrieser
 */
public class IntegrationTest {

	@Test
	public void test_ArrivingCarIsParked() {
		Fixture f = new Fixture();
		Person person1 = f.addPersonWithOneLeg();

		EventsManager events = new EventsManagerImpl();

		/* setup start */
		PlanSimulationImpl planSim = new PlanSimulationImpl(f.scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, events);
		planSim.setSimEngine(engine);

		// setup network
		SimNetwork simNetwork = QueueNetworkCreator.createQueueNetwork(f.scenario.getNetwork(), engine, MatsimRandom.getRandom());
		NetworkFeature netFeature = new DefaultNetworkFeature(simNetwork);

		// setup features; order is important!
		planSim.addSimFeature(new StatusFeature());
		planSim.addSimFeature(netFeature);

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);
		planSim.addSimFeature(ah); // how should a user now ah is a simfeature, bug lh not?

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, f.scenario));
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		planSim.addSimFeature(teleporter); // how should a user now teleporter is a simfeature?
		lh.setDepartureHandler(TransportMode.pt, teleporter);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);
		/* setup end */ // TODO too much boilerplate code

		planSim.runSim();

		SimLink simLink = simNetwork.getLinks().get(((Leg) person1.getSelectedPlan().getPlanElements().get(1)).getRoute().getEndLinkId());
		Assert.assertNotNull("car should be parked, but cannot be found on link.", simLink.getParkedVehicle(person1.getId()));
	}

	@Test
	public void test_ArrivingCarDepartsAgain() {
		Fixture f = new Fixture();
		Person person2 = f.addPersonWithTwoLegs();

		EventsManager events = new EventsManagerImpl();

		/* setup start */
		PlanSimulationImpl planSim = new PlanSimulationImpl(f.scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, events);
		planSim.setSimEngine(engine);

		// setup network
		SimNetwork simNetwork = QueueNetworkCreator.createQueueNetwork(f.scenario.getNetwork(), engine, MatsimRandom.getRandom());
		NetworkFeature netFeature = new DefaultNetworkFeature(simNetwork);

		// setup features; order is important!
		planSim.addSimFeature(new StatusFeature());
		planSim.addSimFeature(netFeature);

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);
		planSim.addSimFeature(ah); // how should a user know ah is a simfeature, bug lh not?

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, f.scenario));
		/* setup end */ // TODO too much boilerplate code

		planSim.runSim();

		SimLink simLink = simNetwork.getLinks().get(((Leg) person2.getSelectedPlan().getPlanElements().get(3)).getRoute().getEndLinkId());
		Assert.assertNotNull("car should be parked, but cannot be found on link.", simLink.getParkedVehicle(person2.getId()));
	}

	@Test
	public void test_TwoActsOnSameLink() {
		Fixture f = new Fixture();
		Person person3 = f.addPersonWithTwoActsOnSameLink();

		EventsManager events = new EventsManagerImpl();
		events.addHandler(new EventsLogger());

		/* setup start */
		PlanSimulationImpl planSim = new PlanSimulationImpl(f.scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, events);
		planSim.setSimEngine(engine);

		// setup network
		SimNetwork simNetwork = QueueNetworkCreator.createQueueNetwork(f.scenario.getNetwork(), engine, MatsimRandom.getRandom());
		NetworkFeature netFeature = new DefaultNetworkFeature(simNetwork);

		// setup features; order is important!
		planSim.addSimFeature(new StatusFeature());
		planSim.addSimFeature(netFeature);

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);
		planSim.addSimFeature(ah); // how should a user know ah is a simfeature, bug lh not?

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, f.scenario));
		/* setup end */ // TODO too much boilerplate code

		planSim.runSim();

		SimLink simLink = simNetwork.getLinks().get(((Leg) person3.getSelectedPlan().getPlanElements().get(5)).getRoute().getEndLinkId());
		Assert.assertNotNull("car should be parked, but cannot be found on link.", simLink.getParkedVehicle(person3.getId()));
	}
}
