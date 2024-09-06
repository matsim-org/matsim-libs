/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.qsim;

import java.util.Comparator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.api.core.v01.events.HasPersonId;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;

public final class ActivityEngineWithWakeup implements ActivityEngine {
	public static final String COMPONENT_NAME = "ActivityEngineWithWakeup";
	private static final Logger log = LogManager.getLogger(ActivityEngineWithWakeup.class );
	private final EventsManager eventsManager;
	private final PreplanningEngine preplanningEngine;
	private final ActivityEngine delegate;

	private final Queue<AgentEntry> wakeUpList = new PriorityBlockingQueue<>(500,
			Comparator.comparingDouble((AgentEntry o) -> o.time).thenComparing(o -> o.agent.getId()));
	private InternalInterface internalInterface;

	@Inject
	ActivityEngineWithWakeup(EventsManager eventsManager, PreplanningEngine preplanningEngine) {
		this.delegate = new ActivityEngineDefaultImpl(eventsManager);
		this.eventsManager = eventsManager;
		this.preplanningEngine = preplanningEngine;
	}

	@Override
	public void onPrepareSim() {
		log.warn( "running onPrepareSim");
		delegate.onPrepareSim();
	}

	@Override
	public void doSimStep(double now) {
		while (!wakeUpList.isEmpty() && wakeUpList.peek().time <= now) {
			final AgentEntry entry = wakeUpList.poll();
			this.eventsManager.processEvent(new AgentWakeupEvent(now, entry.agent.getId()));
			entry.agentWakeup.executeOnWakeup(entry.agent, now );
		}
		delegate.doSimStep(now);
	}

	@Override
	public void afterSim() {
		delegate.afterSim();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
		delegate.setInternalInterface(internalInterface);
	}

	/**
	 * This method is called by QSim to pass in agents which then "live" in the activity layer until they are handed out again
	 * through the internalInterface.
	 * <p>
	 * It is called not before onPrepareSim() and not after afterSim(), but it may be called before, after, or from doSimStep(),
	 * and even from itself (i.e. it must be reentrant), since internalInterface.arrangeNextAgentState() may trigger
	 * the next Activity.
	 */
	@Override
	public boolean handleActivity(MobsimAgent agent) {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();

//		Activity act = (Activity)WithinDayAgentUtils.getCurrentPlanElement(agent);
		if ( agent instanceof PlanAgent ) {
			Activity act = (Activity) ((PlanAgent) agent).getCurrentPlanElement();
			if (!act.getType().contains("interaction")) {
				wakeUpList.addAll(preplanningEngine.generateWakeups(agent, now));
			}
		}

		return delegate.handleActivity(agent);
	}

	public interface AgentWakeup {
		void executeOnWakeup( MobsimAgent agent, double now );
	}

	/**
	 * For within-day replanning. Tells this engine that the activityEndTime the agent reports may have changed since
	 * the agent was added to this engine through handleActivity.
	 * May be merged with handleActivity, since this engine can know by itself if it was called the first time
	 * or not.
	 *
	 * @param agent The agent.
	 */
	@Override
	public void rescheduleActivityEnd(final MobsimAgent agent) {
		delegate.rescheduleActivityEnd(agent);
	}

	/**
	 * Agents cannot be added directly to the activityEndsList since that would
	 * not be thread-safe when within-day replanning is used. There, an agent's
	 * activity end time can be modified. As a result, the agent is located at
	 * the wrong position in the activityEndsList until it is updated by using
	 * rescheduleActivityEnd(...). However, if another agent is added to the list
	 * in the mean time, it might be inserted at the wrong position.
	 * cdobler, apr'12
	 */
	static class AgentEntry {
		AgentEntry( MobsimAgent agent, double time, AgentWakeup agentWakeup ) {
			// yyyy Let us be careful that the executeOnWakeUp does not become overkill here; if we want something more
			// general, rather move on a completely general MessageQueue.  kai, mar'19

			this.agent = agent;
			this.time = time;
			this.agentWakeup = agentWakeup;
		}

		final MobsimAgent agent;
		final double time;
		final AgentWakeup agentWakeup;
	}

	public final static class AgentWakeupEvent extends Event implements HasPersonId {
		private final Id<Person> personId;

		AgentWakeupEvent( double now, Id<Person> personId ) {
			super(now);
			this.personId = personId;
		}

		@Override
		public String getEventType() {
			return "agentWakeup";
		}

		@Override
		public Id<Person> getPersonId() {
			return personId;
		}

		@Override
		public Map<String, String> getAttributes() {
			Map<String, String> attr = super.getAttributes();
//			attr.put(ATTRIBUTE_PERSON, this.personId.toString()); // already done in superclass.  kai, apr'23
			return attr;
		}
	}
}
