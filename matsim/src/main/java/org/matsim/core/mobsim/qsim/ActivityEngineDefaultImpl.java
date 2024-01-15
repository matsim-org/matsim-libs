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

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimAgent.State;

class ActivityEngineDefaultImpl implements ActivityEngine {
	private static final Logger log = LogManager.getLogger( ActivityEngineDefaultImpl.class ) ;

	private final EventsManager eventsManager;

	@Inject
	ActivityEngineDefaultImpl( EventsManager eventsManager ) {
		this.eventsManager = eventsManager;
	}

//	public ActivityEngineDefaultImpl( EventsManager eventsManager, AgentCounter agentCounter ) {
//		this.eventsManager = eventsManager;
//	}

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
		AgentEntry( MobsimAgent agent, double activityEndTime ) {
			this.agent = agent;
			this.activityEndTime = activityEndTime;
		}
		private final MobsimAgent agent;
		private final double activityEndTime;
	}

	private InternalInterface internalInterface;

	/**
	 * This list needs to be a "blocking" queue since this is needed for
	 * thread-safety in the parallel qsim. cdobler, oct'10
	 */
	private final Queue<AgentEntry> activityEndsList = new PriorityBlockingQueue<>(500, (e0, e1) -> {
		int cmp = Double.compare(e0.activityEndTime, e1.activityEndTime);
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
			return e1.agent.getId().compareTo(e0.agent.getId());
		}
		return cmp;
	});

	// See handleActivity for the reason for this.
	private boolean beforeFirstSimStep = true;

	@Override
	public void onPrepareSim() {
		// Nothing to do here
	}

	@Override
	public void doSimStep(double time) {
		beforeFirstSimStep = false;
		while (activityEndsList.peek() != null) {
			if (activityEndsList.peek().activityEndTime <= time) {
				MobsimAgent agent = activityEndsList.poll().agent;
				unregisterAgentAtActivityLocation(agent);
				agent.endActivityAndComputeNextState(time);
				internalInterface.arrangeNextAgentState(agent);
			} else {
				return;
			}
		}
	}

	@Override
	public void afterSim() {
		double now = this.internalInterface.getMobsim().getSimTimer().getTimeOfDay();
		for (AgentEntry entry : activityEndsList) {
			if (entry.activityEndTime != Double.POSITIVE_INFINITY) {
				// since we are at an activity, it is not plausible to assume that the agents know mode or destination
				// link id.  Thus generating the event with ``null'' in the corresponding entries.  kai, mar'12
				eventsManager.processEvent(new PersonStuckEvent(now, entry.agent.getId(), null, null));
			}
		}
		activityEndsList.clear();
	}

	@Override
	public void setInternalInterface(InternalInterface internalInterface) {
		this.internalInterface = internalInterface;
	}


	/**
	 *
	 * This method is called by QSim to pass in agents which then "live" in the activity layer until they are handed out again
	 * through the internalInterface.
	 *
	 * It is called not before onPrepareSim() and not after afterSim(), but it may be called before, after, or from doSimStep(),
	 * and even from itself (i.e. it must be reentrant), since internalInterface.arrangeNextAgentState() may trigger
	 * the next Activity.
	 *
	 */
	@Override
	public boolean handleActivity(MobsimAgent agent) {
		if (agent.getActivityEndTime() == Double.POSITIVE_INFINITY) {
			// This is the last planned activity.
			// So the agent goes to sleep.
			internalInterface.getMobsim().getAgentCounter().decLiving();
		} else if (agent.getActivityEndTime() <= internalInterface.getMobsim().getSimTimer().getTimeOfDay() && !beforeFirstSimStep) {
			// This activity is already over (planned for 0 duration)
			// So we proceed immediately.
			agent.endActivityAndComputeNextState(internalInterface.getMobsim().getSimTimer().getTimeOfDay());
			internalInterface.arrangeNextAgentState(agent) ;
		} else {
			// The agent commences an activity on this link.
			final AgentEntry agentEntry = new AgentEntry(agent, agent.getActivityEndTime());
			activityEndsList.add(agentEntry);
			internalInterface.registerAdditionalAgentOnLink(agent);
		}
		// Why beforeFirstSimStep matters:
		// - If this class has never had a doSimStep() when this method is called, this means that this Agent is having its
		// "overnight", i.e. first Activity.
		// - This means that this ActivityEngine should not just pass this MobsimAgent along if its activityEndTime has already ended,
		// but queue it in with other such Agents and let them all leave on doSimStep(), because we expect those Agents to leave
		// in the order specified by the activityQueue (no matter if it is a good order or not, see comment there). The order in which new Agents enter
		// the simulation and are passed into this method is a different one, so this matters.
		// - This is safe (Agents will not miss a second), simply because doSimStep for this time step has not yet happened.
		// - It also means that e.g. OTFVis will probably display all Agents while they are in their first Activity before you press play.
		// - On the other hand, agents whose first activity is also their last activity go right to sleep "inside" this engine.
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
	@Override
	public void rescheduleActivityEnd(final MobsimAgent agent) {
		if ( agent.getState()!=State.ACTIVITY ) {
			return ;
		}


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
				((org.matsim.core.mobsim.qsim.AgentCounter) internalInterface.getMobsim().getAgentCounter()).incLiving();
			}
		} else if (newActivityEndTime == Double.POSITIVE_INFINITY) {
			/*
			 * After the re-planning the agent's current activity has changed to its last activity.
			 * Therefore the agent is de-activated. cdobler, oct'11
			 */
			unregisterAgentAtActivityLocation(agent);
			internalInterface.getMobsim().getAgentCounter().decLiving();
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
