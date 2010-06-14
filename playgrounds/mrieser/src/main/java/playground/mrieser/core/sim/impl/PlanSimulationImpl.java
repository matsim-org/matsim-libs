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
import java.util.Map;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;

import playground.mrieser.core.sim.api.PlanElementHandler;
import playground.mrieser.core.sim.api.PlanSimulation;
import playground.mrieser.core.sim.utils.ClassBasedMap;

/**
 * @author mrieser
 */
public class PlanSimulationImpl implements PlanSimulation {

	private final ClassBasedMap<PlanElement, PlanElementHandler> peHandlers = new ClassBasedMap<PlanElement, PlanElementHandler>();
	private final Map<Plan, Integer> planElementIndex = new HashMap<Plan, Integer>();

	@Override
	public PlanElementHandler setPlanElementHandler(final Class<? extends PlanElement> klass, final PlanElementHandler handler) {
		return this.peHandlers.put(klass, handler);
	}

	@Override
	public PlanElementHandler removePlanElementHandler(final Class<? extends PlanElement> klass) {
		return this.peHandlers.remove(klass);
	}

	/*package*/ PlanElementHandler getPlanElementHandler(final Class<? extends PlanElement> klass) {
		return this.peHandlers.get(klass);
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
				PlanElementHandler peh = getPlanElementHandler(pe.getClass());
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
			PlanElementHandler peh = getPlanElementHandler(pe.getClass());
			if (peh == null) {
				throw new NullPointerException("No PlanElementHandler found for " + pe.getClass());
			}
			peh.handleStart(pe, plan);
		}
	}
}
