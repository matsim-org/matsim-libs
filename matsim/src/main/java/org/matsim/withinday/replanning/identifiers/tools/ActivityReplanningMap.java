/* *********************************************************************** *
 * project: org.matsim.*
 * ActivityReplanningMap.java
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

package org.matsim.withinday.replanning.identifiers.tools;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.withinday.events.ReplanningEvent;
import org.matsim.withinday.events.handler.ReplanningEventHandler;
import org.matsim.withinday.mobsim.MobsimDataProvider;

/*
 * This Module is used by a NextLegReplanner. It calculates the time
 * when an agent should do NextLegReplanning.
 *
 * When an ActivityStartEvent is thrown the Replanning Time is set to
 * the scheduled departure Time of the Activity.
 * 
 * In previous versions of this class, TreeMaps/Sets have been returned
 * to produce deterministic outcomes. However, they have been replaced 
 * by HashMaps/Sets. If the order of the entries in the returned collections
 * is important, this has to be ensured where the collections are used.
 * Identifiers, for example, iterate over the collections, check whether
 * the contained agents have to be replanned and put them in a TreeSet.
 * cdobler, apr'14 
 */
public class ActivityReplanningMap implements PersonStuckEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, ReplanningEventHandler,
		MobsimInitializedListener, MobsimAfterSimStepListener {

	private static final Logger log = Logger.getLogger(ActivityReplanningMap.class);

	private final MobsimDataProvider mobsimDataProvider;
	
	/*
	 * Agents that have started an activity in the current time step. We store the
	 * MobsimAgents and their Ids (which are contained in the activity start
	 * event). By doing so, the lookup Id -> MobsimAgent is performed by the
	 * EventHandler which is parallelized.
	 * However, this will result in few unnecessary lookups for agents who end their
	 * activity in the same time step as they start it. 
	 */
	private final Map<Id, MobsimAgent> startingAgents;	// PersonId

	/*
	 * Contains activity end times of agents that are currently performing an activity.
	 * This is required in case an agent changes its activity end time. To be able to
	 * remove the agent from the activityPerformingAgents we need to know its original
	 * activity end time.
	 */
	private final Map<Id, Double> activityEndTimes;	// scheduled activity end times
	
	/*
	 * Containing all activity performing agents sorted by their scheduled activity end time.
	 */
	private final SortedSet<AgentEntry> activityPerformingAgents;

	private Map<Id, MobsimAgent> personAgentsToReplanActivityEndCache = null;
	private double personAgentsToReplanActivityEndCacheTime = Double.NaN;
		
	public ActivityReplanningMap(MobsimDataProvider mobsimDataProvider) {
		log.info("Note that the ActivityReplanningMap has to be registered as an EventHandler and a SimulationListener!");
		
		this.mobsimDataProvider = mobsimDataProvider;

		this.startingAgents = new HashMap<Id, MobsimAgent>();
		this.activityEndTimes = new HashMap<Id, Double>();
		
		this.activityPerformingAgents = new TreeSet<AgentEntry>(new AgentEntryComparator());
	}

	/*
	 * When the simulation starts the agents are all performing an activity.
	 * There is no activity start event so we have to collect by hand by
	 * iterating over all links and vehicles.
	 *
	 * Additionally we create the Mapping between the PersonIds and the
	 * PersonDriverAgents.
	 */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {

		for (MobsimAgent mobsimAgent : this.mobsimDataProvider.getAgents().values()) {
			
			// get the agent's activity end time and mark it as currently performing an Activity
			double activityEndTime = mobsimAgent.getActivityEndTime();
			
			// add the agent to the collections
			this.activityEndTimes.put(mobsimAgent.getId(), activityEndTime);
			this.activityPerformingAgents.add(new AgentEntry(mobsimAgent.getId(), mobsimAgent, activityEndTime));
		}
	}
	
	/*
	 * The Activity Start Events are thrown before the Activity Start has been handled
	 * by the Simulation. As a result the Departure Time is not set at that time.
	 * We have to wait until the SimStep has been fully simulated and retrieve
	 * the Activity DepatureTimes then.
	 */
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		
		// reset caches by resetting the cache times
		this.personAgentsToReplanActivityEndCacheTime = Double.NaN;
		
		double now = e.getSimulationTime();
		for (MobsimAgent mobsimAgent : startingAgents.values()) {

			double departureTime = mobsimAgent.getActivityEndTime();

			/*
			 * If it is the last scheduled Activity the departureTime is -infinity.
			 * Otherwise we select the agent for a replanning.
			 */
			if (departureTime >= now) {
				this.activityEndTimes.put(mobsimAgent.getId(), departureTime);
				this.activityPerformingAgents.add(new AgentEntry(mobsimAgent.getId(), mobsimAgent, 
						mobsimAgent.getActivityEndTime()));
			} else {
				log.warn("Departure time is in the past - ignoring activity!");
			}
		}
		startingAgents.clear();
	}

	/*
	 * Collect the Agents that are starting an Activity in
	 * the current Time Step.
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id agentId = event.getPersonId();
		this.startingAgents.put(agentId, this.mobsimDataProvider.getAgent(agentId));
	}

	/*
	 * Nothing to do here?
	 * Agents should be removed from the map if their
	 * replanning time has come.
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id agentId = event.getPersonId();
		this.startingAgents.remove(agentId);
		
		Double activityEndTime = this.activityEndTimes.remove(agentId);
		if (activityEndTime != null) {
			this.activityPerformingAgents.remove(new AgentEntry(agentId, null, activityEndTime));
		}
	}

	/*
	 * Nothing to do here?
	 * Agents should not be removed from the simulation if they are performing an activity.
	 */
	@Override
	public void handleEvent(PersonStuckEvent event) {
	}
	
	@Override
	public void handleEvent(ReplanningEvent event) {
		
		// check whether the agent is performing an activity
		Double activityEndTime = this.activityEndTimes.get(event.getPersonId());
		if (activityEndTime != null) {
			
			// check whether the agent has changed its planned departure time
			MobsimAgent agent = this.mobsimDataProvider.getAgent(event.getPersonId());
			if (activityEndTime != agent.getActivityEndTime()) {
				// Update the agent's activity end time.
				this.activityEndTimes.put(agent.getId(), agent.getActivityEndTime());
				
				// Update the activity performing agents set. To do so, remove old entry and add new one.
				this.activityPerformingAgents.remove(new AgentEntry(agent.getId(), agent, activityEndTime));
				this.activityPerformingAgents.add(new AgentEntry(agent.getId(), agent, agent.getActivityEndTime()));
			}
		}
	}
	
	/*
	 * Returns a set containing the Ids of all agents that are currently performing an activity.
	 */
	public Set<Id> getActivityPerformingAgents() {
		return Collections.unmodifiableSet(this.activityEndTimes.keySet());
	}
	
	/*
	 * Returns a List of all agents that are going to end their activity before the given
	 * time. Typically, this is the current simulation time.
	 */
	public Map<Id, MobsimAgent> getActivityEndingAgents(double time) {
		
		if (time == this.personAgentsToReplanActivityEndCacheTime) return this.personAgentsToReplanActivityEndCache;
		else {
			this.personAgentsToReplanActivityEndCache = new HashMap<Id, MobsimAgent>();
			this.personAgentsToReplanActivityEndCacheTime = time;
			
			Iterator<AgentEntry> iter = this.activityPerformingAgents.iterator();
			while (iter.hasNext()) {
				AgentEntry agentEntry = iter.next();

				/*
				 * This is typically called before the simengines simulate the time step.
				 * If time >= activityEndTime, the agent will depart in this time step.
				 */
				if (time >= agentEntry.activityEndTime) {
					this.personAgentsToReplanActivityEndCache.put(agentEntry.agent.getId(), agentEntry.agent);
				} 
				/*
				 * Since the activityPerformingAgents is a sorted set, we can stop searching
				 * after we found an agent with a later departure time.
				 */
				else break;
			}
			
			return this.personAgentsToReplanActivityEndCache;
		}
	}

	@Override
	public void reset(int iteration) {
		this.startingAgents.clear();
		this.activityEndTimes.clear();

		this.personAgentsToReplanActivityEndCache = null;
		this.personAgentsToReplanActivityEndCacheTime = Double.NaN;
	}

	/*
	 * The agent's Id is stored redundant. Doing so allows us to save a
	 * lookup Id -> agent in the handleEvent(ActivityEndEvent) method. There,
	 * we set the agent field to null, which is fine since the Comparator checks
	 * the Id field and does not retrieve the Id form the agent.
	 */
	private static final class AgentEntry {
		Id agentId;
		MobsimAgent agent;
		double activityEndTime;
		
		public AgentEntry(Id agentId, MobsimAgent agent, double activityEndTime) {
			this.agentId = agentId;
			this.agent = agent;
			this.activityEndTime = activityEndTime;
		}
	}
	
	private static final class AgentEntryComparator implements Comparator<AgentEntry> {
		
		@Override
		public int compare(AgentEntry arg0, AgentEntry arg1) {
			int cmp = Double.compare(arg0.activityEndTime, arg1.activityEndTime);
			if (cmp == 0) {
				/*
				 * Both depart at the same time -> let the one with the larger id be first (=smaller) to
				 * be deterministic. Any other decision criteria would also be fine.
				 */
				return arg1.agentId.compareTo(arg0.agentId);
			}
			return cmp;
		}
	}
}