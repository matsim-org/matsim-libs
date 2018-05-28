/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * WithinDayTransitEngine.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.withinday;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.HasModifiablePlan;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

import com.google.inject.Inject;
import com.google.inject.Singleton;


@Singleton
public class WithinDayTransitEngine implements MobsimEngine {

	// private static final Logger log = Logger.getLogger(WithinDayTransitEngine.class);

	private final LegRerouter rerouter;
	
	private InternalInterface internalInterface;
	
	@Inject
	public WithinDayTransitEngine(LegRerouter rerouter) {
		this.rerouter = rerouter;
	}
	
	@Override
	public void doSimStep(double time) {

	}
	
	public void doReplan() {
		QSim qsim = (QSim) internalInterface.getMobsim();
		qsim.getAgents().values().forEach(this::replanAgent);		
	}
	
	private void replanAgent(MobsimAgent agent) {
		if (agent instanceof HasModifiablePlan) {
			HasModifiablePlan hmp = (HasModifiablePlan) agent;
			
			Plan plan = hmp.getModifiablePlan();
			List<PlanElement> planElements = plan.getPlanElements();
			PlanElement current = WithinDayAgentUtils.getCurrentPlanElement(agent);
			
			List<PlanElement> remainingElements = dropUntil(planElements, current);
			List<Leg> remainingLegs = remainingElements.stream()
					                                   .filter(pe -> pe instanceof Leg)
					                                   .map(pe -> (Leg) pe)
					                                   .collect(Collectors.toList());
			
			if (!remainingLegs.isEmpty()) {
				for (Leg leg : remainingLegs) {
					// Apply the rerouting. The safeReroute method makes sure the start
					// and end link stay the same.
					rerouter.safeReroute(leg);
				}
			}
		}
	}
	
	private static <E> List<E> dropUntil(List<E> lst, E cmp) {
		List<E> result = new ArrayList<>();
		for (E el : lst) {
			if (!result.isEmpty() || el.equals(cmp)) {
				result.add(el);
			}
		}
		return result;
	}
	
	@Override
	public void onPrepareSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterSim() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

}
