/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.events.ActivityEndEvent;
import org.matsim.core.events.ActivityStartEvent;
import org.matsim.core.events.AgentArrivalEvent;
import org.matsim.core.events.AgentDepartureEvent;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.AgentStuckEvent;
import org.matsim.core.events.handler.ActivityEndEventHandler;
import org.matsim.core.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.handler.AgentArrivalEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.events.handler.AgentMoneyEventHandler;
import org.matsim.core.events.handler.AgentStuckEventHandler;
import org.matsim.core.gbl.Gbl;

/**
 * Calculates continuously the score of the selected plans of a given population
 * based on events.<br>
 * Departure- and Arrival-Events *must* be provided to calculate the score,
 * AgentStuck-Events are used if available to add a penalty to the score. The
 * final score are written to the selected plans of each person in the
 * population.
 * 
 * @author mrieser
 */
public class EventsToScore implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler,
		AgentMoneyEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {

	private Population population = null;
	private ScoringFunctionFactory sfFactory = null;
	private final TreeMap<Id, ScoringFunction> agentScorers = new TreeMap<Id, ScoringFunction>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private final double learningRate;

	public EventsToScore(final Population population, final ScoringFunctionFactory factory) {
		this(population, factory, Gbl.getConfig().charyparNagelScoring().getLearningRate());
	}

	public EventsToScore(final Population population, final ScoringFunctionFactory factory, final double learningRate) {
		super();
		this.population = population;
		this.sfFactory = factory;
		this.learningRate = learningRate;
	}

	public void handleEvent(final AgentDepartureEvent event) {
		getScoringFunctionForAgent(event.getPersonId()).startLeg(event.getTime(), event.getLeg());
	}

	public void handleEvent(final AgentArrivalEvent event) {
		getScoringFunctionForAgent(event.getPersonId()).endLeg(event.getTime());
	}

	public void handleEvent(final AgentStuckEvent event) {
		getScoringFunctionForAgent(event.getPersonId()).agentStuck(event.getTime());
	}

	public void handleEvent(final AgentMoneyEvent event) {
		getScoringFunctionForAgent(event.getPersonId()).addMoney(event.getAmount());
	}

	public void handleEvent(ActivityStartEvent event) {
		getScoringFunctionForAgent(event.getPersonId()).startActivity(event.getTime(), event.getAct());
	}

	public void handleEvent(ActivityEndEvent event) {
		getScoringFunctionForAgent(event.getPersonId()).endActivity(event.getTime());
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {
		for (Map.Entry<Id, ScoringFunction> entry : this.agentScorers.entrySet()) {
			Id agentId = entry.getKey();
			ScoringFunction sf = entry.getValue();
			sf.finish();
			double score = sf.getScore();
			Plan plan = this.population.getPersons().get(agentId).getSelectedPlan();
			Double oldScore = plan.getScore();
			if (oldScore == null) {
				plan.setScore(score);
			} else {
				plan.setScore(this.learningRate * score + (1 - this.learningRate) * oldScore);
			}

			this.scoreSum += score;
			this.scoreCount++;
		}
	}

	/**
	 * Returns the actual average plans' score before it was assigned to the
	 * plan and possibility mixed with old scores (learningrate).
	 * 
	 * @return the average score of the plans before mixing with the old scores
	 *         (learningrate)
	 */
	public double getAveragePlanPerformance() {
		if (this.scoreSum == 0)
			return Double.NaN;
		return (this.scoreSum / this.scoreCount);
	}

	/**
	 * Returns the score of a single agent. This method only returns useful
	 * values if the method {@link #finish() } was called before. description
	 * 
	 * @param agentId
	 *            The id of the agent the score is requested for.
	 * @return The score of the specified agent.
	 */
	public Double getAgentScore(final Id agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId);
		if (sf == null)
			return null;
		return sf.getScore();
	}

	public void reset(final int iteration) {
		this.agentScorers.clear();
		this.scoreCount = 0;
		this.scoreSum = 0.0;
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
	public ScoringFunction getScoringFunctionForAgent(final Id agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId);
		if (sf == null) {
			sf = this.sfFactory.getNewScoringFunction(this.population.getPersons().get(agentId).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}

}
