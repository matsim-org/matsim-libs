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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
import org.matsim.core.events.TravelledEvent;
import org.matsim.core.events.TravelEventHandler;

/**
 * Calculates the score of the selected plans of a given scenario
 * based on events.<br>
 * Departure- and Arrival-Events *must* be provided to calculate the score,
 * AgentStuck-Events are used if available to add a penalty to the score. The
 * final score are written to the selected plans of each person in the
 * scenario.
 * 
 * If you want your own way to reproduce plan elements from Events to get scored,
 * build your own class like this.
 *
 * @author mrieser, michaz
 */
public class EventsToScore implements AgentArrivalEventHandler, AgentDepartureEventHandler, AgentStuckEventHandler,
AgentMoneyEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, LinkLeaveEventHandler,
LinkEnterEventHandler, TravelEventHandler {

	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	private PlanElementsToScore handler;
	private Scenario scenario;
	private ScoringFunctionFactory scoringFunctionFactory;
	private double learningRate;
	private boolean finished = false;

	/**
	 * Initializes EventsToScore with a learningRate of 1.0.
	 *
	 * @param scenario
	 * @param factory
	 */
	public EventsToScore(final Scenario scenario, final ScoringFunctionFactory factory) {
		this(scenario, factory, 1.0);
	}

	public EventsToScore(final Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory, final double learningRate) {
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.learningRate = learningRate;
		initHandlers(scenario, scoringFunctionFactory, learningRate);
	}

	private void initHandlers(final Scenario scenario,
			final ScoringFunctionFactory factory, final double learningRate) {
		this.eventsToActivities = new EventsToActivities();
		this.handler = new PlanElementsToScore(scenario, factory, learningRate);
		this.eventsToActivities.setActivityHandler(this.handler);
		this.eventsToLegs = new EventsToLegs();
		this.eventsToLegs.setLegHandler(this.handler);
	}

	@Override
	public void handleEvent(final AgentDepartureEvent event) {
		eventsToLegs.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		eventsToLegs.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		eventsToLegs.handleEvent(event);
	}

	@Override
	public void handleEvent(final AgentArrivalEvent event) {
		eventsToLegs.handleEvent(event);
	}

	@Override
	public void handleEvent(final AgentStuckEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.agentStuck(event.getTime());
		}
	}

	@Override
	public void handleEvent(final AgentMoneyEvent event) {
		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
		if (sf != null) {
			sf.addMoney(event.getAmount());
		}
	}

	@Override
	public void handleEvent(final ActivityStartEvent event) {
		eventsToActivities.handleEvent(event);
	}

	@Override
	public void handleEvent(final ActivityEndEvent event) {
		eventsToActivities.handleEvent(event);
	}

	@Override
	public void handleEvent(TravelledEvent travelEvent) {
		eventsToLegs.handleEvent(travelEvent);
	}


	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {
		eventsToActivities.finish();
		handler.finish();
		finished = true;
	}

	/**
	 * Returns the actual average plans' score before it was assigned to the
	 * plan and possibility mixed with old scores (learningrate).
	 *
	 * @return the average score of the plans before mixing with the old scores
	 *         (learningrate)
	 */
	public double getAveragePlanPerformance() {
		if (!finished) {
			throw new IllegalStateException("Must call finish first.");
		}
		return handler.getAveragePlanPerformance();
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
		if (!finished) {
			throw new IllegalStateException("Must call finish first.");
		}
		ScoringFunction scoringFunction = getScoringFunctionForAgent(agentId);
		if (scoringFunction == null)
			return null;
		return scoringFunction.getScore();
	}

	@Override
	public void reset(final int iteration) {
		this.eventsToActivities.reset(iteration);
		this.eventsToLegs.reset(iteration);
		initHandlers(scenario, scoringFunctionFactory, learningRate);
		finished = false;
	}

	public ScoringFunction getScoringFunctionForAgent(Id agentId) {
		return handler.getScoringFunctionForAgent(agentId);
	}

}
