package org.matsim.application.analysis.traffic;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
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
 * */
public class CongestionIndex {

	private final Network network;
	private final TravelTime travelTime;

	private final Logger log = LogManager.getLogger(CongestionIndex.class);

	public CongestionIndex(Network network, TravelTime travelTime) {
		this.network = network;
		this.travelTime = travelTime;
	}

	/**
	 * Calculates the speed performance index, which is the ratio of actual travel time and free speed travel time.
	 * */
	public double getSpeedPerformanceIndex(Link link, double time) {

		double length = link.getLength();
		double freespeed = link.getFreespeed();

		double freeFlowTravelTime = length / freespeed;

		//probably not the minimum avg travel time on link
		double actualTravelTime = travelTime.getLinkTravelTime(link, time, null, null);

		double ratio = actualTravelTime / freeFlowTravelTime * 100;

		return ratio > 100 ? 100 : ratio;
	}

	/**
	 * Calculates the congestion index based on the ratio of actual travel time and free speed travel time.
	 * */
	public double getLinkCongestionIndex(Link link, int startTime, int endtime) {

		DoubleList speedPerformance = new DoubleArrayList();

		int congestedPeriodCounter = 0;
		int totalObservedPeriods = 0;

		for (int time = startTime; time < endtime; time += 900) {

			double speedPerformanceIndex = this.getSpeedPerformanceIndex(link, time);
			speedPerformance.add(speedPerformanceIndex);

			if (speedPerformanceIndex <= 50)
				congestedPeriodCounter++;

			totalObservedPeriods++;
		}

		double averageSpeedPerformance = speedPerformance.doubleStream().average().orElse(-1);

		return averageSpeedPerformance / 100 * (1 - (double) congestedPeriodCounter / totalObservedPeriods);
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
			double length = link.getLength();

			String type = NetworkUtils.getType(link);

			if (pattern != null) {

				try {
					boolean b = pattern.matcher(type).find();
					if(!b)
						continue;
				} catch (NullPointerException e){
					this.log.warn("Error processing link {} with type {}", link.getId().toString(), type);
					continue;
				}
			}

			sumOfLinkLengthMultipiesLinkCongestionIndex += length * linkCongestionIndex;
			sumLinkLength += length;
		}

		double index = sumOfLinkLengthMultipiesLinkCongestionIndex / sumLinkLength;

		return new BigDecimal(index).setScale(2, RoundingMode.HALF_UP).doubleValue();
	}
}
