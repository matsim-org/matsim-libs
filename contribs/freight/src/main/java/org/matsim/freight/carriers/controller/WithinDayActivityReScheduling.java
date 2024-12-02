/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.controller;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimListener;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.freight.carriers.Tour.Start;
import org.matsim.freight.carriers.Tour.TourActivity;

/*
 * Physically enforces beginnings of time windows for freight activities, i.e. freight agents
 * wait before closed doors until they can deliver / pick up their goods.
 *
 * Required because there is no way to encode this in a MATSim plan. There is no actual
 * within-day replanning taking place. What we would need is an ActivityDurationInterpretation
 * which allows this. Then this could go away.
 *
 */
class WithinDayActivityReScheduling implements MobsimListener, MobsimBeforeSimStepListener {
	public static final String COMPONENT_NAME=WithinDayActivityReScheduling.class.getSimpleName() ;


	@SuppressWarnings("unused")
	private static final  Logger logger = LogManager.getLogger(WithinDayActivityReScheduling.class);

	private final FreightAgentSource freightAgentSource;

	private final Set<Activity> encounteredActivities = new HashSet<>();

	private final CarrierAgentTracker carrierAgentTracker;

	@Inject
	WithinDayActivityReScheduling(FreightAgentSource freightAgentSource, CarrierAgentTracker carrierAgentTracker) {
		this.freightAgentSource = freightAgentSource;
		this.carrierAgentTracker = carrierAgentTracker;
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {
		Collection<MobsimAgent> agentsToReplan = freightAgentSource.getMobSimAgents();
		for (MobsimAgent pa : agentsToReplan) {
			doReplanning(pa, e.getSimulationTime(), e.getQueueSimulation());
		}
	}

	private void doReplanning( MobsimAgent mobsimAgent, double time, Mobsim mobsim ) {
		PlanAgent planAgent = (PlanAgent) mobsimAgent;
		Id<Person> agentId = planAgent.getCurrentPlan().getPerson().getId();
		PlanElement currentPlanElement = WithinDayAgentUtils.getCurrentPlanElement(mobsimAgent);
		if (currentPlanElement instanceof Activity act) {
			if (encounteredActivities.contains(act)) {
				return;
			}
			CarrierDriverAgent driver = carrierAgentTracker.getDriver(agentId);
			TourActivity plannedActivity = (TourActivity) driver.getPlannedTourElement(WithinDayAgentUtils.getCurrentPlanElementIndex(mobsimAgent));
			if (plannedActivity instanceof Start){
				encounteredActivities.add(act);
			} else {
				double newEndTime = Math.max(time, plannedActivity.getTimeWindow().getStart()) + plannedActivity.getDuration();
				act.setMaximumDurationUndefined();
				act.setEndTime(newEndTime);
				WithinDayAgentUtils.resetCaches( mobsimAgent );
				WithinDayAgentUtils.rescheduleActivityEnd(mobsimAgent,mobsim);
				encounteredActivities.add(act);
			}
		}
	}
}
