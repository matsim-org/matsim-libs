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

import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.ScenarioLoader;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.mrieser.core.sim.api.PlanSimulation;
import playground.mrieser.core.sim.features.DefaultNetworkFeature;
import playground.mrieser.core.sim.features.NetworkFeature;
import playground.mrieser.core.sim.features.OTFVisFeature;
import playground.mrieser.core.sim.features.SignalSystemsFeature;
import playground.mrieser.core.sim.features.StatusFeature;
import playground.mrieser.core.sim.features.TransitFeature;
import playground.mrieser.core.sim.impl.ActivityHandler;
import playground.mrieser.core.sim.impl.CarDepartureHandler;
import playground.mrieser.core.sim.impl.LegHandler;
import playground.mrieser.core.sim.impl.PlanSimulationImpl;
import playground.mrieser.core.sim.impl.TeleportationHandler;
import playground.mrieser.core.sim.impl.TimestepSimEngine;
import playground.mrieser.core.sim.impl.TransitDepartureHandler;
import playground.mrieser.core.sim.network.api.SimNetwork;
import playground.mrieser.core.sim.network.queueNetwork.QueueNetworkCreator;

/**
 * @author mrieser
 */
public class UseCase1 {

	public static void main(String[] args) {

		String prefix = "../../MATSim/";

		// load data
		Config config;
		try {
			config = ConfigUtils.loadConfig(prefix + "test/scenarios/equil/config.xml");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		ConfigUtils.modifyFilePaths(config, prefix);
		ScenarioLoader loader = new ScenarioLoaderImpl(config);
		Scenario scenario = loader.loadScenario();

		// setup Sim and Engine
		EventsManager events = new EventsManagerImpl();
		PlanSimulation planSim = new PlanSimulationImpl(scenario, events);
		TimestepSimEngine engine = new TimestepSimEngine(planSim, events);
		planSim.setSimEngine(engine);

		// setup network
		SimNetwork simNetwork = QueueNetworkCreator.createQueueNetwork(scenario.getNetwork());
		NetworkFeature netFeature = new DefaultNetworkFeature(simNetwork);

		// setup features; order is important!
		planSim.addSimFeature(new StatusFeature());
		planSim.addSimFeature(new SignalSystemsFeature());
		planSim.addSimFeature(new TransitFeature());
		planSim.addSimFeature(netFeature);
		planSim.addSimFeature(new OTFVisFeature());

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);

		// setup DepartureHandlers
		lh.setDepartureHandler(TransportMode.car, new CarDepartureHandler(netFeature));
		lh.setDepartureHandler(TransportMode.pt, new TransitDepartureHandler());
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		lh.setDepartureHandler(TransportMode.walk, teleporter);
		lh.setDepartureHandler(TransportMode.bike, teleporter);

		// run
		planSim.runSim();
	}
}
