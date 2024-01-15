/* *********************************************************************** *
 * project: org.matsim.*
 * LinkReplanningMap.java
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
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;

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
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;

/**
 * This Module is used by a CurrentLegReplanner. It calculates the time
 * when an agent should do CurrentLegReplanning.
 * <p></p>
 * The time is estimated as following:
 * When a LinkEnterEvent is thrown the Replanning Time is set to
 * the current time + the FreeSpeed Travel Time. This guarantees that
 * the replanning will be done while the agent is on the Link. After that
 * time, the agent might be already in the outgoing queue of a QLink
 * where not all replanning operations are possible anymore (the agent
 * can e.g. not insert an Activity on its current link anymore).
 * <p></p>
 * <p>
 * The replanning interval (multiple replannings on the same link when
 * an agent is stuck on a link due to a traffic jam) has been removed
 * since it cannot be guaranteed that all replanning operations are
 * valid anymore.
 * </p>
 *
 * @author cdobler
 */
public class LinkReplanningMap implements PersonStuckEventHandler, ActivityStartEventHandler, ActivityEndEventHandler,
		MobsimAfterSimStepListener {

	private static final Logger log = LogManager.getLogger(LinkReplanningMap.class);

	private final EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;

	/*
	 * EXACT... replanning is scheduled for the current time step (time == replanning time)
	 * RESTRICTED ... available replanning operations are restricted (time > replanning time)
	 * UNRESTRICTED ... replanning operations are not restricted (time <= replanning time)
	 */
	private enum TimeFilterMode {
		EXACT, RESTRICTED, UNRESTRICTED
	}

	private final Set<Id<Person>> legJustStartedAgents;
	private double currentTime = 0.0;

	@Inject
	public LinkReplanningMap(EarliestLinkExitTimeProvider earliestLinkExitTimeProvider, EventsManager eventsManager) {

		eventsManager.addHandler(this);
		log.info("Note that the LinkReplanningMap has to be registered as an EventHandler and a SimulationListener!");
		this. earliestLinkExitTimeProvider = earliestLinkExitTimeProvider;

		this.legJustStartedAgents = new HashSet<Id<Person>>();
	}

	@Override
	public void handleEvent(PersonStuckEvent event) {
		this.legJustStartedAgents.remove(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		checkTime(event.getTime());
		this.legJustStartedAgents.add(event.getPersonId());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		this.legJustStartedAgents.remove(event.getPersonId());
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		checkTime(e.getSimulationTime());
	}

	/*
	 * If time > currentTime, then legJustStartedAgents is not up to date
	 * anymore, therefore we clear all entries and update currentTime.
	 */
	private void checkTime(double time) {
		if (time > currentTime) {
			this.currentTime = time;
			this.legJustStartedAgents.clear();
		}
	}

	@Override
	public void reset(int iteration) {
		currentTime = 0.0;
		this.legJustStartedAgents.clear();
	}

	/**
	 * @param time
	 * @return a list of agents who might need a replanning
	 */
	public Set<Id<Person>> getReplanningAgents(final double time) {

		Set<Id<Person>> set = this.earliestLinkExitTimeProvider.getEarliestLinkExitTimesPerTimeStep(time);
		if (set != null) return Collections.unmodifiableSet(set);
		else return new HashSet<>();
	}

	/**
	 * @param time
	 * @return a list of agents who might need an unrestricted replanning and use the given transport mode
	 */
	public Set<Id<Person>> getUnrestrictedReplanningAgents(final double time) {
		return this.filterAgents(time, TimeFilterMode.UNRESTRICTED);
	}

	/**
	 * @param time
	 * @return a list of agents who might need a restricted replanning and use the given transport mode
	 */
	public Set<Id<Person>> getRestrictedReplanningAgents(final double time) {
		return this.filterAgents(time, TimeFilterMode.RESTRICTED);
	}

	private Set<Id<Person>> filterAgents(final double time, final TimeFilterMode timeMode) {

		Set<Id<Person>> set = new HashSet<>();

		Set<Entry<OptionalTime, Set<Id<Person>>>> entries = this.earliestLinkExitTimeProvider.getEarliestLinkExitTimesPerTimeStep().entrySet();

		for (Entry<OptionalTime, Set<Id<Person>>> entry : entries) {
			OptionalTime earliestLinkExitTime = entry.getKey();

			// check time
			if (timeMode == TimeFilterMode.RESTRICTED) {
				if (time <= earliestLinkExitTime.seconds()) continue;
			} else if (timeMode == TimeFilterMode.UNRESTRICTED) {
				if (time > earliestLinkExitTime.seconds()) continue;
			} else {
				throw new RuntimeException("Unexpected TimeFilterMode was found: " + timeMode.toString());
			}

			// non of the checks fails therefore add agents to the set
			set.addAll(entry.getValue());
		}

		return set;
	}

	/**
	 * @return A list of all agents that are currently performing a leg. Note that
	 * some of them might be limited in the available replanning operations!
	 */
	public Set<Id<Person>> getLegPerformingAgents() {
		return Collections.unmodifiableSet(this.earliestLinkExitTimeProvider.getEarliestLinkExitTimes().keySet());
	}

	/**
	 * @return A list of all agents that have just started a leg. Note that
	 * they cannot end their leg on their current link!
	 */
	public Set<Id<Person>> getLegStartedAgents() {
		return Collections.unmodifiableSet(this.legJustStartedAgents);
	}
}
