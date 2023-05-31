package org.matsim.application.analysis.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CongestionIndex {

	private final Network network;
	private final TravelTime travelTime;

	public CongestionIndex(Network network, TravelTime travelTime) {
		this.network = network;
		this.travelTime = travelTime;
	}

	public double getLinkCongestionIndex(Link link, double time) {

		double length = link.getLength();
		double freespeed = link.getFreespeed();

		double freeFlowTravelTime = length / freespeed;

		//probably not the minimum avg travel time on link
		double actualTravelTime = travelTime.getLinkTravelTime(link, time, null, null);

		return actualTravelTime / freeFlowTravelTime;
	}

	/**
	 * Calculates the congestion index based on ratio of actual average and free flow speed according to Christidis and Rivas "Measuring Road Congestion
	 * */
	public double getNetworkCongestionIndexByRelativeTravelTime(Network network, double time, @Nullable String roadType) {

		double sumOfLinkLengthMultipiesLinkCongestionIndex = 0.0;
		double sumLinkLength = 0.0;

		for (Map.Entry<Id<Link>, ? extends Link> entry : network.getLinks().entrySet()) {
			Link link = entry.getValue();

			double linkCongestionIndex = getLinkCongestionIndex(link, time);
			double length = link.getLength();

			String type = NetworkUtils.getType(link);

			if(roadType != null){
				if(Pattern.compile(roadType, Pattern.CASE_INSENSITIVE).matcher(type).find())
					continue;
			}

			sumOfLinkLengthMultipiesLinkCongestionIndex += length * linkCongestionIndex;
			sumLinkLength += length;
		}

		return sumOfLinkLengthMultipiesLinkCongestionIndex / sumLinkLength;
	}

	public double getNetworkCongestionIndexByDelay(Network network, double time) {
		return 0;
	}

	public double getNetworkDayCongestionindex() {
		return 0;
	}
}
