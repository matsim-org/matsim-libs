package org.matsim.application.analysis.traffic;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Class to calculate the traffic congestion index based on the paper
 * "A Traffic Congestion Assessment Method for Urban Road Networks Based on Speed Performance Index" by Feifei He, Xuedong Yan, Yang Liu, Lu Ma.
 */
public final class TrafficStatsCalculator {

	private final Network network;
	private final TravelTime travelTime;

	private final int timeSlice;

	public TrafficStatsCalculator(Network network, TravelTime travelTime, int timeSlice) {
		this.network = network;
		this.travelTime = travelTime;
		this.timeSlice = timeSlice;
	}

	/**
	 * Calculates the speed performance index, which is the ratio of actual travel time and free speed travel time.
	 */
	public double getSpeedPerformanceIndex(Link link, double time) {

		double length = link.getLength();

		double allowedSpeed = NetworkUtils.getAllowedSpeed(link);

		double actualTravelTime = travelTime.getLinkTravelTime(link, time, null, null);

		double actualSpeed = length / actualTravelTime;

		double ratio = actualSpeed / allowedSpeed;

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
	 */
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
