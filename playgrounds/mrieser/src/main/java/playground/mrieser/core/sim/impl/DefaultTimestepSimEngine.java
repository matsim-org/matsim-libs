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

package playground.mrieser.core.sim.impl;

import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.mrieser.core.sim.api.PlanAgent;
import playground.mrieser.core.sim.api.PlanElementHandler;
import playground.mrieser.core.sim.api.PlanSimulation;
import playground.mrieser.core.sim.api.SimKeepAlive;
import playground.mrieser.core.sim.api.TimestepSimEngine;
import playground.mrieser.core.sim.features.SimFeature;

public class DefaultTimestepSimEngine implements TimestepSimEngine {

	private final static Logger log = Logger.getLogger(DefaultTimestepSimEngine.class);

	private final PlanSimulation sim;
	private final EventsManager events;
	private double time;
	private final double timeStepSize;
	private final List<SimKeepAlive> aliveKeepers = new LinkedList<SimKeepAlive>();

	public DefaultTimestepSimEngine(final PlanSimulation sim, final EventsManager events) {
		this(sim, events, 1.0);
	}

	public DefaultTimestepSimEngine(final PlanSimulation sim, final EventsManager events, final double timeStepSize) {
		this.sim = sim;
		this.events = events;
		this.timeStepSize = timeStepSize;
		this.time = 0;
	}

	@Override
	public double getTimestepSize() {
		return this.timeStepSize;
	}

	@Override
	public double getCurrentTime() {
		return this.time;
	}

	@Override
	public EventsManager getEventsManager() {
		return this.events;
	}

	@Override
	public void handleAgent(final PlanAgent agent) {
		PlanElement currentPE = agent.getCurrentPlanElement();
		if (currentPE != null) {
			// == null could be the case when the agent starts with its first activity
			PlanElementHandler peh = this.sim.getPlanElementHandler(currentPE.getClass());
			if (peh == null) {
				throw new NullPointerException("No PlanElementHandler found for " + currentPE.getClass());
			}
			peh.handleEnd(agent);
		}

		PlanElement nextPE = agent.useNextPlanElement();
		if (nextPE != null) {
			// == null could be the case when the agent was at its last activity
			PlanElementHandler peh = this.sim.getPlanElementHandler(nextPE.getClass());
			if (peh == null) {
				throw new NullPointerException("No PlanElementHandler found for " + nextPE.getClass());
			}
			peh.handleStart(agent);
		}
	}

	@Override
	public void runSim() {

		this.time = 8.0 * 3600.0;  // TODO [MR] remove sim-start-time hack

		List<SimFeature> tmpList = this.sim.getSimFeatures();
		SimFeature[] simFeatures = tmpList.toArray(new SimFeature[tmpList.size()]);

		log.info("registered features:");
		for (SimFeature feature : simFeatures) {
			log.info("  " + feature.getClass());
		}

		boolean running = true;
		while (running) {
			for (SimFeature feature : simFeatures) {
				feature.doSimStep(this.time);
			}
			running = keepAlive();
			if (this.time >= 55.0 * 3600) {  // TODO [MR] remove sim-end-time hack
				running = false;
			}
			if (running) {
				this.time += this.timeStepSize;
			}
		}
	}

	@Override
	public void addKeepAlive(final SimKeepAlive keepAlive) {
		this.aliveKeepers.add(keepAlive);
	}

	private boolean keepAlive() {
		for (SimKeepAlive ska : this.aliveKeepers) {
			if (ska.keepAlive()) {
				return true;
			}
		}
		return false;
	}

}
