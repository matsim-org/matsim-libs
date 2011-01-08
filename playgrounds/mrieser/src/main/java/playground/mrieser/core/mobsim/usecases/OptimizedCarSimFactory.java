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

package playground.mrieser.core.mobsim.usecases;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.vis.otfvis2.OTFVisLiveServer;

import playground.mrieser.core.mobsim.features.OTFVisFeature;
import playground.mrieser.core.mobsim.features.StatusFeature;
import playground.mrieser.core.mobsim.features.fastQueueNetworkFeature.FastQueueNetworkFeature;
import playground.mrieser.core.mobsim.impl.ActivityHandler;
import playground.mrieser.core.mobsim.impl.CarDepartureHandler;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.LegHandler;
import playground.mrieser.core.mobsim.impl.PlanSimulationImpl;
import playground.mrieser.core.mobsim.impl.PopulationAgentSource;
import playground.mrieser.core.mobsim.impl.TeleportationHandler;
import playground.mrieser.core.mobsim.network.api.VisNetwork;

// This is rather a Builder than a factory... but the interface is named Factory, so well....
public class OptimizedCarSimFactory implements MobsimFactory {

	private final int nOfThreads;
	private OTFVisLiveServer otfvisServer = null;
	private String[] teleportedModes = null;
	private double populationWeight = 1.0;
	private double mobsimStopTime = Double.POSITIVE_INFINITY;

	/**
	 * @param nOfThreads use <code>0</code> if you do not want to use threads
	 */
	public OptimizedCarSimFactory(final int nOfThreads) {
		this.nOfThreads = nOfThreads;
	}

	public void setOtfvisServer(final OTFVisLiveServer otfvisServer) {
		this.otfvisServer = otfvisServer;
	}

	public void setTeleportedModes(final String[] teleportedModes) {
		this.teleportedModes = teleportedModes.clone();
	}

	/**
	 * Sets the weight for agents created from the population.
	 * If your population is a 20%-sample, the weight is typically 5
	 * (each agent should count for 5 persons).
	 *
	 * @param populationWeight
	 */
	public void setPopulationWeight(double populationWeight) {
		this.populationWeight = populationWeight;
	}

	/**
	 * Sets a time at which the mobsim will stop its execution, no matter
	 * if all agents have completed their plans or not. Initially set to
	 * {@link Double#POSITIVE_INFINITY}.
	 *
	 * @param stopTime
	 */
	public void setMobsimStopTime(final double stopTime) {
		this.mobsimStopTime = stopTime;
	}

	@Override
	public Simulation createMobsim(final Scenario scenario, final EventsManager eventsManager) {

		PlanSimulationImpl planSim = new PlanSimulationImpl(scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, eventsManager);
		engine.setStopTime(this.mobsimStopTime);
		planSim.setMobsimEngine(engine);

		// setup network
		FastQueueNetworkFeature netFeature;
		if (this.nOfThreads == 0) {
			netFeature = new FastQueueNetworkFeature(scenario.getNetwork(), engine);
		} else {
			netFeature = new FastQueueNetworkFeature(scenario.getNetwork(), engine, this.nOfThreads);
		}

		// setup PlanElementHandlers
		ActivityHandler ah = new ActivityHandler(engine);
		LegHandler lh = new LegHandler(engine);
		planSim.setPlanElementHandler(Activity.class, ah);
		planSim.setPlanElementHandler(Leg.class, lh);

		// setup DepartureHandlers
		CarDepartureHandler carHandler = new CarDepartureHandler(engine, netFeature, scenario);
		carHandler.setTeleportVehicles(true);
		lh.setDepartureHandler(TransportMode.car, carHandler);
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		if (this.teleportedModes != null) {
			for (String mode : this.teleportedModes) {
				lh.setDepartureHandler(mode, teleporter);
			}
		}

		// register all features at the end in the right order
		planSim.addMobsimFeature(new StatusFeature());
		planSim.addMobsimFeature(teleporter);
		planSim.addMobsimFeature(ah);
		planSim.addMobsimFeature(netFeature);

		if (this.otfvisServer != null) {
			VisNetwork visNetwork = netFeature.getVisNetwork();
			OTFVisFeature otfvisFeature = new OTFVisFeature(visNetwork, this.otfvisServer.getSnapshotReceiver());
			planSim.addMobsimFeature(otfvisFeature);
		}

		// register agent sources
		planSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(), this.populationWeight));

		return planSim;
	}

}
