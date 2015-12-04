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

import gnu.trove.TDoubleCollection;
import gnu.trove.iterator.TDoubleIterator;
import gnu.trove.list.array.TDoubleArrayList;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.io.IOUtils;

/**
 * This class helps EventsToScore by keeping ScoringFunctions for the entire Population - one per Person -, and dispatching Activities
 * and Legs to the ScoringFunctions. It also gives out the ScoringFunctions, so they can be given other events by EventsToScore.
 * It is not independently useful. Please do not make public.
 * 
 * @author michaz
 *
 */
class ScoringFunctionsForPopulation implements ActivityHandler, LegHandler {

	private final static Logger log = Logger.getLogger(ScoringFunctionsForPopulation.class);
	
	private ScoringFunctionFactory scoringFunctionFactory = null;

	/*
	 * Replaced TreeMaps with (Linked)HashMaps since they should perform much better. For 'partialScores'
	 * a LinkedHashMap is used to ensure that agents are written in a deterministic order to the output files.
	 * 
	 * Replaced List with TDoubleCollection (TDoubleArrayList) in the partialScores map. This collection allows
	 * storing primitive objects, i.e. its double entries don't have to be wrapped into Double objects which
	 * should be faster and reduce the memory overhead.
	 * 
	 * cdobler, nov'15  
	 */
	private final Map<Id<Person>, ScoringFunction> agentScorers = new HashMap<>();
	private final Map<Id<Person>, Plan> agentRecords = new HashMap<>();
	private final Map<Id<Person>, TDoubleCollection> partialScores = new LinkedHashMap<>();

	private Scenario scenario;

	public ScoringFunctionsForPopulation(Scenario scenario, ScoringFunctionFactory scoringFunctionFactory) {
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scenario = scenario;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ScoringFunction data = this.scoringFunctionFactory.createNewScoringFunction(person);
			this.agentScorers.put(person.getId(), data);
			this.agentRecords.put(person.getId(), new PlanImpl());
			this.partialScores.put(person.getId(), new TDoubleArrayList());
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
	public ScoringFunction getScoringFunctionForAgent(final Id<Person> agentId) {
		return this.agentScorers.get(agentId);
	}

	public Map<Id<Person>, Plan> getAgentRecords() {
		return this.agentRecords;
	}

	@Override
	public void handleActivity(Id<Person> agentId, Activity activity) {
		ScoringFunction scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
		if (scoringFunctionForAgent != null) {
			scoringFunctionForAgent.handleActivity(activity);
			this.agentRecords.get(agentId).addActivity(activity);
			TDoubleCollection partialScoresForAgent = this.partialScores.get(agentId);
			partialScoresForAgent.add( scoringFunctionForAgent.getScore());
		}
	}

	@Override
	public void handleLeg(Id<Person> agentId, Leg leg) {
		ScoringFunction scoringFunctionForAgent = this.getScoringFunctionForAgent(agentId);
		if (scoringFunctionForAgent != null) {
			scoringFunctionForAgent.handleLeg(leg);
			agentRecords.get(agentId).addLeg(leg);
			TDoubleCollection partialScoresForAgent = this.partialScores.get(agentId);
			partialScoresForAgent.add(scoringFunctionForAgent.getScore());
		}
	}

	public void finishScoringFunctions() {
		for (ScoringFunction sf : this.agentScorers.values()) {
			sf.finish();
		}
		for (Entry<Id<Person>, TDoubleCollection> entry : this.partialScores.entrySet()) {
			entry.getValue().add(this.getScoringFunctionForAgent(entry.getKey()).getScore());
		}
	}

	public void writeExperiencedPlans(String iterationFilename) {
		Population population = PopulationUtils.createPopulation(scenario.getConfig());
		for (Entry<Id<Person>, Plan> entry : this.agentRecords.entrySet()) {
			Person person = PopulationUtils.createPerson(entry.getKey());
			Plan plan = entry.getValue();
			plan.setScore(getScoringFunctionForAgent(person.getId()).getScore());
			person.addPlan(plan);
			population.addPerson(person);
			if (plan.getScore().isNaN()) {
				log.warn("score is NaN; plan:" + plan.toString());
			}
		}
		new PopulationWriter(population, scenario.getNetwork()).writeV5(iterationFilename + ".xml.gz");

		BufferedWriter out = IOUtils.getBufferedWriter(iterationFilename + "_scores.xml.gz");
		try {
			for (Entry<Id<Person>, TDoubleCollection> entry : this.partialScores.entrySet()) {
				out.write(entry.getKey().toString());
				TDoubleIterator iterator = entry.getValue().iterator();
				while (iterator.hasNext()) {
					out.write('\t' + String.valueOf(iterator.next()));					
				}
				out.newLine();
			}
			out.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}