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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
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
import org.matsim.core.gbl.Gbl;
import org.matsim.core.utils.collections.Tuple;

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
	private final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
	private final TreeMap<Id, Integer> agentPlanElementIndex = new TreeMap<Id, Integer>();
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

	private int increaseAgentPlanElementIndex(final Id personId) {
		Integer index = this.agentPlanElementIndex.get(personId);
		if (index == null) {
			this.agentPlanElementIndex.put(personId, Integer.valueOf(1));
			return 1;
		}
		this.agentPlanElementIndex.put(personId, Integer.valueOf(1 + index.intValue()));
		return 1 + index.intValue();
	}
	
	public void handleEvent(final AgentDepartureEvent event) {
		Tuple<Plan, ScoringFunction> data = getScoringDataForAgent(event.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startLeg(event.getTime(), (Leg) data.getFirst().getPlanElements().get(index));
		}
	}

	public void handleEvent(final AgentArrivalEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.endLeg(event.getTime());
		}
	}

	public void handleEvent(final AgentStuckEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.agentStuck(event.getTime());
		}
	}

	public void handleEvent(final AgentMoneyEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.addMoney(event.getAmount());
		}
	}

	public void handleEvent(final ActivityStartEvent event) {
		Tuple<Plan, ScoringFunction> data = getScoringDataForAgent(event.getPersonId());
		if (data != null) {
			int index = increaseAgentPlanElementIndex(event.getPersonId());
			data.getSecond().startActivity(event.getTime(), (Activity) data.getFirst().getPlanElements().get(index));
		}
	}

	public void handleEvent(final ActivityEndEvent event) {
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
		for (Map.Entry<Id, Tuple<Plan, ScoringFunction>> entry : this.agentScorers.entrySet()) {
			Plan plan = entry.getValue().getFirst();
			ScoringFunction sf = entry.getValue().getSecond();
			sf.finish();
			double score = sf.getScore();
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
		Tuple<Plan, ScoringFunction> data = this.agentScorers.get(agentId);
		if (data == null)
			return null;
		return data.getSecond().getScore();
	}

	public void reset(final int iteration) {
		this.agentScorers.clear();
		this.agentPlanElementIndex.clear();
		this.scoreCount = 0;
		this.scoreSum = 0.0;
	}

	private Tuple<Plan, ScoringFunction> getScoringDataForAgent(final Id agentId) {
		Tuple<Plan, ScoringFunction> data = this.agentScorers.get(agentId);
		if (data == null) {
			Person person = this.population.getPersons().get(agentId);
			if (person == null) {
				return null;
			}
			data = new Tuple<Plan, ScoringFunction>(person.getSelectedPlan(), this.sfFactory.getNewScoringFunction(person.getSelectedPlan()));
			this.agentScorers.put(agentId, data);
		}
		return data;
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
		Tuple<Plan, ScoringFunction> data = this.getScoringDataForAgent(agentId);
		if (data == null) {
			return null;
		}
		return data.getSecond();
	}

}
