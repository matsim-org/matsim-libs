/**
 * How good the approximation is: the matrix, measured against routing and against the trips of a run.
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
 *     ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.ExperiencedLegs}, the default), or
 *     generated pairs ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.ODSample}). Real trips
 *     weight the error by what actually happens; generated pairs cover the matrix evenly, including the
 *     pairs and hours nobody drove in this scenario - and that is also why they are not the default: a
 *     volume-weighted congestion index is a statement about the vehicles that drove, so applied to a
 *     pair nobody drove it overestimates, badly enough to lose to no congestion model at all. See
 *     {@link org.matsim.contrib.msp.traveltimematrix.evaluation.CongestionExposure}, which measures
 *     that.</li>
 *     <li><b>what to compare against</b>, and this is a <em>list</em>: free-speed routing, which
 *     isolates the zone system of the matrix; time-dependent routing over the congested travel times of
 *     the run ({@link org.matsim.contrib.msp.traveltimematrix.evaluation.RoutedTravelTimes}), which
 *     isolates the congestion model; what the trips really took, which contains everything the matrix
 *     cannot know - the route the agent chose, the queue it sat in, the exact minute it left - and is
 *     the error an analysis using the matrix really suffers; and one day what a routing API answers.
 *     They typically have opposite signs, which is why they are a list and not a choice.</li>
 * </ul>
 * The result is therefore a <b>grid</b>, {@code references x variants}, and the cells on and near its
 * diagonal are what make the rest of it readable - see
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation}. Only one combination is
 * impossible - an experienced travel time for a pair nobody drove - and it is refused before anything
 * is read.
 * <p>
 * The two choices meet in
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery}: a pair, a departure time,
 * and whatever is known about that pair without asking the matrix. Nothing in it is called "the"
 * reference, because there is more than one.
 * <p>
 * The ladder of rungs that end up in the table, from the exact answer down to the approximation under
 * test:
 * <ul>
 *     <li><b>variant 4</b> - time-dependent shortest paths over the congested travel times of a finished
 *     run, one shortest-path tree per distinct origin and departure time. Reported under the name of the
 *     reference it also is, because against that reference it is the self-check of the grid.</li>
 *     <li><b>level 3</b> - the free-speed path, timed under congestion. Its gap to variant 4 is the cost
 *     of holding the path fixed, which every level below it pays as well.</li>
 *     <li><b>levels 1 and 0</b> - the matrix itself, see
 *     {@link org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix}.</li>
 *     <li>two baselines that must be beaten: no congestion at all, and - on request - a constant speed
 *     factor ({@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndex#constant(double)}),
 *     which is what {@code DetourTimeEstimator} of {@code contribs/drt} uses today. The step from the
 *     first to the second is what modelling congestion buys; the step from the second to level 0 is what
 *     the time of day buys.</li>
 * </ul>
 * Next to the error table sits one diagnostic that is not an error metric:
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.CongestionExposure} reports how much excess
 * travel time the queries carry against how much the index applies to them, over the same groups. It is
 * the difference between "the congestion model is miscalibrated" and "the congestion model is being asked
 * about pairs whose congestion the run never recorded", and nothing in a MAPE can tell those apart.
 * <p>
 * Every one of them is a {@link org.matsim.contrib.zone.skims.TravelTimeMatrix}, the router included, so
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.MatrixEvaluation} asks all of them the same
 * question and knows nothing about which of them routes - nor about which of the two choices above was
 * made. That is what lets every cell of the grid be read against every other:
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeErrorStats} computes the same
 * MAPE, bias and RMSE throughout.
 * <p>
 * <b>What none of this is.</b> Every implemented reference is measured against one finished run, not
 * against the world. The routed ones are the exact answer <em>under that run's travel times</em>, so a
 * route the simulation would never have taken is still optimal there; the experienced one is what that
 * run did. A run whose congestion is wrong yields a reference and a validation that are wrong in the
 * same way, because the index was measured from the same events. Validating the run itself against real
 * travel times is a different job, and {@code SampleValidationRoutes} of {@code contribs/application}
 * and {@code TravelTimeValidationRunner} of {@code contribs/analysis} are where it lives - the
 * {@code from_node,to_node} shape of a written
 * {@link org.matsim.contrib.msp.traveltimematrix.evaluation.TravelTimeQuery} is what the former reads,
 * so the same pairs can go to an API and come back as the measured travel time of another evaluation.
 */
package org.matsim.contrib.msp.traveltimematrix.evaluation;
