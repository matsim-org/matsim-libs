/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScoreNoFilter.java
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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.ActivityEndEvent;
import org.matsim.core.api.experimental.events.ActivityStartEvent;
import org.matsim.core.api.experimental.events.AgentArrivalEvent;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.AgentStuckEvent;
import org.matsim.core.api.experimental.events.Event;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.LinkLeaveEvent;
import org.matsim.core.api.experimental.events.TravelledEvent;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.events.handler.BasicEventHandler;

// quite dirty: just to try out alternative version
public class EventsToScoreNoFilter implements BasicEventHandler {
	private EventsToActivities eventsToActivities;
	private EventsToLegs eventsToLegs;
	private ScoringFunctionsForPopulation scoringFunctionsForPopulation;
	// XXX added here to keep modifications minimal, but should be in ScoringFunctionsForPopulation
	private final Map<Id, ScoringFunction> functionsMap = new HashMap<Id, ScoringFunction>();
	private Scenario scenario;
	private ScoringFunctionFactory scoringFunctionFactory;
	private double learningRate;
	private boolean finished = false;
	
	private int iteration = -1 ;

	private double scoreSum = 0.0;
	private long scoreCount = 0;
	private Integer scoreMSAstartsAtIteration;
	

	/**
	 * Initializes EventsToScore with a learningRate of 1.0.
	 *
	 * @param scenario
	 * @param factory
	 */
	public EventsToScoreNoFilter(final Scenario scenario, final ScoringFunctionFactory factory) {
		this(scenario, factory, 1.0);
	}

	public EventsToScoreNoFilter(final Scenario scenario, final ScoringFunctionFactory scoringFunctionFactory, final double learningRate) {
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.learningRate = learningRate;
		initHandlers(scoringFunctionFactory);
		
		String str = this.scenario.getConfig().vspExperimental().getValue(VspExperimentalConfigKey.scoreMSAStartsAtIteration) ;
		if ( str.equals("null") ) {
			this.scoreMSAstartsAtIteration = null ;
		} else {
			this.scoreMSAstartsAtIteration = Integer.valueOf(str) ;
		}
	}

	private void initHandlers(final ScoringFunctionFactory factory) {
		this.eventsToActivities = new EventsToActivities();
		this.scoringFunctionsForPopulation = new ScoringFunctionsForPopulation(scenario, factory);
		// XXX changed, but belongs to scoringFunctionForPopulation
		functionsMap.clear();
		for ( Id id : scenario.getPopulation().getPersons().keySet() ) {
			functionsMap.put( id , scoringFunctionsForPopulation.getScoringFunctionForAgent( id ) );
		}
		this.eventsToActivities.setActivityHandler(this.scoringFunctionsForPopulation);
		this.eventsToLegs = new EventsToLegs();
		this.eventsToLegs.setLegHandler(this.scoringFunctionsForPopulation);
	}

	// XXX changed
	@Override
	public void handleEvent(Event event) {
		// this is for the activity and leg related stuff ("old" scoring function)
		if ( event instanceof LinkEnterEvent ) {
			eventsToLegs.handleEvent((LinkEnterEvent) event) ;
		} else if ( event instanceof LinkLeaveEvent ) {
			eventsToLegs.handleEvent((LinkLeaveEvent) event ) ;
		} else if ( event instanceof AgentDepartureEvent ) {
			eventsToLegs.handleEvent((AgentDepartureEvent) event) ;
		} else if ( event instanceof AgentArrivalEvent ) {
			eventsToLegs.handleEvent((AgentArrivalEvent) event ) ;
		} else if ( event instanceof ActivityStartEvent ) {
			eventsToActivities.handleEvent((ActivityStartEvent) event) ;
		} else if ( event instanceof ActivityEndEvent ) {
			eventsToActivities.handleEvent( (ActivityEndEvent) event ) ;
		} else if ( event instanceof TravelledEvent ) {
			eventsToLegs.handleEvent( (TravelledEvent) event ) ;
		} 

		for ( ScoringFunction sf : functionsMap.values() ) {
			if (sf != null) {
				if ( event instanceof AgentStuckEvent ) {
					sf.agentStuck( event.getTime() ) ;
				} else if ( event instanceof AgentMoneyEvent ) {
					sf.addMoney( ((AgentMoneyEvent)event).getAmount() ) ;
				} else {
					sf.handleEvent( event ) ;
				}
			}
		}
	}

