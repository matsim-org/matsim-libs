
/* *********************************************************************** *
 * project: org.matsim.*
 * ExperiencedPlansServiceImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

import com.google.inject.Inject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;

class ExperiencedPlansServiceImpl implements ExperiencedPlansService, EventsToLegs.LegHandler, EventsToActivities.ActivityHandler {

	private final static Logger log = LogManager.getLogger(ExperiencedPlansServiceImpl.class);

	@Inject
	Network network;
	@Inject private Config config;
	@Inject private Population population;
	@Inject(optional = true) private ScoringFunctionsForPopulation scoringFunctionsForPopulation;

	private final IdMap<Person, Plan> agentRecords = new IdMap<>(Person.class);

	@Inject
    ExperiencedPlansServiceImpl(ControlerListenerManager controlerListenerManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
        controlerListenerManager.addControlerListener(new IterationStartsListener() {
            @Override
            public void notifyIterationStarts(IterationStartsEvent event) {
                for (Person person : population.getPersons().values()) {
                    agentRecords.put(person.getId(), PopulationUtils.createPlan());
                }
            }
        });
        eventsToActivities.addActivityHandler(this);
        eventsToLegs.addLegHandler(this);
    }

    ExperiencedPlansServiceImpl(EventsToActivities eventsToActivities, EventsToLegs eventsToLegs, Scenario scenario) {
        this.population = scenario.getPopulation();

        for (Person person : population.getPersons().values()) {
            agentRecords.put(person.getId(), PopulationUtils.createPlan());
        }
        eventsToActivities.addActivityHandler(this);
        eventsToLegs.addLegHandler(this);
        this.config = scenario.getConfig();
    }

	@Override
	synchronized public void handleLeg(PersonExperiencedLeg o) {
		// Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
		// on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
		Id<Person> agentId = o.getAgentId();
		Leg leg = o.getLeg();
		Plan plan = agentRecords.get(agentId);
		if (plan != null) {
			plan.addLeg(leg);
		}
	}

	@Override
	synchronized public void handleActivity(PersonExperiencedActivity o) {
		// Has to be synchronized because the thing which sends Legs and the thing which sends Activities can run
		// on different threads. Will go away when/if we get a more Actor or Reactive Streams like event infrastructure.
		Id<Person> agentId = o.getAgentId();
		Activity activity = o.getActivity();
		Plan plan = agentRecords.get(agentId);
		if (plan != null) {
			agentRecords.get(agentId).addActivity(activity);
		}
	}

	@Override
	public void writeExperiencedPlans(String iterationFilename) {
//		finishIteration(); // already called somewhere else in pgm flow.
		Population tmpPop = PopulationUtils.createPopulation(config,network);
		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Person person = PopulationUtils.getFactory().createPerson(entry.getKey());
			Plan plan = entry.getValue();
			person.addPlan(plan);
			tmpPop.addPerson(person);
		}
		new PopulationWriter(tmpPop, null).write(iterationFilename);
		// I removed the "V5" here in the assumption that it is better to move along with future format changes.  If this is
		// undesired, please change back but could you then please also add a comment why you prefer this.  Thanks.
		// kai, jan'16
	}
	@Override
	public final void finishIteration() {
		// I separated this from "writeExperiencedPlans" so that it can be called separately even when nothing is written.  Can't say
		// if the design might be better served by an iteration ends listener.  kai, feb'17
		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Plan plan = entry.getValue();
			if (scoringFunctionsForPopulation != null) {
				plan.setScore(scoringFunctionsForPopulation.getScoringFunctionForAgent(entry.getKey()).getScore());
				if (plan.getScore().isNaN()) {
					log.warn("score is NaN; plan:" + plan.toString());
				}
			}
		}
	}

	@Override
	public IdMap<Person, Plan> getExperiencedPlans() {
		return this.agentRecords;
	}

}
