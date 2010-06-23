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

package playground.mrieser.core.sim.usecases;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;

import playground.mrieser.core.sim.features.DefaultNetworkFeature;
import playground.mrieser.core.sim.features.NetworkFeature;
import playground.mrieser.core.sim.features.StatusFeature;
import playground.mrieser.core.sim.impl.ActivityHandler;
import playground.mrieser.core.sim.impl.CarDepartureHandler;
import playground.mrieser.core.sim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.sim.impl.LegHandler;
import playground.mrieser.core.sim.impl.PlanSimulationImpl;
import playground.mrieser.core.sim.impl.TeleportationHandler;
import playground.mrieser.core.sim.network.api.SimNetwork;
import playground.mrieser.core.sim.network.queueNetwork.QueueNetworkCreator;

public class RefSimFactory implements MobsimFactory {

	@Override
	public Simulation createMobsim(final Scenario scenario, final EventsManager eventsManager) {

		PlanSimulationImpl planSim = new PlanSimulationImpl(scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, eventsManager);
		planSim.setSimEngine(engine);

		// setup network
		SimNetwork simNetwork = QueueNetworkCreator.createQueueNetwork(scenario.getNetwork(), engine, MatsimRandom.getRandom());
		NetworkFeature netFeature = new DefaultNetworkFeature(simNetwork);

		// setup features; order is important!
		planSim.addSimFeature(new StatusFeature());

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);
		planSim.addSimFeature(ah); // how should a user know ah is a simfeature, bug lh not?

		planSim.addSimFeature(netFeature); // order of features is important!

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(engine, netFeature, scenario));
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		planSim.addSimFeature(teleporter); // how should a user know teleporter is a simfeature?
		lh.setDepartureHandler(TransportMode.pt, teleporter);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);

		return planSim;
	}

}
