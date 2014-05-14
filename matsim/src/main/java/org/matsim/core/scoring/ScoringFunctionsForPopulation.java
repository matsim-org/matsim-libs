/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunctionsForPopulation.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * 
 * This class helps EventsToScore by keeping ScoringFunctions for the entire Population - one per Person -, and dispatching Activities
 * and Legs to the ScoringFunctions. It also gives out the ScoringFunctions, so they can be given other events by EventsToScore.
 * It is not independently useful. Please do not make public.
 * @author michaz
 *
 */
class ScoringFunctionsForPopulation implements ActivityHandler, LegHandler {

    private final TreeMap<Id, ArrayList<PersonExperienceListener>> agentScorers = new TreeMap<Id, ArrayList<PersonExperienceListener>>();

	private final Map<Id, Plan> agentRecords = new TreeMap<Id,Plan>();

    ScoringFunctionsForPopulation(Scenario scenario, HashSet<PersonExperienceListenerProvider> personExperienceListenerFactories) {
        for (Person person : scenario.getPopulation().getPersons().values()) {
            ArrayList<PersonExperienceListener> personExperienceListeners = new ArrayList<PersonExperienceListener>();
            for (PersonExperienceListenerProvider personExperienceListenerFactory : personExperienceListenerFactories) {
                PersonExperienceListener data = personExperienceListenerFactory.provideFor(person);
                personExperienceListeners.add(data);
            }
            this.agentScorers.put(person.getId(), personExperienceListeners);
			this.agentRecords.put(person.getId(), new PlanImpl());
		}
	}

	/**
	 * Returns the scoring function for the specified agent. If the agent
	 * already has a scoring function, that one is returned. If the agent does
	 * not yet have a scoring function, a new one is created and assigned to the
	 * agent and returned.
	 *
	 * @param agentId
	 *            The id of the agent the scoring function is requested for.
	 * @return The scoring function for the specified agent.
	 */
	public ArrayList<PersonExperienceListener> getScoringFunctionForAgent(final Id agentId) {
		return this.agentScorers.get(agentId);
	}

	public Map<Id, Plan> getAgentRecords() {
		return agentRecords;
	}

	@Override
	public void handleActivity(Id agentId, Activity activity) {
		ArrayList<PersonExperienceListener> scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
		if (scoringFunctionForAgent != null) {
            for (PersonExperienceListener listener : scoringFunctionForAgent) {
                listener.handleActivity(activity);
                agentRecords.get(agentId).addActivity(activity);
            }
		}
	}

	@Override
	public void handleLeg(Id agentId, Leg leg) {
		ArrayList<PersonExperienceListener> scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
		if (scoringFunctionForAgent != null) {
            for (PersonExperienceListener listener : scoringFunctionForAgent) {
                listener.handleLeg(leg);
                agentRecords.get(agentId).addLeg(leg);
            }
		}
	}

	public void finishScoringFunctions() {
		for (ArrayList<PersonExperienceListener> sf : agentScorers.values()) {
            for (PersonExperienceListener listener : sf) {
                listener.finish();
            }
		}
	}

}
