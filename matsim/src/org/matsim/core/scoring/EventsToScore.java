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
import org.matsim.api.basic.v01.events.BasicActivityEndEvent;
import org.matsim.api.basic.v01.events.BasicAgentArrivalEvent;
import org.matsim.api.basic.v01.events.BasicAgentMoneyEvent;
import org.matsim.api.basic.v01.events.BasicAgentStuckEvent;
import org.matsim.api.basic.v01.events.handler.BasicActivityEndEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentArrivalEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentMoneyEventHandler;
import org.matsim.api.basic.v01.events.handler.BasicAgentStuckEventHandler;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.events.ActivityStartEventImpl;
import org.matsim.core.events.AgentDepartureEventImpl;
import org.matsim.core.events.handler.ActivityStartEventHandler;
import org.matsim.core.events.handler.AgentDepartureEventHandler;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;

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
public class EventsToScore implements BasicAgentArrivalEventHandler, AgentDepartureEventHandler, BasicAgentStuckEventHandler,
		BasicAgentMoneyEventHandler, ActivityStartEventHandler, BasicActivityEndEventHandler {

	private PopulationImpl population = null;
	private ScoringFunctionFactory sfFactory = null;
	private final TreeMap<Id, ScoringFunction> agentScorers = new TreeMap<Id, ScoringFunction>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private final double learningRate;

	public EventsToScore(final PopulationImpl population, final ScoringFunctionFactory factory) {
		this(population, factory, Gbl.getConfig().charyparNagelScoring().getLearningRate());
	}

	public EventsToScore(final PopulationImpl population, final ScoringFunctionFactory factory, final double learningRate) {
		super();
		this.population = population;
		this.sfFactory = factory;
		this.learningRate = learningRate;
	}

	public void handleEvent(final AgentDepartureEventImpl event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.startLeg(event.getTime(), event.getLeg());
		}
	}

	public void handleEvent(final BasicAgentArrivalEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.endLeg(event.getTime());
		}
	}

	public void handleEvent(final BasicAgentStuckEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.agentStuck(event.getTime());
		}
	}

	public void handleEvent(final BasicAgentMoneyEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.addMoney(event.getAmount());
		}
	}

	public void handleEvent(final ActivityStartEventImpl event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.startActivity(event.getTime(), event.getAct());
		}
	}

	public void handleEvent(final BasicActivityEndEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.endActivity(event.getTime());
		}
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
			PersonImpl person = this.population.getPersons().get(agentId);
			if (person == null) {
				return null;
			}
			sf = this.sfFactory.getNewScoringFunction(person.getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}

}
