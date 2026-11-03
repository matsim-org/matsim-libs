/**
 * How good the approximation is: the matrix, measured against time-dependent routing on the same run.
 * <p>
 * The matrix trades accuracy for one lookup, so the number that decides whether it is usable is the
 * error, and this package produces it. Nothing in here is needed to <em>use</em> the matrix - the
 * dependency runs evaluation &rarr; matrix and never back, so the evaluation can be as heavy as it
 * likes.
 * <p>
 * The ladder, from the exact answer down to the approximation under test:
 * <ul>
 *     <li><b>variant 4</b>, the reference - time-dependent shortest paths over the congested travel
 *     times of a finished run, one shortest-path tree per origin and departure time.</li>
 *     <li><b>level 3</b> - the free-speed path, timed under congestion. Its gap to variant 4 is the cost
 *     of holding the path fixed, which every level below it pays as well.</li>
 *     <li><b>levels 1 and 0</b> - the matrix itself, see
 *     {@link org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix}.</li>
 *     <li>two baselines that must be beaten: a constant speed factor
 *     ({@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex#constant(double)}),
 *     which is what {@code DetourTimeEstimator} of {@code contribs/drt} uses today, and no congestion
 *     at all.</li>
 * </ul>
 * Every one of them is a {@link org.matsim.contrib.zone.skims.TravelTimeMatrix}, the reference
 * included, so {@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation} asks all of
 * them the same question and knows nothing about which of them routes.
 * <ul>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample} - the queries, grouped by
 *     origin because that is what makes the reference affordable.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.RoutedTravelTimes} - variant 4 and
 *     level 3 for those queries.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeErrorStats} - MAPE, bias,
 *     RMSE.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation} - the comparison
 *     and its two csvs.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix} - the
 *     whole experiment as one command.</li>
 * </ul>
 * <b>What the reference is not.</b> It is the exact answer under the travel times of one finished run,
 * not measured reality: a route the simulation would never have taken is still optimal here, and a run
 * whose congestion is wrong produces a reference that is wrong in the same way. Validating the run
 * itself against route APIs is a different job, and {@code SampleValidationRoutes} of
 * {@code contribs/application} is where it lives.
 */
package org.matsim.contrib.msp.traveltimematrix.evaluation;
