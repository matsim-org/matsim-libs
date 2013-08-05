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
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.withinday.trafficmonitoring.EarliestLinkExitTimeProvider;

/**
 * This Module is used by a CurrentLegReplanner. It calculates the time
 * when an agent should do CurrentLegReplanning.
 * <p/>
 * The time is estimated as following:
 * When a LinkEnterEvent is thrown the Replanning Time is set to
 * the current time + the FreeSpeed Travel Time. This guarantees that
 * the replanning will be done while the agent is on the Link. After that
 * time, the agent might be already in the outgoing queue of a QLink
 * where not all replanning operations are possible anymore (the agent
 * can e.g. not insert an Activity on its current link anymore).
 * <p/>
 * <p>
 * The replanning interval (multiple replannings on the same link when
 * an agent is stuck on a link due to a traffic jam) has been removed
 * since it cannot be guaranteed that all replanning operations are
 * valid anymore.
 * </p>
 * 
 * @author cdobler
 */
public class LinkReplanningMap implements AgentStuckEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, 
		MobsimAfterSimStepListener {

	private static final Logger log = Logger.getLogger(LinkReplanningMap.class);

	private final EarliestLinkExitTimeProvider earliestLinkExitTimeProvider;

	/*
	 * EXACT... replanning is scheduled for the current time step (time == replanning time)
	 * RESTRICTED ... available replanning operations are restricted (time > replanning time)
	 * UNRESTRICTED ... replanning operations are not restricted (time <= replanning time)
	 */
	private enum TimeFilterMode {
		EXACT, RESTRICTED, UNRESTRICTED
	}

	private final Set<Id> legJustStartedAgents;
	private double currentTime = 0.0;
	
	public LinkReplanningMap(EarliestLinkExitTimeProvider earliestLinkExitTimeProvider) {

		log.info("Note that the LinkReplanningMap has to be registered as an EventHandler and a SimulationListener!");
		this. earliestLinkExitTimeProvider = earliestLinkExitTimeProvider;
		
		this.legJustStartedAgents = new HashSet<Id>();
	}

	@Override
	public void handleEvent(AgentStuckEvent event) {
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
	public Set<Id> getReplanningAgents(final double time) {
		return this.filterAgents(time, TimeFilterMode.EXACT);
	}

	/**
	 * @param time
	 * @return a list of agents who might need an unrestricted replanning and use the given transport mode
	 */
	public Set<Id> getUnrestrictedReplanningAgents(final double time) {
		return this.filterAgents(time, TimeFilterMode.UNRESTRICTED);
	}
	
	/**
	 * @param time
	 * @return a list of agents who might need a restricted replanning and use the given transport mode
	 */
	public Set<Id> getRestrictedReplanningAgents(final double time) {
		return this.filterAgents(time, TimeFilterMode.RESTRICTED);
	}
	
	private Set<Id> filterAgents(final double time, final TimeFilterMode timeMode) {
		Set<Id> set = new HashSet<Id>();
		
		Iterator<Entry<Id, Double>> entries = this.earliestLinkExitTimeProvider.getEarliestLinkExitTimes().entrySet().iterator();
		while (entries.hasNext()) {
			Entry<Id, Double> entry = entries.next();
			Id personId = entry.getKey();

			double replanningTime = entry.getValue();

			// check time
			if (timeMode == TimeFilterMode.EXACT) {
				if (time != replanningTime) continue;				
			} else if (timeMode == TimeFilterMode.RESTRICTED) {
				if (time <= replanningTime) continue;
			} else if (timeMode == TimeFilterMode.UNRESTRICTED) {
				if (time > replanningTime) continue;
			}
			
			// non of the checks fails therefore add agent to the replanning set
			set.add(personId);	
		}

		return set;
	}
	
	/**
	 * @return A list of all agents that are currently performing a leg. Note that
	 * some of them might be limited in the available replanning operations! 
	 */
	public Set<Id> getLegPerformingAgents() {
		return Collections.unmodifiableSet(this.earliestLinkExitTimeProvider.getEarliestLinkExitTimes().keySet());
	}

	/**
	 * @return A list of all agents that have just started a leg. Note that
	 * they cannot end their leg on their current link!
	 */
	public Set<Id> getLegStartedAgents() {
		return Collections.unmodifiableSet(this.legJustStartedAgents);
	}
}