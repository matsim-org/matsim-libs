/**
 * How good the approximation is: the matrix, measured against time-dependent routing on the same run.
 * <p>
 * The matrix trades accuracy for one lookup, so the number that decides whether it is usable is the
 * error, and this package produces it. Nothing in here is needed to <em>use</em> the matrix - the
 * dependency runs evaluation &rarr; matrix and never back, so the evaluation can be as heavy as it
 * likes.
 * <p>
 * There are <b>two</b> errors and they answer different questions.
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix} measures the
 * matrix against a time-dependent router on the same travel times, which is the error of the
 * approximation alone, on any pairs and departure times one likes.
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunValidateTravelTimeMatrix} measures it
 * against the car legs a run actually drove
 * ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.ExperiencedLegs}), which is the error an
 * analysis using the matrix really suffers: it also contains the route the agent chose, the queue it sat
 * in and the exact minute it left. The second is always the larger, and both go through the same
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation}, so the two tables have
 * the same columns and can be read side by side.
 * <p>
 * The ladder of the first of them, from the exact answer down to the approximation under test:
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
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.ExperiencedLegs} - the car legs of a
 *     run, as the node-to-node travel times a matrix can be held to.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeErrorStats} - MAPE, bias,
 *     RMSE.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation} - the comparison
 *     and its two csvs.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix} and
 *     {@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunValidateTravelTimeMatrix} - the two
 *     studies, one command each.</li>
 * </ul>
 * <b>What neither of them is.</b> Both are measured against one finished run, not against the world. The
 * routing reference is the exact answer <em>under that run's travel times</em>, so a route the simulation
 * would never have taken is still optimal there; the legs are what that run experienced. A run whose
 * congestion is wrong yields both a reference and a validation that are wrong in the same way, because
 * the index was measured from the same events. Validating the run itself against real travel times is a
 * different job, and {@code SampleValidationRoutes} of {@code contribs/application} and
 * {@code TravelTimeValidationRunner} of {@code contribs/analysis} are where it lives - the
 * {@code from_node,to_node} shape of a written {@link org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample}
 * is what the former reads, so the same pairs can go to an API.
 */
package org.matsim.contrib.msp.traveltimematrix.evaluation;
