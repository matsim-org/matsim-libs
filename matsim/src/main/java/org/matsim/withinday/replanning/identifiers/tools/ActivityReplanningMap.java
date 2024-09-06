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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.withinday.events.ReplanningEvent;
import org.matsim.withinday.events.handler.ReplanningEventHandler;
import org.matsim.withinday.mobsim.MobsimDataProvider;

/**
 * This class tracks agents and their activity end times. It can be used to identify
 * those agents which are going to end their activities in the current time step.
 * Doing so allows one to e.g. extend their activities. Moreover, it can return a set
 * containing all agents currently performing activites.
 */
public class ActivityReplanningMap implements PersonStuckEventHandler,
		ActivityStartEventHandler, ActivityEndEventHandler, ReplanningEventHandler,
		MobsimInitializedListener, MobsimAfterSimStepListener {

	private static final Logger log = LogManager.getLogger(ActivityReplanningMap.class);

	private final MobsimDataProvider mobsimDataProvider;

	/*
	 * Agents that have started an activity in the current time step. We store the
	 * MobsimAgents and their Ids (which are contained in the activity start
	 * event). By doing so, the lookup Id -> MobsimAgent is performed by the
	 * EventHandler which is parallelized.
	 * However, this will result in few unnecessary lookups for agents who end their
	 * activity in the same time step as they start it.
	 */
	private final Map<Id<Person>, MobsimAgent> startingAgents;	// PersonId

	/*
	 * Contains activity end times of agents that are currently performing an activity.
	 * This is required in case an agent changes its activity end time. To be able to
	 * remove the agent from the activityPerformingAgents we need to know its original
	 * activity end time.
	 */
	private final Map<Id<Person>, Double> activityEndTimes;	// scheduled activity end times

	/*
	 * Contains a map for each time bin (a bin equals a time step in the mobility simulation).
	 * The set contains all agents ending their activity in that time bin.
	 */
	private final Map<Integer, Map<Id<Person>, MobsimAgent>> activityPerformingAgents;

	// package protected to be accessible for test case
	/*package*/ double simStartTime;
	/*package*/ double timeStepSize;

	@Inject
	public ActivityReplanningMap(MobsimDataProvider mobsimDataProvider, EventsManager eventsManager) {
		eventsManager.addHandler(this);
		log.info("Note that the ActivityReplanningMap has to be registered as an EventHandler and a SimulationListener!");

		this.mobsimDataProvider = mobsimDataProvider;

		this.startingAgents = new HashMap<>();
		this.activityEndTimes = new HashMap<>();

		this.activityPerformingAgents = new ConcurrentHashMap<>();
	}

	/*
	 * When the simulation starts the agents are all performing an activity. We collect
	 * them and check their activity end time.
	 */
	@Override
	public void notifyMobsimInitialized(MobsimInitializedEvent e) {

		MobsimTimer mobsimTimer = ((QSim) e.getQueueSimulation()).getSimTimer();
		this.simStartTime = mobsimTimer.getSimStartTime();
		this.timeStepSize = mobsimTimer.getSimTimestepSize();

		this.activityPerformingAgents.clear();

		for (MobsimAgent mobsimAgent : this.mobsimDataProvider.getAgents().values()) {

			// get the agent's activity end time and mark it as currently performing an Activity
			double activityEndTime = mobsimAgent.getActivityEndTime();

			// add the agent to the collections
			this.activityEndTimes.put(mobsimAgent.getId(), activityEndTime);

			int bin = this.getTimeBin(activityEndTime);
			Map<Id<Person>, MobsimAgent> map = getMapForTimeBin(bin);
			map.put(mobsimAgent.getId(), mobsimAgent);
		}

	}

	/*
	 * The Activity Start Events are thrown before the Activity Start has been handled
	 * by the Simulation. As a result the Departure Time is not set at that time.
	 * We have to wait until the SimStep has been fully simulated and retrieve
	 * the activity departure times then.
	 */
	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {

		double now = e.getSimulationTime();
		for (MobsimAgent mobsimAgent : startingAgents.values()) {

			double departureTime = mobsimAgent.getActivityEndTime();

			/*
			 * If it is the last scheduled Activity the departureTime is -infinity.
			 * Otherwise we select the agent for a replanning.
			 */
			if (departureTime >= now) {
				this.activityEndTimes.put(mobsimAgent.getId(), departureTime);
				int bin = this.getTimeBin(mobsimAgent.getActivityEndTime());
				Map<Id<Person>, MobsimAgent> map = getMapForTimeBin(bin);
				map.put(mobsimAgent.getId(), mobsimAgent);
			} else {
				log.warn("Departure time is in the past - ignoring activity!");
			}
		}
		this.startingAgents.clear();

		/*
		 * Remove current time bin from activityPerformingAgents map. They have been handled
		 * in the current time step.
		 */
		this.activityPerformingAgents.remove(this.getTimeBin(now));
	}

	/*
	 * We collect departures in time bins. Each bin contains all departures of one
	 * time step of the mobility simulation.
	 *
	 * Method is package protected to be accessible for test case.
	 */
	/*package*/ int getTimeBin(double time) {

		double timeAfterSimStart = time - simStartTime;

		/*
		 * Agents who end their first activity before the simulation has started
		 * will depart in the first time step.
		 */
		if (timeAfterSimStart <= 0.0) return 0;

		/*
		 * Calculate the bin for the given time. Increase it by one if the result
		 * of the modulo operation is > 0. If it is 0, it is the last time value
		 * which is part of the previous bin.
		 */
		int bin = (int) (timeAfterSimStart / timeStepSize);
		if (timeAfterSimStart % timeStepSize != 0.0) bin++;

		return bin;
	}

	private Map<Id<Person>, MobsimAgent> getMapForTimeBin(int bin) {
		Map<Id<Person>, MobsimAgent> map = this.activityPerformingAgents.get(bin);
		if (map == null) {
			map = new HashMap<>();
			this.activityPerformingAgents.put(bin, map);
		}
		return map;
	}

	/*
	 * Collect the agents that are starting an Activity in the current time step.
	 * Do the agent lookup here since this can be executed parallel to the mobsim.
	 */
	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Person> agentId = event.getPersonId();
		this.startingAgents.put(agentId, this.mobsimDataProvider.getAgent(agentId));
	}

	/*
	 * Nothing to do here?
	 * Agents should be removed from the map if their replanning time has come.
	 */
	@Override
	public void handleEvent(ActivityEndEvent event) {
		Id<Person> agentId = event.getPersonId();
		this.startingAgents.remove(agentId);

		Double activityEndTime = this.activityEndTimes.remove(agentId);
		if (activityEndTime != null) {
			Map<Id<Person>, MobsimAgent> map;
			// remove
			map = this.getMapForTimeBin(this.getTimeBin(activityEndTime));
			map.remove(agentId);
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

				// Update the activity performing agents map. To do so, remove old entry and add new one.
				Map<Id<Person>, MobsimAgent> map;
				// remove
				map = this.getMapForTimeBin(this.getTimeBin(activityEndTime));
				map.remove(agent.getId());

				// add
				map = this.getMapForTimeBin(this.getTimeBin(agent.getActivityEndTime()));
				map.put(agent.getId(), agent);
			}
		}
	}

	/**
	 * Returns a set containing the Ids of all agents that are currently performing an activity.
	 */
	public Set<Id<Person>> getActivityPerformingAgents() {
		return Collections.unmodifiableSet(this.activityEndTimes.keySet());
	}

	/**
	 * Returns a Collection containing all agents that are going to end their activity in the
	 * time step belongs to the given time. Typically, this is the current simulation time.
	 * For times in the past an empty set is returned.
	 * Since in data structure in the background the MobsimAgents are available, we return them
	 * instead of only their Ids as the getActivityPerformingAgents() method does.
	 */
	public Collection<MobsimAgent> getActivityEndingAgents(double time) {
		return Collections.unmodifiableCollection(this.getMapForTimeBin(this.getTimeBin(time)).values());
	}

	@Override
	public void reset(int iteration) {
		this.startingAgents.clear();
		this.activityEndTimes.clear();
	}
}
