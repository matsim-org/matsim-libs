/**
 * How good the approximation is: the matrix, measured against a router or against the trips of a run.
 * <p>
 * The matrix trades accuracy for one lookup, so the number that decides whether it is usable is the
 * error, and this package produces it. Nothing in here is needed to <em>use</em> the matrix - the
 * dependency runs evaluation &rarr; matrix and never back, so the evaluation can be as heavy as it
 * likes.
 * <p>
 * An evaluation is two independent choices, and
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.RunEvaluateTravelTimeMatrix} is those two
 * choices and nothing else:
 * <ul>
 *     <li><b>which questions to ask</b> - the trips the run really made
 *     ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.ExperiencedLegs}), or generated pairs
 *     ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample}). Real trips weight the error
 *     by what actually happens; generated pairs cover the matrix evenly, including the pairs and hours
 *     nobody drove in this scenario.</li>
 *     <li><b>what to compare against</b> - what those trips really took, or what time-dependent routing
 *     computes for the same questions
 *     ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.RoutedTravelTimes}), or one day what a
 *     routing API answers. The first contains everything the matrix cannot know - the route the agent
 *     chose, the queue it sat in, the exact minute it left - and is the error an analysis using the
 *     matrix really suffers; the second isolates the error of the approximation itself. They typically
 *     have opposite signs.</li>
 * </ul>
 * The two meet in {@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery}: a pair, a
 * departure time, and the travel time to be compared against, whoever determined it. Only one
 * combination is impossible - an experienced travel time for a pair nobody drove - and it is refused
 * before anything is read.
 * <p>
 * The ladder of rungs that end up in the table, from the exact answer down to the approximation under
 * test:
 * <ul>
 *     <li><b>variant 4</b> - time-dependent shortest paths over the congested travel times of a finished
 *     run, one shortest-path tree per distinct origin and departure time.</li>
 *     <li><b>level 3</b> - the free-speed path, timed under congestion. Its gap to variant 4 is the cost
 *     of holding the path fixed, which every level below it pays as well.</li>
 *     <li><b>levels 1 and 0</b> - the matrix itself, see
 *     {@link org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix}.</li>
 *     <li>two baselines that must be beaten: a constant speed factor
 *     ({@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex#constant(double)}),
 *     which is what {@code DetourTimeEstimator} of {@code contribs/drt} uses today, and no congestion
 *     at all.</li>
 * </ul>
 * Every one of them is a {@link org.matsim.contrib.zone.skims.TravelTimeMatrix}, the router included, so
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation} asks all of them the same
 * question and knows nothing about which of them routes - nor about which of the two choices above was
 * made. That is what lets two tables be read side by side:
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeErrorStats} computes the same
 * MAPE, bias and RMSE either way.
 * <p>
 * <b>What none of this is.</b> Both implemented references are measured against one finished run, not
 * against the world. The routed one is the exact answer <em>under that run's travel times</em>, so a
 * route the simulation would never have taken is still optimal there; the experienced one is what that
 * run did. A run whose congestion is wrong yields both a reference and a validation that are wrong in
 * the same way, because the index was measured from the same events. Validating the run itself against
 * real travel times is a different job, and {@code SampleValidationRoutes} of
 * {@code contribs/application} and {@code TravelTimeValidationRunner} of {@code contribs/analysis} are
 * where it lives - the {@code from_node,to_node} shape of a written
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery} is what the former reads,
 * so the same pairs can go to an API and come back as the reference of another evaluation.
 */
package org.matsim.contrib.msp.traveltimematrix.evaluation;
