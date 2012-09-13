///* *********************************************************************** *
// * project: org.matsim.*
// * EventsToScore.java
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
// *                   LICENSE and WARRANTY file.                            *
// * email           : info at matsim dot org                                *
// *                                                                         *
// * *********************************************************************** *
// *                                                                         *
// *   This program is free software; you can redistribute it and/or modify  *
// *   it under the terms of the GNU General Public License as published by  *
// *   the Free Software Foundation; either version 2 of the License, or     *
// *   (at your option) any later version.                                   *
// *   See also COPYING, LICENSE and WARRANTY file                           *
// *                                                                         *
// * *********************************************************************** */
//
//package playground.yu.scoring;
//
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.matsim.api.core.v01.Id;
//import org.matsim.api.core.v01.Scenario;
//import org.matsim.api.core.v01.population.Activity;
//import org.matsim.api.core.v01.population.Leg;
//import org.matsim.api.core.v01.population.Person;
//import org.matsim.api.core.v01.population.Plan;
//import org.matsim.core.api.experimental.events.ActivityEndEvent;
//import org.matsim.core.api.experimental.events.ActivityStartEvent;
//import org.matsim.core.api.experimental.events.AgentArrivalEvent;
//import org.matsim.core.api.experimental.events.AgentDepartureEvent;
//import org.matsim.core.api.experimental.events.AgentMoneyEvent;
//import org.matsim.core.api.experimental.events.AgentStuckEvent;
//import org.matsim.core.api.experimental.events.LinkEnterEvent;
//import org.matsim.core.api.experimental.events.LinkLeaveEvent;
//import org.matsim.core.api.experimental.events.handler.ActivityEndEventHandler;
//import org.matsim.core.api.experimental.events.handler.ActivityStartEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentArrivalEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentDepartureEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentMoneyEventHandler;
//import org.matsim.core.api.experimental.events.handler.AgentStuckEventHandler;
//import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
//import org.matsim.core.api.experimental.events.handler.LinkLeaveEventHandler;
//import org.matsim.core.events.TravelledEventHandler;
//import org.matsim.core.events.TravelledEvent;
//import org.matsim.core.scoring.EventsToActivities;
//import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
//import org.matsim.core.scoring.EventsToLegs;
//import org.matsim.core.scoring.EventsToLegs.LegHandler;
//import org.matsim.core.scoring.ScoringFunction;
//import org.matsim.core.scoring.ScoringFunctionFactory;
//import org.matsim.core.utils.collections.Tuple;
//
///**
// * a copy of {@code org.matsim.core.scoring.EventsToScore r17026}
// * 
// * @author mrieser
// */
//public class Events2Score implements AgentArrivalEventHandler,
//		AgentDepartureEventHandler, AgentStuckEventHandler,
//		AgentMoneyEventHandler, ActivityStartEventHandler,
//		ActivityEndEventHandler, LinkLeaveEventHandler, LinkEnterEventHandler,
//		TravelledEventHandler {
//
//	private Scenario scenario = null;
//	protected ScoringFunctionFactory sfFactory = null;
//	protected final TreeMap<Id, Tuple<Plan, ScoringFunction>> agentScorers = new TreeMap<Id, Tuple<Plan, ScoringFunction>>();
//	private double scoreSum = 0.0;
//	private long scoreCount = 0;
//	private final double learningRate;
//	private final EventsToActivities eventsToActivities;
//	private final EventsToLegs eventsToLegs;
//	private final DistributeToScoringFunctions handler;
//
//	private class DistributeToScoringFunctions implements ActivityHandler,
//			LegHandler {
//
//		@Override
//		public void handleActivity(Id agentId, Activity activity) {
//			ScoringFunction scoringFunctionForAgent = getScoringFunctionForAgent(agentId);
//			if (scoringFunctionForAgent != null) {
//				scoringFunctionForAgent.handleActivity(activity);
//			}
//		}
//
//		@Override
//		public void handleLeg(Id agentId, Leg leg) {
//			ScoringFunction scoringFunctionForAgent = getScoringFunctionForAgent(agentId);
//			if (scoringFunctionForAgent != null) {
//				scoringFunctionForAgent.handleLeg(leg);
//			}
//		}
//	}
//
//	/**
//	 * Initializes EventsToScore with a learningRate of 1.0.
//	 * 
//	 * @param scenario
//	 * @param factory
//	 */
//	public Events2Score(final Scenario scenario,
//			final ScoringFunctionFactory factory) {
//		this(scenario, factory, 1.0);
//	}
//
//	public Events2Score(final Scenario scenario,
//			final ScoringFunctionFactory factory, final double learningRate) {
//		super();
//		this.scenario = scenario;
//		sfFactory = factory;
//		this.learningRate = learningRate;
//		eventsToActivities = new EventsToActivities();
//		handler = new DistributeToScoringFunctions();
//		eventsToActivities.setActivityHandler(handler);
//		eventsToLegs = new EventsToLegs();
//		eventsToLegs.setLegHandler(handler);
//	}
//
//	@Override
//	public void handleEvent(final AgentDepartureEvent event) {
//		eventsToLegs.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(LinkEnterEvent event) {
//		eventsToLegs.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(LinkLeaveEvent event) {
//		eventsToLegs.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(final AgentArrivalEvent event) {
//		eventsToLegs.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(final AgentStuckEvent event) {
//		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
//		if (sf != null) {
//			sf.agentStuck(event.getTime());
//		}
//	}
//
//	@Override
//	public void handleEvent(final AgentMoneyEvent event) {
//		ScoringFunction sf = getScoringFunctionForAgent(event.getPersonId());
//		if (sf != null) {
//			sf.addMoney(event.getAmount());
//		}
//	}
//
//	@Override
//	public void handleEvent(final ActivityStartEvent event) {
//		eventsToActivities.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(final ActivityEndEvent event) {
//		eventsToActivities.handleEvent(event);
//	}
//
//	@Override
//	public void handleEvent(TravelledEvent travelEvent) {
//		eventsToLegs.handleEvent(travelEvent);
//	}
//
//	/**
//	 * Finishes the calculation of the plans' scores and assigns the new scores
//	 * to the plans.
//	 */
//	public void finish() {
//		eventsToActivities.finish();
//		for (Map.Entry<Id, Tuple<Plan, ScoringFunction>> entry : agentScorers
//				.entrySet()) {
//			Plan plan = entry.getValue().getFirst();
//			ScoringFunction sf = entry.getValue().getSecond();
//			sf.finish();
//			double score = sf.getScore();
//			Double oldScore = plan.getScore();
//			if (oldScore == null) {
//				plan.setScore(score);
//			} else {
//				plan.setScore(learningRate * score + (1 - learningRate)
//						* oldScore);
//			}
//
//			scoreSum += score;
//			scoreCount++;
//		}
//	}
//
//	/**
//	 * Returns the actual average plans' score before it was assigned to the
//	 * plan and possibility mixed with old scores (learningrate).
//	 * 
//	 * @return the average score of the plans before mixing with the old scores
//	 *         (learningrate)
//	 */
//	public double getAveragePlanPerformance() {
//		if (scoreSum == 0) {
//			return Double.NaN;
//		}
//		return scoreSum / scoreCount;
//	}
//
//	/**
//	 * Returns the score of a single agent. This method only returns useful
//	 * values if the method {@link #finish() } was called before. description
//	 * 
//	 * @param agentId
//	 *            The id of the agent the score s requested for.
//	 * @return Th score of the specified agent.
//	 */
//	public Double getAgentScore(final Id agentId) {
//		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
//		if (data == null) {
//			return null;
//		}
//		return data.getSecond().getScore();
//	}
//
//	@Override
//	public void reset(final int iteration) {
//		eventsToActivities.reset(iteration);
//		eventsToLegs.reset(iteration);
//		agentScorers.clear();
//		scoreCount = 0;
//		scoreSum = 0.0;
//	}
//
//	private Tuple<Plan, ScoringFunction> getPlanAndScoringFunctionForAgent(
//			final Id agentId) {
//		Tuple<Plan, ScoringFunction> data = agentScorers.get(agentId);
//		if (data == null) {
//			Person person = scenario.getPopulation().getPersons().get(agentId);
//			if (person == null) {
//				return null;
//			}
//			data = new Tuple<Plan, ScoringFunction>(
//					person.getSelectedPlan(),
//					sfFactory.createNewScoringFunction(person.getSelectedPlan()));
//			agentScorers.put(agentId, data);
//		}
//		return data;
//	}
//
//	/**
//	 * Returns the scoring function for the specified agent. If the agent
//	 * already has a scoring function, that one is returned. If the agent does
//	 * not yet have a scoring function, a new one is created and assigned to the
//	 * agent and returned.
//	 * 
//	 * @param agentId
//	 *            The id of the agent the scoring function is requested for.
//	 * @return The scoring function for the specified agent.
//	 */
//	public ScoringFunction getScoringFunctionForAgent(final Id agentId) {
//		Tuple<Plan, ScoringFunction> data = getPlanAndScoringFunctionForAgent(agentId);
//		if (data == null) {
//			return null;
//		}
//		return data.getSecond();
//	}
//
//}
