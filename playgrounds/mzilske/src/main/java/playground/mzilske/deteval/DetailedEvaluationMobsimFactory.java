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

package playground.mzilske.deteval;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimFactory;
import org.matsim.core.mobsim.framework.Simulation;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.vis.otfvis.OTFClientLive;
import org.matsim.vis.otfvis.OnTheFlyServer;
import playground.mrieser.core.mobsim.features.OTFVisFeature;
import playground.mrieser.core.mobsim.features.StatusFeature;
import playground.mrieser.core.mobsim.features.fastQueueNetworkFeature.FastQueueNetworkFeature;
import playground.mrieser.core.mobsim.impl.ActivityHandler;
import playground.mrieser.core.mobsim.impl.DefaultTimestepSimEngine;
import playground.mrieser.core.mobsim.impl.LegHandler;
import playground.mrieser.core.mobsim.impl.PlanMobsimImpl;
import playground.mrieser.core.mobsim.impl.PopulationAgentSource;
import playground.mrieser.core.mobsim.impl.TeleportationHandler;
import playground.mrieser.core.mobsim.network.api.VisNetwork;

// This is rather a Builder than a factory... but the interface is named Factory, so well....
public class DetailedEvaluationMobsimFactory implements MobsimFactory {

	private final int nOfThreads;
	private boolean useOTFVis = true;
	private String[] teleportedModes = null;

	private List<Population> backgroundPopulations = new ArrayList<Population>();
	
	/**
	 * @param nOfThreads use <code>0</code> if you do not want to use threads
	 */
	public DetailedEvaluationMobsimFactory(final int nOfThreads) {
		this.nOfThreads = nOfThreads;
	}

	public void setTeleportedModes(final String[] teleportedModes) {
		this.teleportedModes = teleportedModes.clone();
	}

	@Override
	public Simulation createMobsim(final Scenario scenario, final EventsManager eventsManager) {

		PlanMobsimImpl planSim = new PlanMobsimImpl(scenario);
		DefaultTimestepSimEngine engine = new DefaultTimestepSimEngine(planSim, eventsManager);
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
		StrictCarDepartureHandler carHandler = new StrictCarDepartureHandler(engine, netFeature, scenario);
		lh.setDepartureHandler(TransportMode.car, carHandler);
		TeleportationHandler teleporter = new TeleportationHandler(engine);
		if (this.teleportedModes != null) {
			for (String mode : this.teleportedModes) {
				lh.setDepartureHandler(mode, teleporter);
			}
		}

		// register all features at the end in the right order
		planSim.addMobsimFeature(new StatusFeature());
		planSim.addMobsimFeature(teleporter); // how should a user know teleporter is a simfeature?
		planSim.addMobsimFeature(ah); // how should a user know ah is a simfeature, bug lh not?
		planSim.addMobsimFeature(netFeature); // order of features is important!

		if (useOTFVis) {
			OnTheFlyServer server = OnTheFlyServer.createInstance(scenario, eventsManager);
			OTFClientLive.run(scenario.getConfig(), server);
			VisNetwork visNetwork = netFeature.getVisNetwork();
			OTFVisFeature otfvisFeature = new OTFVisFeature(visNetwork, server.getSnapshotReceiver());
			planSim.addMobsimFeature(otfvisFeature);
		}

		
		
		// register agent sources
		planSim.addAgentSource(new PopulationAgentSource(scenario.getPopulation(), 1.0));
		CarDistributor carDistributor = new CarDistributor(scenario.getPopulation(), ((ScenarioImpl) scenario).getVehicles(), netFeature, engine);
		planSim.addMobsimFeature(carDistributor);
		for (Population backgroundPopulation : this.backgroundPopulations) {
			planSim.addAgentSource(new PopulationAgentSource(backgroundPopulation, 1.0));
			CarDistributor backgroundCarDistributor = new CarDistributor(backgroundPopulation, ((ScenarioImpl) scenario).getVehicles(), netFeature, engine);
			backgroundCarDistributor.setPunishVehicleMove(false);
			planSim.addMobsimFeature(backgroundCarDistributor);
		}
				
		return planSim;
	}
	
	/**
	 * Background populations are not replanned. They are not part of the Scenario container, and they are only used by the Mobsim.
	 * 
	 * @param population
	 */
	public void addBackgroundPopulation(Population population) {
		this.backgroundPopulations.add(population);
	}

	public void setUseOTFVis(boolean useOTFVis) {
		this.useOTFVis = useOTFVis;
	}

}
