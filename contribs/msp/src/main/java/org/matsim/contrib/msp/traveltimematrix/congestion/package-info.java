/**
 * The congestion index: the one number per time bin that makes the matrix time-varying.
 * <p>
 * The index is the <b>excess travel time index</b> of
 * {@link org.matsim.application.analysis.traffic.TrafficStatsCalculator}, i.e. the TomTom-style ratio
 * <pre>
 *   CI(bin) = sum over links and time steps of (congested - free speed) travel time * volume
 *           / sum over links and time steps of  free speed             travel time * volume
 * </pre>
 * so {@code CI = 0.2} means "a trip takes 20% longer than on an empty network". That is exactly the
 * quantity {@code TT_ff * (1 + CI)} needs, which is why this definition and not a speed performance
 * index was picked.
 * <p>
 * {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexCalculator} measures it on a
 * finished run (a {@code TravelTime} and a {@code VolumesAnalyzer} read off the events), one value per
 * time bin and road class, and
 * {@link org.matsim.contrib.msp.traveltimematrix.congestion.CongestionIndexProfile} is what it is kept
 * and written as - a csv small enough to read by hand and to put in a paper.
 */
package org.matsim.contrib.msp.traveltimematrix.congestion;
