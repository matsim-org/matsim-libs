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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.Simulation;

import playground.mrieser.core.sim.api.NewSimEngine;
import playground.mrieser.core.sim.api.PlanElementHandler;
import playground.mrieser.core.sim.api.PlanSimulation;
import playground.mrieser.core.sim.features.SimFeature;
import playground.mrieser.core.sim.utils.ClassBasedMap;

/**
 * @author mrieser
 */
public class PlanSimulationImpl implements PlanSimulation, Simulation { // TODO [MR] remove Simulation, only for backwards compability

	private final static Logger log = Logger.getLogger(PlanSimulationImpl.class);

	private final Scenario scenario;
	private NewSimEngine simEngine = null;
	private final ClassBasedMap<PlanElement, PlanElementHandler> peHandlers = new ClassBasedMap<PlanElement, PlanElementHandler>();
	private final LinkedList<SimFeature> simFeatures = new LinkedList<SimFeature>();

	public PlanSimulationImpl(final Scenario scenario, final EventsManager events) {
		this.scenario = scenario;
	}

	@Override
	public void setSimEngine(final NewSimEngine simEngine) {
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
		runSim();
	}

	@Override
	public void runSim() {
		log.info("begin simulation.");

		// TODO
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
		for (Person person : this.scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			this.simEngine.handleAgent(new DefaultPlanAgent(plan));
		}
	}

	@Override
	public void addSimFeature(final SimFeature feature) {
		this.simFeatures.add(feature);
	}

	public void removeSimFeature(final SimFeature feature) {
		this.simFeatures.remove(feature);
	}

	@Override
	public List<SimFeature> getSimFeatures() {
		return Collections.unmodifiableList(this.simFeatures);
	}
}
