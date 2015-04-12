/* *********************************************************************** *
 * project: org.matsim.*
 * OnlyTimeDependentScoringFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring.functions;

import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.scoring.ScoringFunctionAdapter;

/**
 * A Scoring Function that only respects the travel time.
 * 
 * @author cdobler
 */
public class OnlyTravelTimeDependentScoringFunction extends ScoringFunctionAdapter {
	
	protected double score;
	protected double startTime;
	
	public OnlyTravelTimeDependentScoringFunction() {
	}
	
	/**
	 * Tells the scoring function that the agent begins with an activity.
	 *
	 * @param time The time at which the mentioned activity starts.
	 * @param act The activity the agent starts. Can be used to get the activity
	 * type, exact location, facility, opening times and other information.
	 */
	@Override
	public void startActivity(final double time, final Activity activity) {
	}

    /**
	 * Tells the scoring function that the agents starts a new leg.
	 *
	 * @param time The time at which the agent starts the new leg.
	 * @param leg The leg the agent starts. Can be used to get leg mode and other
	 * information about the leg.
	 */
	@Override
	public void startLeg(double time, Leg leg) {
		startTime = time;
	}

	/**
	 * Tells the scoring function that the current leg ends.
	 * 
	 * Score is just the sum of all traveltimes. 
	 * 
	 * @param time The time at which the current leg ends.
	 */
	@Override
	public void endLeg(double time) {
		score = score - (time - startTime);
		startTime = Double.NaN;
	}

	/**
	 * Tells the scoring function that the agent got stuck in the simulation and
	 * is removed from the simulation. This should usually lead to a high penalty
	 * in the score, as the agent was not able to perform its plan as wanted.
	 * An agent can get stuck while performing an activity or while driving.
	 *
	 * @param time The time at which the agent got stuck and was removed from the
	 * simulation.
	 */
	@Override
	public void agentStuck(final double time) {
	}

	/**
	 * Adds the specified amount of utility to the agent's score. This is mostly
	 * used for handling {@link PersonMoneyEvent}s, allowing other parts of the
	 * code to influence an agent's score.
	 *
	 * @param amount amount to be added to the agent's score
	 */
	@Override
	public void addMoney(final double amount) {
	}

	/**
	 * Tells the scoring function that no more information will be given to it
	 * and that the final score should be calculated.  But the score must <b>not</b>
	 * be written to the plan!
	 */
	@Override
	public void finish() {
		
	}

	/**
	 * Returns the score for this plan.

	 * @return the score
	 */
	@Override
	public double getScore() {
		return score;
	}

	public void reset() {		
		score = 0.0;
	}

	@Override
	public void endActivity(double time, Activity activity) {
		
	}

	@Override
	public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		
	}
	
}