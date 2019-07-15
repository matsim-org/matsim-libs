
/* *********************************************************************** *
 * project: org.matsim.*
 * package-info.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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

 /**
 * Scoring is responsible for calculating the (actual) utility/score of a plan.
 * <br>
 * <h3>Important Classes and Interfaces</h3>
 * A {@link org.matsim.core.scoring.ScoringFunction} is responsible for calculating
 * the score of a plan based on information about how long an activity was
 * executed or how much time an agent spent traveling. MATSim provides a
 * default scoring function,
 * {@link org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory}, which takes
 * performing activities and traveling into account for the calculation of the
 * score. Advanced scoring functions could be thought of where where the
 * distance, people traveling along, people met at activities etc. have an
 * influence on the score as well.
 * <br>
 * Because an instance of a ScoringFunction is usually specific for one person
 * or for one plan only, a {@link org.matsim.core.scoring.ScoringFunctionFactory} is
 * needed to create the needed instances of the ScoringFunction to be used.
 * <br>
 * The information, when an agent is traveling and when it is performing an
 * activity, usually comes from {@linkplain org.matsim.core.events MATSim-Events}.
 * {@link org.matsim.core.scoring.EventsToScore} feeds the information from the
 * simulation to the agents' ScoringFunction-instance, which then calculates
 * the effective score.
 *
 * <h3>Typical Usage</h3>
 * If you are using the MATSim-{@linkplain org.matsim.core.controler.Controler} but
 * want to use your own ScoringFunction, you can provide a factory for your
 * ScoringFunction and set it with
 * {@link org.matsim.core.controler.Controler#setScoringFunctionFactory(org.matsim.core.scoring.ScoringFunctionFactory)}.
 * If you do not use the Controler, you can can use
 * {@link org.matsim.core.scoring.EventsToScore} as events handler with your
 * {@link org.matsim.core.events.EventsManager}-object to calculate the scores. Do not
 * forget to call {@link org.matsim.core.scoring.EventsToScore#finish()} after
 * processing all events.
 *
 * <h3>Replacing / Extending the scoring function</h3>
 * There are two possibilities one can think of on how to change the scoring
 * function: One can either want to use a complete different scoring algorithm,
 * or one might plan to use an existing scoring function, but add additional
 * utility-terms to it (e.g. tolls paid, money paid for parking lots, ...).
 * For the first case (use a complete different scoring algorithm), write your
 * own scoring function and scoring function factory, and add it to the
 * Controler with {@link org.matsim.core.controler.Controler#setScoringFunctionFactory(org.matsim.core.scoring.ScoringFunctionFactory)}.
 * But if you only plan to add or subtract some utility amounts from agents'
 * score, you can generate {@link org.matsim.api.core.v01.events.PersonMoneyEvent}s.
 * Scoring functions should listen for <code>AgentUtilityEvent</code>s and just
 * add the utility amount specified in the event to the agent's score.
 *
 * As an example, the roadpricing-package makes use of <code>AgentMoneyEvent</code>s:
 * An event-handler listens for agents entering and leaving links and thus
 * calculates how much toll an agent pays. After the iteration, the event
 * handler generates events itself: for each agent that must pay some toll, an
 * <code>AgentUtilityEvent</code> is generated with the correct amount of toll.
 * The scoring function receives those events and thus adds the paid tolls to
 * the agents' score.
 *
 *
 * @see playground.meisterk.org.matsim.run.westumfahrung.PlanAverageScore
 */
package org.matsim.core.scoring;