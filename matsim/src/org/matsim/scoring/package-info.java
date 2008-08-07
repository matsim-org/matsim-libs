/**
 * Scoring is responsible for calculating the (actual) utility/score of a plan.
 * <br>
 * <h3>Important Classes and Interfaces</h3>
 * A {@link org.matsim.scoring.ScoringFunction} is responsible for calculating
 * the score of a plan based on information about how long an activity was
 * executed or how much time an agent spent traveling. MATSim provides a
 * default scoring function,
 * {@link org.matsim.scoring.CharyparNagelScoringFunction}, which takes
 * performing activities and traveling into account for the calculation of the
 * score. Advanced scoring functions could be thought of where where the
 * distance, people traveling along, people met at activities etc. have an
 * influence on the score as well.
 * <br>
 * Because an instance of a ScoringFunction is usually specific for one person
 * or for one plan only, a {@link org.matsim.scoring.ScoringFunctionFactory} is
 * needed to create the needed instances of the ScoringFunction to be used.
 * <br>
 * The information, when an agent is traveling and when it is performing an
 * activity, usually comes from {@linkplain org.matsim.events MATSim-Events}.
 * {@link org.matsim.scoring.EventsToScore} feeds the information from the
 * simulation to the agents' ScoringFunction-instance, which then calculates
 * the effective score. For the few cases where the expected (not the actual,
 * experienced) score of a plan should be calculated,
 * {@link org.matsim.scoring.PlanScorer} may be of help.
 *
 * <h3>Typical Usage</h3>
 * If you are using the MATSim-{@linkplain org.matsim.controler.Controler} but
 * want to use your own ScoringFunction, you can provide a factory for your
 * ScoringFunction and set it with
 * {@link org.matsim.controler.Controler#setScoringFunctionFactory(org.matsim.scoring.ScoringFunctionFactory)}.
 * If you do not use the Controler, you can can use
 * {@link org.matsim.scoring.EventsToScore} as events handler with your
 * {@link org.matsim.events.Events}-object to calculate the scores. Do not
 * forget to call {@link org.matsim.scoring.EventsToScore#finish()} after
 * processing all events.
 *
 * @see org.matsim.population.algorithms.PlanAverageScore
 */
package org.matsim.scoring;