package org.matsim.contrib.msp.traveltimematrix.congestion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.analysis.traffic.TrafficStatsCalculator;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.contrib.msp.traveltimematrix.RoadClassification;
import org.matsim.contrib.msp.traveltimematrix.prepare.MeasuredTraffic;
import org.matsim.core.router.util.TravelTime;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Measures a {@link CongestionIndexProfile} on a finished run: the excess travel time index per time bin
 * and {@link RoadClassification road class}.
 * <p>
 * The index is the one of {@link TrafficStatsCalculator#getTomTomNetworkCongestionIndex(int, int, String)}
 * - volume-weighted excess travel time over volume-weighted free-speed travel time - and this class is
 * deliberately a rewrite of it rather than a caller:
 * <ul>
 *     <li>that method filters on <b>one exact</b> OSM highway type, while a road class here is a
 *     <b>group</b> of types, and the groups cannot be combined after the fact: an index is a ratio of two
 *     weighted sums, and the weights are not exposed.</li>
 *     <li>it loops over the whole network per call, so a profile of 96 bins times 6 classes would walk
 *     the network 576 times. Here every class and bin is filled in <b>one</b> pass over the links.</li>
 * </ul>
 * The two therefore have to agree, and {@code CongestionIndexCalculatorTest} asserts that they do for
 * {@link RoadClassification#ALL}: that test is what keeps this class honest.
 * <p>
 * <b>Volume weighting.</b> Weighting by volume is what makes the index the right multiplier for a travel
 * time: it answers "how much longer does the average <em>vehicle</em> take", not "how congested is the
 * average <em>link</em>", and an empty residential street therefore does not dilute it. The price is
 * that a road class with no traffic at all in a bin has no index - {@code 0/0} - and falls back to the
 * network-wide value, which is logged.
 * <p>
 * The weights are the volumes of <b>every</b> vehicle on the link, whatever its mode, which is again what
 * {@code TrafficStatsCalculator} does. Restricting the travel times to one mode is the caller's job and
 * is what {@code RunComputeCongestionIndex} does; the weights stay as they are, because a bus in the
 * queue is part of why the cars are slow.
 *
 * @see org.matsim.contrib.msp.traveltimematrix.RunComputeCongestionIndex to run this on an events file
 */
public final class CongestionIndexCalculator {

	private static final Logger log = LogManager.getLogger(CongestionIndexCalculator.class);

	/**
	 * The time bin size {@link VolumesAnalyzer} is hard-wired to, and whose getter it does not expose.
	 * {@link TrafficStatsCalculator} makes the same assumption, with the same comment. Taken from
	 * {@link MeasuredTraffic}, which is where the volumes this reads were created with it.
	 */
	private static final int VOLUME_BIN_SIZE = MeasuredTraffic.VOLUME_BIN_SIZE;

	private final Network network;
	private final TravelTime travelTime;
	private final VolumesAnalyzer volumes;
	private final RoadClassification roadClasses;

	/**
	 * Free-speed link travel times the excess is measured against. {@link QSimFreeSpeedTravelTime} rather
	 * than {@code length / freespeed}, because the qsim rounds a link time up to a whole time step and the
	 * congested travel times read off the events carry that rounding - comparing against the unrounded
	 * value would report the rounding as congestion.
	 */
	private final QSimFreeSpeedTravelTime freeSpeedTravelTime;

	/**
	 * @param network the links the index is measured over - the study area subnetwork, not the whole
	 * network of the run, see {@link org.matsim.contrib.msp.traveltimematrix.prepare.StudyAreaNetwork}
	 * @param travelTime the congested travel times of the run, e.g.
	 * {@code TravelTimeCalculator.getLinkTravelTimes()} fed with its events
	 * @param volumes the volumes of the run, from the same events
	 * @param roadClasses the classes to measure separately; {@link RoadClassification#single()} for the
	 * network-wide index alone
	 * @param qSimTimeStepSize {@code config.qsim().getTimeStepSize()} of the run, for the free-speed
	 * reference
	 */
	public CongestionIndexCalculator(Network network, TravelTime travelTime, VolumesAnalyzer volumes,
		RoadClassification roadClasses, double qSimTimeStepSize) {
		this.network = network;
		this.travelTime = travelTime;
		this.volumes = volumes;
		this.roadClasses = roadClasses;
		this.freeSpeedTravelTime = new QSimFreeSpeedTravelTime(qSimTimeStepSize);
	}

	/**
	 * @param timeBinSize the resolution of the profile [s]. 900 s is what the traffic dashboard of
	 * {@code contribs/application} uses and a reasonable default: short enough to resolve the peaks,
	 * long enough that each bin still has traffic in it.
	 * @param endTime the end of the day to measure [s], e.g. 86400. Queries past it get the last bin,
	 * see {@link CongestionIndexProfile#binOf(double)}.
	 */
	public CongestionIndexProfile calculate(int timeBinSize, int endTime) {

		if (timeBinSize <= 0 || endTime <= 0) {
			throw new IllegalArgumentException("A congestion index profile needs a positive bin size and end time, "
				+ "not " + timeBinSize + " s and " + endTime + " s.");
		}

		int numberOfBins = (int) Math.ceil((double) endTime / timeBinSize);
		int numberOfClasses = this.roadClasses.size();

		// the two sums of the index, per bin and class, plus one extra column for the network-wide value.
		// Accumulating both sums and dividing at the end is what makes this one pass over the links: a
		// class index is a ratio of sums, not a mean of ratios.
		double[][] excess = new double[numberOfBins][numberOfClasses + 1];
		double[][] freeSpeed = new double[numberOfBins][numberOfClasses + 1];
		int allColumn = numberOfClasses;

		for (Link link : this.network.getLinks().values()) {

			int roadClass = this.roadClasses.indexOf(link);
			int[] linkVolumes = this.volumes.getLinkIds().contains(link.getId())
				? this.volumes.getVolumesForLink(link.getId())
				: null;

			for (int bin = 0; bin < numberOfBins; bin++) {

				// the bin is sampled once, at its start. Both inputs are step functions of time
				// already - TravelTimeCalculator bins the travel times, VolumesAnalyzer the volumes -
				// so a bin no coarser than those resolutions is fully described by one sample, and a
				// coarser one is the caller asking for a coarser model. It is also exactly what
				// TrafficStatsCalculator does, which is what lets the two be compared value by value.
				double time = (double) bin * timeBinSize;

				double freeSpeedTime = this.freeSpeedTravelTime.getLinkTravelTime(link, time, null, null);
				double congestedTime = this.travelTime.getLinkTravelTime(link, time, null, null);
				double excessTime = Math.max(0., congestedTime - freeSpeedTime);

				double volume = volumeAt(linkVolumes, time, timeBinSize);

				excess[bin][roadClass] += excessTime * volume;
				freeSpeed[bin][roadClass] += freeSpeedTime * volume;
				excess[bin][allColumn] += excessTime * volume;
				freeSpeed[bin][allColumn] += freeSpeedTime * volume;
			}
		}

		Map<String, double[]> indexByRoadClass = new LinkedHashMap<>();
		double[] all = new double[numberOfBins];
		for (int bin = 0; bin < numberOfBins; bin++) {
			all[bin] = ratio(excess[bin][allColumn], freeSpeed[bin][allColumn]);
		}
		indexByRoadClass.put(RoadClassification.ALL, all);

		if (!this.roadClasses.isSingleClass()) {
			int[] binsWithoutTraffic = new int[numberOfClasses];
			for (int roadClass = 0; roadClass < numberOfClasses; roadClass++) {
				double[] values = new double[numberOfBins];
				for (int bin = 0; bin < numberOfBins; bin++) {
					if (freeSpeed[bin][roadClass] == 0) {
						// no vehicle on this class in this bin, so there is nothing to measure. The
						// network-wide value is the least wrong stand-in, and it is also what a query
						// would fall back to if the class were left out altogether.
						values[bin] = all[bin];
						binsWithoutTraffic[roadClass]++;
					} else {
						values[bin] = ratio(excess[bin][roadClass], freeSpeed[bin][roadClass]);
					}
				}
				indexByRoadClass.put(this.roadClasses.getRoadClasses().get(roadClass), values);
			}
			logBinsWithoutTraffic(binsWithoutTraffic, numberOfBins);
		}

		CongestionIndexProfile profile = new CongestionIndexProfile(timeBinSize, indexByRoadClass);
		logProfile(profile);
		return profile;
	}

	/**
	 * The volume on a link during a bin, in vehicles. {@link VolumesAnalyzer} counts per hour, so a bin
	 * shorter than an hour gets its share of the hour it sits in - the same approximation
	 * {@link TrafficStatsCalculator} makes. It cancels out of the index whenever all classes are equally
	 * affected, and it is a real assumption only for bins at the edge of a peak.
	 *
	 * @param linkVolumes the hourly volumes of the link, or null if the link saw no traffic at all
	 */
	private static double volumeAt(int[] linkVolumes, double time, int timeBinSize) {
		if (linkVolumes == null) {
			return 0;
		}
		int hour = (int) Math.floor(time / VOLUME_BIN_SIZE);
		if (hour >= linkVolumes.length) {
			return 0;
		}
		return linkVolumes[hour] * (double) timeBinSize / VOLUME_BIN_SIZE;
	}

	private static double ratio(double excess, double freeSpeed) {
		return freeSpeed == 0 ? 0 : excess / freeSpeed;
	}

	private void logBinsWithoutTraffic(int[] binsWithoutTraffic, int numberOfBins) {
		for (int roadClass = 0; roadClass < binsWithoutTraffic.length; roadClass++) {
			if (binsWithoutTraffic[roadClass] > 0) {
				log.warn("Road class '{}' carried no traffic in {} of {} time bins, so those bins hold the "
						+ "network-wide index instead. A class that is empty most of the day is too small to be "
						+ "worth splitting off.", this.roadClasses.getRoadClasses().get(roadClass),
					binsWithoutTraffic[roadClass], numberOfBins);
			}
		}
	}

	private static void logProfile(CongestionIndexProfile profile) {
		StringBuilder summary = new StringBuilder();
		for (String roadClass : profile.getRoadClasses()) {
			double[] values = profile.getValues(roadClass);
			double max = 0;
			int argMax = 0;
			for (int bin = 0; bin < values.length; bin++) {
				if (values[bin] > max) {
					max = values[bin];
					argMax = bin;
				}
			}
			summary.append(String.format(Locale.ROOT, "%n  %-14s mean %.4f, peak %.4f at %02d:%02d", roadClass,
				profile.getDailyMean(roadClass), max,
				argMax * profile.getTimeBinSize() / 3600, argMax * profile.getTimeBinSize() % 3600 / 60));
		}
		log.info("Congestion index over {} bins of {} s:{}", profile.getNumberOfBins(), profile.getTimeBinSize(),
			summary);
	}
}
