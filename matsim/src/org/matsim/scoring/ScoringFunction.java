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

package org.matsim.scoring;

import org.matsim.events.AgentMoneyEvent;
import org.matsim.interfaces.core.v01.Act;
import org.matsim.interfaces.core.v01.Leg;

/**
 * A scoring function calculates the score for one plan of an agent.  The score
 * usually depends on how much time an agent is traveling and how much time an
 * agent spends at an activity.  Thus the scoring function gets informed when
 * activities start and end as well as when legs start and end.<br>
 * Note that one ScoringFunction calculates the score for exactly one agent.
 * Thus every agents must have its own instance of a scoring function!
 *
 * @author mrieser
 */
public interface ScoringFunction {

	/* In the case when every agent will have it's own scoring function, a
	 * method named "startPlan(Plan plan)" (or something similar) may be
	 * needed to reset the score calculation from iteration to iteration.
	 * -marcel, 21jun07
	 */

	/**
	 * Tells the scoring function that the agent begins with an activity.
	 *
	 * @param time The time at which the mentioned activity starts.
	 * @param act The activity the agent starts. Can be used to get the activity
	 * type, exact location, facility, opening times and other information.
	 */
	public void startActivity(final double time, final Act act);

	/**
	 * Tells the scoring function that the agent stops with an activity.
	 *
	 * @param time The time at which the agent stops performing the current
	 * activity.
	 */
	public void endActivity(final double time);

	/**
	 * Tells the scoring function that the agents starts a new leg.
	 *
	 * @param time The time at which the agent starts the new leg.
	 * @param leg The leg the agent starts. Can be used to get leg mode and other
	 * information about the leg.
	 */
	public void startLeg(final double time, final Leg leg);

	/**
	 * Tells the scoring function that the current leg ends.
	 *
	 * @param time The time at which the current leg ends.
	 */
	public void endLeg(final double time);

	/**
	 * Tells the scoring function that the agent got stuck in the simulation and
	 * is removed from the simulation. This should usually lead to a high penalty
	 * in the score, as the agent was not able to perform its plan as wanted.
	 * An agent can get stuck while performing an activity or while driving.
	 *
	 * @param time The time at which the agent got stuck and was removed from the
	 * simulation.
	 */
	public void agentStuck(final double time);

	/**
	 * Adds the specified amount of utility to the agent's score. This is mostly
	 * used for handling {@link AgentMoneyEvent}s, allowing other parts of the
	 * code to influence an agent's score.
	 *
	 * @param amount amount to be added to the agent's score
	 */
	public void addMoney(final double amount);

	/**
	 * Tells the scoring function that no more information will be given to it
	 * and that the final score should be calculated.  But the score must <b>not</b>
	 * be written to the plan!
	 */
	public void finish();

	/**
	 * Returns the score for this plan.

	 * @return the score
	 */
	public double getScore();

	/**
	 * May be used to reset scores and counters of implementations of scoring
	 * functions in order to re-score the same plan object with different
	 * time information.
	 */
	public void reset();

}
