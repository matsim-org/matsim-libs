/* *********************************************************************** *
 * project: org.matsim.*
 * JointEventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtripsoptimizer.scoring;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.scoring.EventsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.utils.collections.Tuple;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.IdLeg;
import playground.thibautd.jointtripsoptimizer.population.JointActingTypes;
import playground.thibautd.jointtripsoptimizer.population.JointLeg;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;
import playground.thibautd.jointtripsoptimizer.population.PopulationOfCliques;
import playground.thibautd.jointtripsoptimizer.population.PopulationWithCliques;

/**
 * Same role as {@link org.matsim.core.scoring.EventsToScore}, but takes into account
 * the driver events to score the passengers plan.
 * That is, the score is "as if" the joint trips were "really" simulated.
 *
 * Do not score "on the fly", but accumulates information used to compute scores
 * "off line".
 *
 * method of calculus: departure and end times for shared rides correspond to the
 * lattest observed.
 * Activity start and end time not modified yet.
 *
 * @author thibautd
 */
public class JointEventsToScore implements
		AgentArrivalEventHandler,
		AgentDepartureEventHandler,
		AgentStuckEventHandler,
		AgentMoneyEventHandler,
		ActivityStartEventHandler,
		ActivityEndEventHandler {

	//internal identifiers for eventTypes
	private static String DEPARTURE = "departure";
	private static String ARRIVAL = "arrival";
	private static String ACT_START = "start";
	private static String ACT_END = "end";

	//private final EventsToScore eventsToScoreDelegate;
	private final PopulationWithCliques population;
	private final PopulationOfCliques cliques;
	private final ScoringFunctionFactory sfFactory;
	private final double learningRate;

	private final Map<Id, Tuple<Plan, ScoringFunction>> agentScorers =
		new TreeMap<Id, Tuple<Plan, ScoringFunction>>();

	private final Map<Id, List<Tuple<String, Double>>> agentTimes =
		new TreeMap<Id, List<Tuple<String, Double>>>();

	private final Map<Id, Integer> indicesInPlan =
		new TreeMap<Id, Integer>();

	private final Map<Id, Integer> indicesInEvents =
		new TreeMap<Id, Integer>();

	private final Map<IdLeg, Double> examinedLegsForDeparture =
		new TreeMap<IdLeg, Double>();

	private final Map<IdLeg, Double> examinedLegsForArrival =
		new TreeMap<IdLeg, Double>();

	/**
	 * Initializes EventsToScore with a learningRate of 1.0.
	 *
	 * @param population
	 * @param factory
	 */
	public JointEventsToScore(
			final PopulationWithCliques population,
			final ScoringFunctionFactory factory) {
		this(population, factory, 1.0);
	}

	public JointEventsToScore(
			final PopulationWithCliques population,
			final ScoringFunctionFactory factory,
			final double learningRate) {
		//this.eventsToScoreDelegate = new EventsToScore(population, factory, learningRate);
		this.population = population;
		this.cliques = population.getCliques();
		this.sfFactory = factory;
		this.learningRate = learningRate;
	}

	public void handleEvent(AgentDepartureEvent event) {
		//eventsToScoreDelegate.handleEvent(event);
		this.getAgentTimesList(event.getPersonId()).add(
				new Tuple<String, Double>(DEPARTURE, event.getTime()));
	}

	public void handleEvent(AgentArrivalEvent event) {
		//eventsToScoreDelegate.handleEvent(event);
		this.getAgentTimesList(event.getPersonId()).add(
				new Tuple<String, Double>(ARRIVAL, event.getTime()));
	}

	public void handleEvent(AgentStuckEvent event) {
		//eventsToScoreDelegate.handleEvent(event);
		ScoringFunction scoringFunction =
			this.getScoringFunctionForAgent(event.getPersonId());
		if (scoringFunction != null) {
			scoringFunction.agentStuck(event.getTime());
		}
	}

	public void handleEvent(AgentMoneyEvent event) {
		//eventsToScoreDelegate.handleEvent(event);
		ScoringFunction scoringFunction =
			this.getScoringFunctionForAgent(event.getPersonId());
		if (scoringFunction != null) {
			scoringFunction.addMoney(event.getAmount());
		}
	}

	public void handleEvent(ActivityStartEvent event) {
		//eventsToScoreDelegate.handleEvent(event);
		this.getAgentTimesList(event.getPersonId()).add(
				new Tuple<String, Double>(ACT_START, event.getTime()));
	}

	public void handleEvent(ActivityEndEvent event) {
		//eventsToScoreDelegate.handleEvent(event);
		this.getAgentTimesList(event.getPersonId()).add(
				new Tuple<String, Double>(ACT_END, event.getTime()));
	}

	public double getAveragePlanPerformance() {
		//return eventsToScoreDelegate.getAveragePlanPerformance();
		return 0d;
	}

	public double getAgentScore(Id agentId) {
		//return eventsToScoreDelegate.getAgentScore(agentId);
		return this.getScoringFunctionForAgent(agentId).getScore();
	}

	public void reset(int iteration) {
		//eventsToScoreDelegate.reset(iteration);
		this.agentScorers.clear();
		this.agentTimes.clear();
		this.examinedLegsForDeparture.clear();
		this.examinedLegsForArrival.clear();
	}

	/**
	 * Returns the scoring function for the specified agent. If the agent
	 * already has a scoring function, that one is returned. If the agent does
	 * not yet have a scoring function, a new one is created and assigned to the
	 * agent and returned.
	 *
	 * Taken from {@link org.matsim.core.scoring.EventsToScore}
	 *
	 * @param agentId
	 *            The id of the agent the scoring function is requested for.
	 * @return The scoring function for the specified agent.
	 */
	public ScoringFunction getScoringFunctionForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = this.getScoringDataForAgent(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond();
	}

	/**
	 * Taken from {@link org.matsim.core.scoring.EventsToScore}
	 */
	private Tuple<Plan, ScoringFunction> getScoringDataForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = this.agentScorers.get(agentId);
		if (data == null) {
			Person person = this.population.getPersons().get(agentId);
			if (person == null) {
				return null;
			}
			data = new Tuple<Plan, ScoringFunction>(
					person.getSelectedPlan(),
					this.sfFactory.createNewScoringFunction(person.getSelectedPlan()));
			this.agentScorers.put(agentId, data);
		}
		return data;
	}

	private List<Tuple<String, Double>> getAgentTimesList(final Id agentId) {
		if (this.agentTimes.containsKey(agentId)) {
			return agentTimes.get(agentId);
		}

		List<Tuple<String, Double>> agentTimeList =
			new ArrayList<Tuple<String, Double>>();
		agentTimes.put(agentId, agentTimeList);
		return agentTimeList;
	}

	/*
	 * =========================================================================
	 * Scoring relative functions
	 * =========================================================================
	 */

	/**
	 * Iterates over cliques and scores the corresponding individual plans.
	 */
	public void finish() {
		//eventsToScoreDelegate.finish();
		List<Id> individualsToScore;
		Map<Id, List<PlanElement>> individualPlans;
		ScoringFunction scoringFunction;
		boolean removeIndividual;
		List<Id> toRemove = new ArrayList<Id>();
		Plan selectedPlan;
		Double oldScore;

		for (Clique clique : cliques.getCliques().values()) {
			individualsToScore = new ArrayList<Id>(clique.getMembers().keySet());
			individualPlans = ((JointPlan) clique.getSelectedPlan()).getIndividualPlanElements();
			initialiseIndices(individualsToScore);

			do {
				for (Id individualId : individualsToScore) {
					removeIndividual = this.scoreNextEvent(
							individualId,
							individualsToScore,
							individualPlans);

					if (removeIndividual) {
						toRemove.add(individualId);
					}
				}
				individualsToScore.removeAll(toRemove);
				toRemove.clear();
			} while (!individualsToScore.isEmpty());

			for (Person member : clique.getMembers().values()) {
				scoringFunction = this.getScoringFunctionForAgent(member.getId());
				scoringFunction.finish();
				selectedPlan = member.getSelectedPlan();
				oldScore = selectedPlan.getScore();
				selectedPlan.setScore(oldScore == null ?
						scoringFunction.getScore() :
						this.learningRate * scoringFunction.getScore() +
						(1 - this.learningRate) * oldScore);
			}
		}
	}

	private boolean scoreNextEvent(
			Id individualId,
			List<Id> individualsToScore,
			Map<Id, List<PlanElement>> individualPlans) {
		Tuple<String, Double> currentEvent = null;

		try {
			currentEvent = 
				this.getAgentTimesList(individualId).get(this.getIndexInEvents(individualId));
		} catch (IndexOutOfBoundsException e) {
			// if no more events, we have finished with this plan
			return true;
		}

		if (currentEvent.getFirst().equals(DEPARTURE)) {
			this.scoreDeparture(individualId, currentEvent.getSecond());
		}
		else if (currentEvent.getFirst().equals(ARRIVAL)) {
			this.scoreArrival(individualId, currentEvent.getSecond());
		}
		else if (currentEvent.getFirst().equals(ACT_START)) {
			this.scoreActivityStart(individualId, currentEvent.getSecond());
		}
		else if (currentEvent.getFirst().equals(ACT_END)) {
			this.scoreActivityEnd(individualId, currentEvent.getSecond());
		}

		return false;

	}

	private void scoreDeparture(
			Id individualId,
			double time) {
		int indexInPlan = this.getIndexInPlan(individualId);
		Plan individualPlan = this.getScoringDataForAgent(individualId).getFirst();
		JointLeg leg = (JointLeg) individualPlan.getPlanElements().get(indexInPlan); 

		if (!leg.getJoint()) {
			this.getScoringFunctionForAgent(individualId).startLeg(time, leg);
			this.incrIndexInEvents(individualId);
			return;
		}

		if (!examinedLegsForDeparture.containsKey(leg.getId())) {
			this.examinedLegsForDeparture.put((IdLeg) leg.getId(), time);
		}

		if (this.examinedLegsForDeparture.keySet().containsAll(leg.getLinkedElementsIds())) {
			double correctedTime = this.getDepartureTime(leg);

			this.getScoringFunctionForAgent(individualId).startLeg(correctedTime, leg);
			this.incrIndexInEvents(individualId);
		}
	}

	private double getDepartureTime(JointLeg leg) {
		double maxTime = Double.NEGATIVE_INFINITY;
		double currentTime;

		for (IdLeg id : leg.getLinkedElementsIds()) {
			currentTime = this.examinedLegsForDeparture.get(id);
			maxTime = Math.max(currentTime, maxTime);
		}

		return maxTime;
	}

	private void scoreArrival(
			Id individualId,
			double time) {
		int indexInPlan = this.getIndexInPlan(individualId);
		Plan individualPlan = this.getScoringDataForAgent(individualId).getFirst();
		JointLeg leg = (JointLeg) individualPlan.getPlanElements().get(indexInPlan); 

		if (!leg.getJoint()) {
			this.getScoringFunctionForAgent(individualId).endLeg(time);
			this.incrIndexInPlan(individualId);
			this.incrIndexInEvents(individualId);
			return;
		}

		if (!examinedLegsForArrival.containsKey(leg.getId())) {
			this.examinedLegsForArrival.put((IdLeg) leg.getId(), time);
		}

		if (this.examinedLegsForArrival.keySet().containsAll(leg.getLinkedElementsIds())) {
			double correctedTime = this.getArrivalTime(leg);

			this.getScoringFunctionForAgent(individualId).startLeg(correctedTime, leg);
			this.incrIndexInPlan(individualId);
			this.incrIndexInEvents(individualId);
		}
	}

	/**
	 * TODO: improve (currently returns the max start time)
	 */
	private double getArrivalTime(JointLeg leg) {
		double maxTime = Double.NEGATIVE_INFINITY;
		double currentTime;

		for (IdLeg id : leg.getLinkedElementsIds()) {
			currentTime = this.examinedLegsForArrival.get(id);
			maxTime = Math.max(currentTime, maxTime);
		}

		return maxTime;
	}

	private void scoreActivityStart(
			Id individualId,
			double time) {
		int indexInPlan = this.getIndexInPlan(individualId);
		Plan individualPlan = this.getScoringDataForAgent(individualId).getFirst();
		Activity activity = (Activity) individualPlan.getPlanElements().get(indexInPlan);

		//if (!activity.getType().equals(JointActingTypes.DROP_OFF)) {
			this.getScoringFunctionForAgent(individualId).startActivity(time, activity);
			this.incrIndexInEvents(individualId);
		//}
	}

	private void scoreActivityEnd(
			Id individualId,
			double time) {
		int indexInPlan = this.getIndexInPlan(individualId);
		Plan individualPlan = this.getScoringDataForAgent(individualId).getFirst();
		Activity activity = (Activity) individualPlan.getPlanElements().get(indexInPlan);

		//if (!activity.getType().equals(JointActingTypes.PICK_UP)) {
			this.getScoringFunctionForAgent(individualId).endActivity(time);
			this.incrIndexInPlan(individualId);
			this.incrIndexInEvents(individualId);
		//}
	}

	private void initialiseIndices(List<Id> individualsToScore) {
		this.indicesInPlan.clear();
		this.indicesInEvents.clear();

		for (Id id : individualsToScore) {
			this.indicesInPlan.put(id, 0);
			this.indicesInEvents.put(id, 0);
		}

		this.examinedLegsForDeparture.clear();
		this.examinedLegsForArrival.clear();
	}

	private int getIndexInPlan(Id id) {
		return this.indicesInPlan.get(id);
	}

	private void incrIndexInPlan(Id id) {
		this.indicesInPlan.put(id, this.indicesInPlan.get(id) + 1);
	}

	private int getIndexInEvents(Id id) {
		return this.indicesInEvents.get(id);
	}

	private void incrIndexInEvents(Id id) {
		this.indicesInEvents.put(id, this.indicesInEvents.get(id) + 1);
	}

}
