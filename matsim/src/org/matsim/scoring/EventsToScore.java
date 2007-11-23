/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScore.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.scoring;

import java.util.TreeMap;

import org.matsim.basic.v01.BasicPlan;
import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventAgentDeparture;
import org.matsim.events.EventAgentStuck;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerAgentDepartureI;
import org.matsim.events.handler.EventHandlerAgentStuckI;
import org.matsim.plans.Plans;
import org.matsim.utils.identifiers.IdI;

/**
 * Calculates continuously the score of the selected plans of a given population
 * based on events.<br>
 * Departure- and Arrival-Events *must* be provided to calculate the score,
 * AgentStuck-Events are used if available to add a penalty to the score.
 * The final score are written to the selected plans of each person in the
 * population.
 *
 * @author mrieser
 */
public class EventsToScore implements EventHandlerAgentArrivalI,
		EventHandlerAgentDepartureI, EventHandlerAgentStuckI {

	private Plans population = null;
	private ScoringFunctionFactory sfFactory = null;
	private final TreeMap<String, ScoringFunction> agentScorers = new TreeMap<String, ScoringFunction>();
	private double scoreSum = 0.0;
	private long scoreCount = 0;

	public EventsToScore(final Plans population, final ScoringFunctionFactory factory) {
		super();
		this.population = population;
		this.sfFactory = factory;
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.events.handler.EventHandlerAgentDepartureI#handleEvent(org.matsim.demandmodeling.events.EventAgentDeparture)
	 */
	public void handleEvent(final EventAgentDeparture event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.startLeg(event.time, event.leg);
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.events.handler.EventHandlerAgentArrivalI#handleEvent(org.matsim.demandmodeling.events.EventAgentArrival)
	 */
	public void handleEvent(final EventAgentArrival event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.endLeg(event.time);
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.events.handler.EventHandlerAgentStuckI#handleEvent(org.matsim.demandmodeling.events.EventAgentStuck)
	 */
	public void handleEvent(final EventAgentStuck event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.agentId);
		sf.agentStuck(event.time);
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {
		for (ScoringFunction sf : this.agentScorers.values()) {
			sf.finish();
			this.scoreSum += sf.getScore();
			this.scoreCount++;
		}
	}

	/**
	 * Returns the actual average plans' score before it was assigned to the plan
	 * and possibility mixed with old scores (learningrate).
	 *
	 * @return the average score of the plans before mixing with the old scores (learningrate)
	 */
	public double getAveragePlanPerformance() {
		if (this.scoreSum == 0) return BasicPlan.UNDEF_SCORE;
		return (this.scoreSum / this.scoreCount);
	}

	/**
	 * Returns the score of a single agent. This method only returns useful values
	 * if the method {@link #finish() } was called before.
	 * description
	 *
	 * @param agentId The id of the agent the score is requested for.
	 * @return The score of the specified agent.
	 */
	public double getAgentScore(final IdI agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId.toString());
		if (sf == null) return BasicPlan.UNDEF_SCORE;
		return sf.getScore();
	}

	/* (non-Javadoc)
	 * @see org.matsim.demandmodeling.events.handler.EventHandlerI#reset(int)
	 */
	public void reset(final int iteration) {
		this.agentScorers.clear();
		this.scoreCount = 0;
		this.scoreSum = 0.0;
	}

	/**
	 * Returns the scoring function for the specified agent. If the agent already
	 * has a scoring function, that one is returned. If the agent does not yet
	 * have a scoring function, a new one is created and assigned to the agent
	 * and returned.
	 *
	 * @param agentId The id of the agent the scoring function is requested for.
	 * @return The scoring function for the specified agent.
	 */
	private ScoringFunction getScoringFunctionForAgent(final String agentId) {
		ScoringFunction sf = this.agentScorers.get(agentId);
		if (sf == null) {
			sf = this.sfFactory.getNewScoringFunction(this.population.getPerson(agentId).getSelectedPlan());
			this.agentScorers.put(agentId, sf);
		}
		return sf;
	}

}
