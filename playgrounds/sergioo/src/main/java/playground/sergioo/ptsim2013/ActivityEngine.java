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

package playground.sergioo.ptsim2013;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.qsim.InternalInterface;
import org.matsim.core.mobsim.qsim.interfaces.ActivityHandler;
import org.matsim.core.mobsim.qsim.interfaces.MobsimEngine;
import org.matsim.core.utils.misc.Time;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

public class ActivityEngine implements MobsimEngine, ActivityHandler {

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
		public AgentEntry(MobsimAgent agent, double activityEndTime) {
			this.agent = agent;
			this.activityEndTime = activityEndTime;
		}
		MobsimAgent agent;
		double activityEndTime;
	}

	/**
	 * This list needs to be a "blocking" queue since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private Queue<AgentEntry> activityEndsList = new PriorityBlockingQueue<AgentEntry>(500, new Comparator<AgentEntry>() {

		@Override
		public int compare(AgentEntry arg0, AgentEntry arg1) {
			int cmp = Double.compare(arg0.activityEndTime, arg1.activityEndTime);
			if (cmp == 0) {
				// Both depart at the same time -> let the one with the larger id be first (=smaller)
				//
				// yy We are not sure what the above comment line is supposed to say.  Presumably, it is supposed
				// to say that the agent with the larger ID should be "smaller" one in the comparison. 
				// In practice, it seems
				// that something like "emob_9" is before "emob_8", and something like "emob_10" before "emob_1".
				// It is unclear why this convention is supposed to be helpful.
				// kai & dominik, jul'12
				//
				return arg1.agent.getId().compareTo(arg0.agent.getId());
			}
			return cmp;
		}

	});

	private InternalInterface internalInterface;

	@Override
	public void onPrepareSim() {
		// Nothing to do here
	}

	@Override
	public void doSimStep(double time) {
		while (activityEndsList.peek() != null) {
			MobsimAgent agent = activityEndsList.peek().agent;
			if (activityEndsList.peek().activityEndTime <= time) {
				activityEndsList.poll();
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndComputeNextState(time);
				internalInterface.arrangeNextAgentState(agent) ;
			} else {
				return;
			}
		}
	}

	@Override
	public void afterSim() {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (AgentEntry entry : activityEndsList) {
			if (entry.activityEndTime!=Double.POSITIVE_INFINITY && entry.activityEndTime!=Time.UNDEFINED_TIME) {
				// since we are at an activity, it is not plausible to assume that the agents know mode or destination 
				// link id.  Thus generating the event with ``null'' in the corresponding entries.  kai, mar'12
				EventsManager eventsManager = ((QSim) internalInterface.getMobsim()).getEventsManager();
				eventsManager.processEvent(new PersonStuckEvent(now, entry.agent.getId(), null, null));
			}
		}
		activityEndsList.clear();
	}

	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}

	@Override
	public boolean handleActivity(MobsimAgent agent) {
		/*
		 * Add the agent to the activityEndsList if it is not its last
		 * activity and register it on the link. Otherwise decrease the
		 * agents counter by one.
		 */
		if (agent.getActivityEndTime() == Double.POSITIVE_INFINITY) {
			((QSim) internalInterface.getMobsim()).getAgentCounter().decLiving();
		} else {
			activityEndsList.add(new AgentEntry(agent, agent.getActivityEndTime()));
			internalInterface.registerAdditionalAgentOnLink(agent);			
		}
		return true;
	}

	/**
	 * For within-day replanning. Tells this engine that the activityEndTime the agent reports may have changed since 
	 * the agent was added to this engine through handleActivity.
	 * May be merged with handleActivity, since this engine can know by itself if it was called the first time
	 * or not.
	 * 
	 * @param agent The agent.
	 */
	void rescheduleActivityEnd(final MobsimAgent agent) {
		double newActivityEndTime = agent.getActivityEndTime();
		AgentEntry oldEntry = removeAgentFromQueue(agent);

		// The intention in the following is that an agent that is no longer alive has an activity end time of infinity.  The number of
		// alive agents is only modified when an activity end time is changed between a finite time and infinite.  kai, jun'11
		if (oldEntry == null) {
			if (newActivityEndTime == Double.POSITIVE_INFINITY) {
				// agent was de-activated and still should be de-activated - nothing to do here
			} else {
				// re-activate the agent
				activityEndsList.add(new AgentEntry(agent, newActivityEndTime));
				internalInterface.registerAdditionalAgentOnLink(agent);
				((AgentCounter) ((QSim) internalInterface.getMobsim()).getAgentCounter()).incLiving();
			}
		} else if (newActivityEndTime == Double.POSITIVE_INFINITY) {
			/*
			 * After the re-planning the agent's current activity has changed to its last activity.
			 * Therefore the agent is de-activated. cdobler, oct'11
			 */
			unregisterAgentAtActivityLocation(agent);
			((QSim) internalInterface.getMobsim()).getAgentCounter().decLiving();
		} else {
			/*
			 *  The activity is just rescheduled during the day, so we keep the agent active. cdobler, oct'11
			 */
			activityEndsList.add(new AgentEntry(agent, newActivityEndTime));
		}
	}

	private AgentEntry removeAgentFromQueue(MobsimAgent agent) {
		Iterator<AgentEntry> iterator = activityEndsList.iterator();
		while (iterator.hasNext()) {
			AgentEntry entry = iterator.next();
			if (entry.agent == agent) {
				iterator.remove();
				return entry;
			}
		}
		return null;
	}

	private void unregisterAgentAtActivityLocation(final MobsimAgent agent) {
		Id<Person> agentId = agent.getId();
		Id<Link> linkId = agent.getCurrentLinkId();
		if (linkId != null) { // may be bushwacking
			internalInterface.unregisterAdditionalAgentOnLink(agentId, linkId);
		}
	}

}