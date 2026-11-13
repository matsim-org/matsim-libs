/**
 * A time-varying car travel time matrix, built from a free-speed matrix and a congestion index.
 * <p>
 * The idea is one equation. A free-speed travel time matrix is computed once, a congestion index curve is
 * read off a finished run, and a query multiplies the two:
 * <pre>
 *   TT(o, d, t) = TT_ff(o, d) * (1 + beta * CI(t))
 * </pre>
 * so that the time of day costs one lookup and one multiplication instead of one full matrix per time
 * bin. {@link org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix} is that product. It
 * implements {@link org.matsim.contrib.zone.skims.TravelTimeMatrix} of {@code contribs/dvrp}, whose
 * {@code getTravelTime} already takes a departure time, so it is a drop-in replacement wherever DVRP,
 * DRT or an analysis uses a matrix today.
 * <p>
 * Two variants are implemented, and they differ only in what {@code CI} is conditioned on:
 * <ul>
 *     <li><b>level 0</b> - one index for the whole network, {@code CI(t)}.</li>
 *     <li><b>level 1</b> - one index per road class, weighted by how much of the free-speed travel time
 *     of that origin-destination pair is spent on each class:
 *     {@code TT = TT_ff * (1 + beta * sum_k s_k(o,d) * CI_k(t))}. A motorway trip is then not slowed
 *     down by congestion that happens in residential streets.</li>
 * </ul>
 * <b>The base matrix comes in two shapes</b>, and which one is built follows from
 * {@code maxNeighborDistance} alone. A finite radius gives the two-half matrix of {@code contribs/dvrp}:
 * zone to zone for long pairs, node to node for short ones. An <b>unbounded</b> radius gives
 * {@link org.matsim.contrib.msp.traveltimematrix.NodeToNodeTravelTimeMatrix} - every pair node to node,
 * no zone system at all, and therefore exact. That matters for measuring rather than only for answering:
 * with the discretisation error at zero, what is left of the error of the equation above is the
 * congestion index and nothing else. Its price is level 1, whose road class shares are stored per zone
 * pair.
 * <p>
 * The pieces live one per step of the method:
 * <ul>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.prepare} - cutting the car subnetwork the
 *     matrix and the index are built over.</li>
 *     <li>{@link org.matsim.contrib.msp.traveltimematrix.congestion} - the congestion index: how it is
 *     measured on a run, and the binned curve it is stored as.</li>
 *     <li>this package - the road classes, the per origin-destination road class shares that level 1
 *     weighs with, the matrix itself, and the two commands.</li>
 * </ul>
 * Dependencies run from this package into the sub packages, never the other way round, and nothing here
 * knows about any particular application of the matrix.
 *
 * @see org.matsim.contrib.msp.traveltimematrix.TimeVaryingTravelTimeMatrix the matrix itself
 * @see org.matsim.contrib.msp.traveltimematrix.TravelTimeMatrixConfigGroup the parameters
 * @see org.matsim.contrib.msp.traveltimematrix.RunComputeCongestionIndex to measure the index on a run
 */
package org.matsim.contrib.msp.traveltimematrix;
