package org.matsim.application.analysis.traffic;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.Int2DoubleArrayMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Class to calculate the traffic congestion index based on the paper
 * "A Traffic Congestion Assessment Method for Urban Road Networks Based on Speed Performance Index" by by Feifei He, Xuedong Yan*, Yang Liu, Lu Ma.
 */
public class CongestionIndex {

	private final Network network;
	private final TravelTime travelTime;

	private final int timeSlice = 900;

	private final IdMap<Link, Int2DoubleMap> perLink = new IdMap(Link.class);

	public CongestionIndex(Network network, TravelTime travelTime) {
		this.network = network;
		this.travelTime = travelTime;

		for (Link link : network.getLinks().values())
			perLink.put(link.getId(), new Int2DoubleArrayMap());
	}

	/**
	 * Calculates the speed performance index, which is the ratio of actual travel time and free speed travel time.
	 */
	public double getSpeedPerformanceIndex(Link link, double time) {

		double length = link.getLength();

		double allowedSpeed = Double.parseDouble(link.getAttributes().getAttribute(NetworkUtils.ALLOWED_SPEED).toString());

		double actualTravelTime = travelTime.getLinkTravelTime(link, time, null, null);

		double actualSpeed = length / actualTravelTime;

		double ratio = actualSpeed / allowedSpeed;

		return ratio > 1 ? 1 : ratio;
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

		Pattern pattern = null;

		if (roadType != null)
			pattern = Pattern.compile(roadType, Pattern.CASE_INSENSITIVE);


		for (Map.Entry<Id<Link>, ? extends Link> entry : this.network.getLinks().entrySet()) {
			Link link = entry.getValue();

			double linkCongestionIndex = getLinkCongestionIndex(link, startTime, endTime);

			if (roadType == null)
				this.perLink.get(link.getId()).put(startTime, linkCongestionIndex);

			double length = link.getLength();
			String type = NetworkUtils.getHighwayType(link);

			if (pattern != null && pattern.matcher(type).find())
				continue;

			sumOfLinkLengthMultipiesLinkCongestionIndex += length * linkCongestionIndex;
			sumLinkLength += length;
		}

		double index = sumOfLinkLengthMultipiesLinkCongestionIndex / sumLinkLength;

		return new BigDecimal(index).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * Calculates the avg speed for a given link and time interval.
	 * */
	public double getAvgSpeed(Link link, int startTime, int endTime) {

		DoubleList list = new DoubleArrayList();

		for (int time = startTime; time < endTime; time += timeSlice) {
			double linkTravelTime = this.travelTime.getLinkTravelTime(link, time, null, null);
			list.add(linkTravelTime);
		}

		double avgSpeed = list.doubleStream().average().orElse(-1);

		return new BigDecimal(avgSpeed).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}

	public IdMap<Link, Int2DoubleMap> getPerLink() {
		return perLink;
	}
}