	/**
	 * Finishes the calculation of the plans' scores and assigns the new scores
	 * to the plans.
	 * I think this should be split into two methods: One can want to close the ScoringFunctions to look
	 * at scores WITHOUT wanting something to be written into Plans.
	 * Actually, I think the two belong in different classes. michaz '12
	 * <p/>
	 * yy Absolutely.  kai, oct'12
	 */
	public void finish() {
		eventsToActivities.finish();	
		scoringFunctionsForPopulation.finishScoringFunctions();
		assignNewScores();
		finished = true;
	}
	
	private void assignNewScores() {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ScoringFunction sf = scoringFunctionsForPopulation.getScoringFunctionForAgent(person.getId());
			double score = sf.getScore();
			Plan plan = person.getSelectedPlan();
			Double oldScore = plan.getScore();
			if (oldScore == null) {
				plan.setScore(score);
				if ( plan.getScore().isNaN() ) {
					Logger.getLogger(this.getClass()).warn("score is NaN; plan:" + plan.toString() );
				}
			} else {
				if ( this.scoreMSAstartsAtIteration == null || this.iteration < this.scoreMSAstartsAtIteration ) {
					plan.setScore(this.learningRate * score + (1 - this.learningRate) * oldScore);
					if ( plan.getScore().isNaN() ) {
						Logger.getLogger(this.getClass()).warn("score is NaN; plan:" + plan.toString() );
					}
				} else {
					double alpha = 1./(this.iteration - this.scoreMSAstartsAtIteration + 1) ;
					plan.setScore( alpha * score + (1.-alpha) * oldScore ) ;
					if ( plan.getScore().isNaN() ) {
						Logger.getLogger(this.getClass()).warn("score is NaN; plan:" + plan.toString() );
					}
					// the above is some variant of MSA (method of successive
					// averages). It is not the same as MSA since
					// a plan is typically not scored in every iteration.
					// However, plans are called with rates, for example
					// only every 10th iteration. Yet, something like 1/(10x)
					// still diverges in the same way as 1/x
					// when integrated, so MSA should still converge to the
					// correct result. kai, oct'12
					// yyyy this has never been tested with scenarios :-(  .  At least there is a test case.  kai, oct'12
					// (In the meantime, I have used it in certain of my own 1% runs, e.g. Ivory Coast.)  
				}
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
		else
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
		if (!finished) {
			throw new IllegalStateException("Must call finish first.");
		}
		ScoringFunction scoringFunction = getScoringFunctionForAgent(agentId);
		if (scoringFunction == null)
			return null;
		return scoringFunction.getScore();
	}

	@Override
	public void reset(final int it) {
		this.eventsToActivities.reset(it);
		this.eventsToLegs.reset(it);
		initHandlers(scoringFunctionFactory);
		finished = false;
		this.iteration = it ;
		// ("reset" is called just before the mobsim starts, so it probably has the correct iteration number for our purposes) 
	}

	public ScoringFunction getScoringFunctionForAgent(Id agentId) {
		return scoringFunctionsForPopulation.getScoringFunctionForAgent(agentId);
	}

	public Map<Id, Plan> getAgentRecords() {
		return scoringFunctionsForPopulation.getAgentRecords();
	}

	public void writeExperiencedPlans(String iterationFilename) {
		scoringFunctionsForPopulation.writeExperiencedPlans(iterationFilename);
	}

}
