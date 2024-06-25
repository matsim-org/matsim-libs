/* *********************************************************************** *
 * project: org.matsim.*
 * ScoringFunction.java
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

package org.matsim.core.scoring;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.router.TripStructureUtils;

/**
 * A scoring function calculates the score for one plan of an agent.  The score
 * usually depends on how much time an agent is traveling and how much time an
 * agent spends at an activity.  Thus the scoring function gets informed when
 * activities start and end as well as when legs start and end.<br>
 * Note that one ScoringFunction calculates the score for exactly one agent.
 * Thus every agents must have its own instance of a scoring function!
 * <p></p>
 * See {@link tutorial.programming.example16customscoring.RunCustomScoringExample} for an example.
 *
 * @author mrieser
 */
public interface ScoringFunction {

	/**
	 * Tells the scoring function about an Activity. The Activity which
	 * the agent is in when the simulation starts will have a startTime
	 * of Time.getUndefinedTime(). The Activity which the agent is in when
	 * the simulation ends will have an endTime of Time.getUndefinedTime().
	 * It is up to the implementation what to make of this,
	 * especially to "wrap" it "around".
	 * @param activity
	 */
    void handleActivity(Activity activity);

    /**
     * Tells the scoring function about a Leg. Will contain complete route
     * information for network routes (as you would expect in a Plan), but
     * only a GenericRoute for everything else, especially transit.
     */
    void handleLeg(Leg leg);

	/**
	 * Tells the scoring function that the agent got stuck in the simulation and
	 * is removed from the simulation. This should usually lead to a high penalty
	 * in the score, as the agent was not able to perform its plan as wanted.
	 * An agent can get stuck while performing an activity or while driving.
	 *
	 * @param time The time at which the agent got stuck and was removed from the
	 * simulation.
	 */
	void agentStuck(final double time);

	/**
	 * Adds the specified amount of utility to the agent's score. This is mostly
	 * used for handling {@link PersonMoneyEvent}s, allowing other parts of the
	 * code to influence an agent's score.
	 *
	 * @param amount amount to be added to the agent's score
	 */
	void addMoney(final double amount);

	/**
	 * Adds the specified amount of utility to the agent's score. This is mostly
	 * used for handling {@link PersonScoreEvent}s, allowing other parts of the
	 * code to influence an agent's score.
	 *
	 * @param amount amount to be added to the agent's score
	 */
	void addScore(final double amount);

	/**
	 * Tells the scoring function that no more information will be given to it
	 * and that the final score should be calculated.  But the score must <b>not</b>
	 * be written to the plan!
	 */
	void finish();

	/**
	 * Returns the score for this plan.

	 * @return the score
	 */
	double getScore();

	void handleEvent( Event event ) ;

	default void handleTrip( TripStructureUtils.Trip trip ) {
		// empty default implementation, since older implementations of the interface
		// don't have this method, and work happily without. kai, sep'18
	}

	/**
	 * Write detailed score explanation into the output {@code out}. Multiple value should be separated with {@link #SCORE_DELIMITER}.
	 */
	default void explainScore(StringBuilder out) {
	}

	/**
	 * Delimiter used to separate scores in explanation string.
	 */
	String SCORE_DELIMITER = ";";
	/**
	 * The attribute that will be used for the score explanation.
	 */
	String SCORE_EXPLANATION_ATTR = "scoreExplanation";

}
