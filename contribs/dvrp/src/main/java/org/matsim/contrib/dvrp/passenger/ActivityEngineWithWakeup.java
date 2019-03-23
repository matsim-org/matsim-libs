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

package org.matsim.contrib.dvrp.passenger;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.ActivityEngine;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;

public class ActivityEngineWithWakeup implements MobsimEngine, ActivityHandler {
	private final EventsManager eventsManager;
	private final WakeupGenerator wakeupGenerator;
	private ActivityEngine delegate;

	private final Queue<AgentEntry> wakeUpList = new PriorityBlockingQueue<>(500, (o1, o2) -> {
		int cmp = Double.compare(o1.time, o2.time);
		return cmp != 0 ? cmp : o1.agent.getId().compareTo(o2.agent.getId());
	});

	@Inject
	ActivityEngineWithWakeup(EventsManager eventsManager, WakeupGenerator wakeupGenerator) {
		this.delegate = new ActivityEngine(eventsManager);
		this.eventsManager = eventsManager;
		this.wakeupGenerator = wakeupGenerator;
	}

	@Override
	public void onPrepareSim() {
		delegate.onPrepareSim();
	}

	@Override
	public void doSimStep(double now) {
		while (!wakeUpList.isEmpty() && wakeUpList.peek().time <= now) {
			final AgentEntry entry = wakeUpList.poll();
			this.eventsManager.processEvent(new AgentWakeupEvent(now, entry.agent.getId()));
			entry.agentWakeup.wakeUp(entry.agent, now);
		}
		delegate.doSimStep(now);
	}

	@Override
	public void afterSim() {
		delegate.afterSim();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
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
		wakeupGenerator.generateWakeups(agent)
				.forEach(wakeup -> wakeUpList.add(new AgentEntry(agent, wakeup.getLeft(), wakeup.getRight())));

		return delegate.handleActivity(agent);
	}

	public interface AgentWakeup {
		void wakeUp(MobsimAgent agent, double now);
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
	private static class AgentEntry {
		public AgentEntry(MobsimAgent agent, double time, AgentWakeup agentWakeup) {
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

		public AgentWakeupEvent(double now, Id<Person> personId) {
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
			attr.put(ATTRIBUTE_PERSON, this.personId.toString());
			return attr;
		}
	}
}
