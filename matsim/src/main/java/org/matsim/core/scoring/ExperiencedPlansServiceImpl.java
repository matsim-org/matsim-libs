
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControllerListenerManager;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.population.PopulationUtils;

import java.util.Map;

final class ExperiencedPlansServiceImpl implements ExperiencedPlansService, EventsToLegs.LegHandler, EventsToActivities.ActivityHandler, IterationStartsListener {

	private final static Logger log = LogManager.getLogger(ExperiencedPlansServiceImpl.class);

	@Inject private Network network;
	@Inject private Config config;
	@Inject private Population population;
	@Inject(optional = true) private ScoringFunctionsForPopulation scoringFunctionsForPopulation;

	private final IdMap<Person, Plan> agentRecords = new IdMap<>(Person.class);
	private boolean hasFinished = false;

	@Inject
	ExperiencedPlansServiceImpl(ControllerListenerManager controllerListenerManager, EventsToActivities eventsToActivities, EventsToLegs eventsToLegs) {
		controllerListenerManager.addControllerListener( this );
		eventsToActivities.addActivityHandler(this);
		eventsToLegs.addLegHandler(this);
	}

	@Override public void notifyIterationStarts(IterationStartsEvent event) {
		for (Person person : population.getPersons().values()) {
			agentRecords.put(person.getId(), PopulationUtils.createPlan());
		}
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
		final Population tmpPop = getPopulationWithExperiencedPlans();
		new PopulationWriter(tmpPop, null).write(iterationFilename);
	}

	@Override
	public Population getPopulationWithExperiencedPlans(){
		if ( !hasFinished ){
			finishIteration();
		}
		Population tmpPop = PopulationUtils.createPopulation(config,network);
		log.warn( "agentsRecords.size={}", agentRecords.size());
		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Person person = PopulationUtils.getFactory().createPerson(entry.getKey());

			// copy the attributes from the original person to the experienced plans person:
			Person originalPerson = population.getPersons().get( entry.getKey() );
			for( Map.Entry<String, Object> entry2 : originalPerson.getAttributes().getAsMap().entrySet() ){
				person.getAttributes().putAttribute( entry2.getKey(),entry2.getValue() );
				// note that this is not a completely deep copy.  Should not be a problem since we only write to file, but in the
				// end we never know.  kai, oct'25
			}

			Plan plan = entry.getValue();
			plan.setScore( originalPerson.getSelectedPlan().getScore() );

			person.addPlan(plan);
			tmpPop.addPerson(person);
		}
		return tmpPop;
	}

	@Override
	public final void finishIteration() {
		// I separated this from "writeExperiencedPlans" so that it can be called separately even when nothing is written.  Can't say
		// if the design might be better served by an iteration ends listener.  kai, feb'17
//		for (Map.Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
//			Plan plan = entry.getValue();
//			if (scoringFunctionsForPopulation != null) {
//				final ScoringFunction scoringFunctionForAgent = scoringFunctionsForPopulation.getScoringFunctionForAgent( entry.getKey() );
//				// yyyy not sure why this can happen. kai, jan'26
//				if ( scoringFunctionForAgent != null ){
//					plan.setScore( scoringFunctionForAgent.getScore() );
//					if( plan.getScore().isNaN() ){
//						log.warn( "score is NaN; plan:" + plan.toString() );
//					}
//				}
//			}
//		}
		hasFinished = true;
	}

	@Override
	public IdMap<Person, Plan> getExperiencedPlans() {
		return this.agentRecords;
	}

}
