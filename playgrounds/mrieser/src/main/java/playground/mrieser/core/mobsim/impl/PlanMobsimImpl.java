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

package playground.mrieser.core.mobsim.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.Simulation;

import playground.mrieser.core.mobsim.api.AgentSource;
import playground.mrieser.core.mobsim.api.NewMobsimEngine;
import playground.mrieser.core.mobsim.api.PlanAgent;
import playground.mrieser.core.mobsim.api.PlanElementHandler;
import playground.mrieser.core.mobsim.api.PlanMobsim;
import playground.mrieser.core.mobsim.features.MobsimFeature;
import playground.mrieser.core.mobsim.utils.ClassBasedMap;

/**
 * @author mrieser
 */
public class PlanMobsimImpl implements PlanMobsim, Simulation { // TODO [MR] remove Simulation, only for backwards compability

	private final static Logger log = Logger.getLogger(PlanMobsimImpl.class);

	private final Scenario scenario;
	private NewMobsimEngine simEngine = null;
	private final ClassBasedMap<PlanElement, PlanElementHandler> peHandlers = new ClassBasedMap<PlanElement, PlanElementHandler>();
	private final LinkedList<MobsimFeature> simFeatures = new LinkedList<MobsimFeature>();
	private final LinkedList<AgentSource> agentSources = new LinkedList<AgentSource>();

	public PlanMobsimImpl(final Scenario scenario) {
		this.scenario = scenario;
	}

	@Override
	public void setMobsimEngine(final NewMobsimEngine simEngine) {
		this.simEngine = simEngine;
	}

	@Override
	public PlanElementHandler setPlanElementHandler(final Class<? extends PlanElement> klass, final PlanElementHandler handler) {
		return this.peHandlers.put(klass, handler);
	}

	@Override
	public PlanElementHandler removePlanElementHandler(final Class<? extends PlanElement> klass) {
		return this.peHandlers.remove(klass);
	}

	@Override
	public PlanElementHandler getPlanElementHandler(final Class<? extends PlanElement> klass) {
		return this.peHandlers.get(klass);
	}

	@Override
	public void run() {
		runMobsim();
	}

	@Override
	public void runMobsim() {
		log.info("begin simulation.");

		// init
		if (this.simEngine == null) {
			throw new NullPointerException("No SimEngine set! see PlanSimulation.setSimEngine();");
		}

		// create agents etc.
		initAgents();

		// run
		this.simEngine.runSim();

		// finish
		// anything to do?

		log.info("simulation ends.");
	}

	private void initAgents() {
		for (AgentSource source : this.agentSources) {
			for (PlanAgent agent : source.getAgents()) {
				this.simEngine.handleAgent(agent);
			}
		}
	}

	@Override
	public void addAgentSource(final AgentSource agentSource) {
		this.agentSources.add(agentSource);
	}

	@Override
	public void addMobsimFeature(final MobsimFeature feature) {
		this.simFeatures.add(feature);
	}

	public void removeSimFeature(final MobsimFeature feature) {
		this.simFeatures.remove(feature);
	}

	@Override
	public List<MobsimFeature> getMobsimFeatures() {
		return Collections.unmodifiableList(this.simFeatures);
	}
}
