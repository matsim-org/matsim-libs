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

package playground.yu.scoring;

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
import org.matsim.core.events.TravelEvent;
import org.matsim.core.events.TravelEventHandler;
import org.matsim.core.scoring.EventsToActivities;
import org.matsim.core.scoring.EventsToLegs;
import org.matsim.core.scoring.PlanElementsToScore;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

/**
 * just a copy from {@code org.matsim.core.scoring.EventsToScore}
 * 
 * @author mrieser, michaz
 */
public class Events2Score implements AgentArrivalEventHandler,
		AgentDepartureEventHandler, AgentStuckEventHandler,
		AgentMoneyEventHandler, ActivityStartEventHandler,
		ActivityEndEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler,
		TravelEventHandler {

	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	private PlanElementsToScore handler;
	private final Scenario scenario;
	private final ScoringFunctionFactory scoringFunctionFactory;
	private final double learningRate;

	/**
	 * Initializes EventsToScore with a learningRate of 1.0.
	 * 
	 * @param scenario
	 * @param factory
	 */
	public Events2Score(final Scenario scenario,
			final ScoringFunctionFactory factory) {
		this(scenario, factory, 1.0);
	}

	public Events2Score(final Scenario scenario,
			final ScoringFunctionFactory scoringFunctionFactory,
			final double learningRate) {
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.learningRate = learningRate;
		initHandlers(scenario, scoringFunctionFactory, learningRate);
	}

	private void initHandlers(final Scenario scenario,
			final ScoringFunctionFactory factory, final double learningRate) {
		eventsToActivities = new EventsToActivities();
		handler = new PlanElementsToScore(scenario, factory, learningRate);
		eventsToActivities.setActivityHandler(handler);
		eventsToLegs = new EventsToLegs();
		eventsToLegs.setLegHandler(handler);
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
	public void handleEvent(TravelEvent travelEvent) {
		eventsToLegs.handleEvent(travelEvent);
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 */
	public void finish() {
		eventsToActivities.finish();
		handler.finish();
	}

	/**
	 * Returns the actual average plans' score before it was assigned to the
	 * plan and possibility mixed with old scores (learningrate).
	 * 
	 * @return the average score of the plans before mixing with the old scores
	 *         (learningrate)
	 */
	public double getAveragePlanPerformance() {
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
		ScoringFunction scoringFunction = getScoringFunctionForAgent(agentId);
		if (scoringFunction == null) {
			return null;
		}
		return scoringFunction.getScore();
	}

	@Override
	public void reset(final int iteration) {
		eventsToActivities.reset(iteration);
		eventsToLegs.reset(iteration);
		initHandlers(scenario, scoringFunctionFactory, learningRate);
	}

	public ScoringFunction getScoringFunctionForAgent(Id agentId) {
		return handler.getScoringFunctionForAgent(agentId);
	}

}
