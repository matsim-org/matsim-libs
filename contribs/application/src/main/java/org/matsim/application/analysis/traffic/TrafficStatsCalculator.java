package org.matsim.application.analysis.traffic;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.analysis.VolumesAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.trafficmonitoring.QSimFreeSpeedTravelTime;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * Class to calculate the traffic congestion index based on the paper
 * "A Traffic Congestion Assessment Method for Urban Road Networks Based on Speed Performance Index" by Feifei He, Xuedong Yan, Yang Liu, Lu Ma.
 */
public final class TrafficStatsCalculator {

	private final Network network;
	private final TravelTime travelTime;
	private final VolumesAnalyzer volumesAnalyzer;
	private final QSimFreeSpeedTravelTime qSimFreeSpeedTravelTime = new QSimFreeSpeedTravelTime(1);

	private final int timeSlice;


	public TrafficStatsCalculator(Network network, TravelTime travelTime, VolumesAnalyzer volumesAnalyzer, int timeSlice) {
		this.network = network;
		this.travelTime = travelTime;
		this.volumesAnalyzer = volumesAnalyzer;
		this.timeSlice = timeSlice;
	}

	/**
	 * The expected extra travel time on a link at a specific time. Based on the idea of TomTom travel time index
	 * @param link the link to be analyzed
	 * @param time time of the day
	 * @return excess travel time (in absolute value).
	 */
	public double getLinkExcessTravelTime(Link link, double time) {
		double congestedTravelTime = travelTime.getLinkTravelTime(link, time, null, null);
		double freeSpeedTravelTime = qSimFreeSpeedTravelTime.getLinkTravelTime(link, time, null, null);
		return Math.max(0., congestedTravelTime - freeSpeedTravelTime);
	}

	/**
	 * The expected percentage of extra travel time on a link over a period of time. Based on the idea of TomTom travel time index
	 * @return average value of excess travel time index for the time period
	 */
	public double getLinkExcessTravelTimeIndex(Link link, int startTime, int endTime) {
		int observedPeriodCounter = 0;
		double sumExcessTravelTimeIndexValue = 0.;
		for (int time = startTime; time < endTime; time += timeSlice) {
			double freeSpeedTravelTime = qSimFreeSpeedTravelTime.getLinkTravelTime(link, time, null, null);
			sumExcessTravelTimeIndexValue += this.getLinkExcessTravelTime(link, time) / freeSpeedTravelTime;
			observedPeriodCounter++;
		}

		if (observedPeriodCounter == 0)
			return 0.;
		return sumExcessTravelTimeIndexValue / observedPeriodCounter;
	}

	/**
	 * Calculates the network TomTom congestion index (normalized excess travel time) for a given time period. Can be done for a certain road type.
	 */
	public double getTomTomNetworkCongestionIndex(int startTime, int endTime, @Nullable String roadType) {
		double sumExcessTravelTime = 0.;
		double sumFreeSpeedTravelTime = 0.;

		for (Map.Entry<Id<Link>, ? extends Link> entry : this.network.getLinks().entrySet()) {
			Link link = entry.getValue();

			String type = NetworkUtils.getHighwayType(link);
			if (roadType != null && !type.equals(roadType))
				continue;

			if (!volumesAnalyzer.getLinkIds().contains(link.getId())){
				continue;
			}

			for (int time = startTime; time < endTime; time += timeSlice) {
				double excessTravelTime = this.getLinkExcessTravelTime(link, time);
				double freeSpeedTravelTime = qSimFreeSpeedTravelTime.getLinkTravelTime(link, time, null, null);

				// We cannot get time-bin-size of the Volumes (as it is private). In the analysis, time-bin-size is always 3600, so we enter the value
				// by hand.
				int volumeIdx = (int) Math.floor(time / 3600.);
				double volumeRatio = timeSlice / 3600.;

				double volumeDuringTimeSlice = volumesAnalyzer.getVolumesForLink(link.getId())[volumeIdx] * volumeRatio;
				sumExcessTravelTime += excessTravelTime * volumeDuringTimeSlice;
				sumFreeSpeedTravelTime += freeSpeedTravelTime * volumeDuringTimeSlice;
			}
		}
		return sumExcessTravelTime / sumFreeSpeedTravelTime;
	}


	// Previous analysis from here
	/**
	 * Calculates the speed performance index, which is the ratio of actual travel time and free speed travel time.
	 */
	public double getSpeedPerformanceIndex(Link link, double time) {

		double length = link.getLength();

		double actualTravelTime = travelTime.getLinkTravelTime(link, time, null, null);

		double actualSpeed = length / actualTravelTime;

		double ratio = actualSpeed / link.getFreespeed();

		return ratio > 1 ? 1 : ratio;
	}

	public double getSpeedPerformanceIndex(Link link, int startTime, int endTime) {
		DoubleList indices = new DoubleArrayList();

		for (int time = startTime; time < endTime; time += timeSlice)
			indices.add(
					this.getSpeedPerformanceIndex(link, time)
			);

		return indices.doubleStream().average().orElse(-1);
	}

	/**
	 * Calculates the congestion index based on the ratio of actual travel time and free speed travel time.
	 *
	 * @deprecated -- // kai does not like this quantity.  In particular, he finds the re-weighting by the fraction of non-congested time arbitrary;
	 * //	this is consistent with the fact that it cannot be interpreted in ecomics terms (other than, say, the TomTom congestion index).
	 */
	@Deprecated
	public double getLinkCongestionIndex(Link link, int startTime, int endTime) {

		DoubleList speedPerformance = new DoubleArrayList();

		int congestedPeriodCounter = 0;
		int totalObservedPeriods = 0;

		for (int time = startTime; time < endTime; time += timeSlice) {

			double speedPerformanceIndex = this.getSpeedPerformanceIndex(link, time);
			speedPerformance.add(speedPerformanceIndex);

			if (speedPerformanceIndex <= 0.5)
				congestedPeriodCounter++;

			totalObservedPeriods++;
		}

		double averageSpeedPerformance = speedPerformance.doubleStream().average().orElse(-1);

		return averageSpeedPerformance * (1 - (double) congestedPeriodCounter / totalObservedPeriods);
	}

	/**
	 * Calculates the network congestion index for a given time period. Can be done for a certain road type.
	 */
	@Deprecated
	public double getNetworkCongestionIndex(int startTime, int endTime, @Nullable String roadType) {

		double sumOfLinkLengthMultipiesLinkCongestionIndex = 0.0;
		double sumLinkLength = 0.0;

		for (Map.Entry<Id<Link>, ? extends Link> entry : this.network.getLinks().entrySet()) {
			Link link = entry.getValue();

			String type = NetworkUtils.getHighwayType(link);
			if (roadType != null && !type.equals(roadType))
				continue;

			double linkCongestionIndex = getLinkCongestionIndex(link, startTime, endTime);

			double length = link.getLength() * link.getNumberOfLanes();

			sumOfLinkLengthMultipiesLinkCongestionIndex += length * linkCongestionIndex;
			sumLinkLength += length;
		}

		return sumOfLinkLengthMultipiesLinkCongestionIndex / sumLinkLength;
	}

	/**
	 * Calculates the avg speed for a given link and time interval in meter per seconds.
	 */
	public double getAvgSpeed(Link link, int startTime, int endTime) {

		DoubleList speeds = new DoubleArrayList();

		for (int time = startTime; time < endTime; time += timeSlice) {

			double seconds = this.travelTime.getLinkTravelTime(link, time, null, null);
			speeds.add(link.getLength() / seconds);
		}

		return speeds.doubleStream().average().orElse(-1);
	}
}
