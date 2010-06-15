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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;

import playground.mrieser.core.sim.api.NewSimEngine;
import playground.mrieser.core.sim.api.PlanElementHandler;
import playground.mrieser.core.sim.api.PlanSimulation;
import playground.mrieser.core.sim.features.SimFeature;

public class TimestepSimEngine implements NewSimEngine {

	private final PlanSimulation sim;
	private final EventsManager events;
	private double time;
	private final double timeStepSize;
	private final Map<Plan, Integer> planElementIndex = new HashMap<Plan, Integer>();

	public TimestepSimEngine(final PlanSimulation sim, final EventsManager events) {
		this(sim, events, 1.0);
	}

	public TimestepSimEngine(final PlanSimulation sim, final EventsManager events, final double timeStepSize) {
		this.sim = sim;
		this.events = events;
		this.timeStepSize = timeStepSize;
		this.time = 0;
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
	public void handleNextPlanElement(Plan plan) {
		Integer idx = this.planElementIndex.get(plan);
		int i = 0;
		int nOfPE = plan.getPlanElements().size();
		if (idx != null) {
			i = idx.intValue();
			if (i < nOfPE) {
				PlanElement pe = plan.getPlanElements().get(i);
				PlanElementHandler peh = sim.getPlanElementHandler(pe.getClass());
				if (peh == null) {
					throw new NullPointerException("No PlanElementHandler found for " + pe.getClass());
				}
				peh.handleEnd(pe, plan);
			}
			i++;
		}
		if (i <= nOfPE) {
			this.planElementIndex.put(plan, Integer.valueOf(i)); // first store current index to prevent endless loop
		}
		if (i < nOfPE) {
			PlanElement pe = plan.getPlanElements().get(i);
			PlanElementHandler peh = sim.getPlanElementHandler(pe.getClass());
			if (peh == null) {
				throw new NullPointerException("No PlanElementHandler found for " + pe.getClass());
			}
			peh.handleStart(pe, plan);
		}
	}

	@Override
	public void runSim() {
		this.time = 0.0;

		List<SimFeature> tmpList = this.sim.getSimFeatures();
		SimFeature[] simFeatures = tmpList.toArray(new SimFeature[tmpList.size()]);

		boolean running = true;
		while (running) {
			boolean isFinished = true;
			for (SimFeature feature : simFeatures) {
				feature.doSimStep(time);
				isFinished = isFinished && feature.isFinished();
			}
			running = !isFinished;
			if (running) {
				time += this.timeStepSize;
			}
		}
	}

}
