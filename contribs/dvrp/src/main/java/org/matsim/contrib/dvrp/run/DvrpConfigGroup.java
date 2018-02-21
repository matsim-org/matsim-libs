/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.dvrp.run;

import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

public class DvrpConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "dvrp";

	@SuppressWarnings("deprecation")
	public static DvrpConfigGroup get(Config config) {
		return (DvrpConfigGroup)config.getModule(GROUP_NAME);// will fail if not in the config
	}

	public static final String MODE = "mode";
	public static final String NETWORK_MODE = "networkMode";
	public static final String TRAVEL_TIME_ESTIMATION_ALPHA = "travelTimeEstimationAlpha";
	public static final String TRAVEL_TIME_ESTIMATION_BETA = "travelTimeEstimationBeta";

	private String mode = null; // travel mode (passengers'/customers' perspective)
	private String networkMode = TransportMode.car; // used for building routes, calculating travel times, etc.
	// (dispatcher's perspective)
	private double travelTimeEstimationAlpha = 0.05; // in (0, 1]; 1 => TTs from the last iteration only
	private double travelTimeEstimationBeta = 0; // in [s], in [0, +oo); 0 => only offline TT estimation

	public DvrpConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, "Mode which will be handled by PassengerEngine and VrpOptimizer "
				+ "(passengers'/customers' perspective)");
		map.put(NETWORK_MODE, "Mode of which the network will be used for routing vehicles, calculating trave times, "
				+ "etc. (fleet operator's perspective). " + "Default is car.");
		map.put(TRAVEL_TIME_ESTIMATION_ALPHA, //
				"Used for OFFLINE estimation of travel times for VrpOptimizer"
						+ " by means of the exponential moving average."
						+ " The weighting decrease, alpha, must be in (0,1]."
						+ " We suggest small values of alpha, e.g. 0.05."
						+ " The averaging starts from the initial travel time estimates. If not provided,"
						+ " the free-speed TTs is used as the initial estimates");
		map.put(TRAVEL_TIME_ESTIMATION_BETA,
				"Used for ONLINE estimation of travel times for VrpOptimizer"
						+ " by combining WithinDayTravelTime and DvrpOfflineTravelTimeEstimator."
						+ " The beta coefficient is provided in seconds and should be either 0 (no online estimation)"
						+ " or positive (mixed online-offline estimation)."
						/////
						+ " For 'beta = 0', only the offline estimate is used:" + " 'onlineTT(t) = offlineTT(t)',"
						+ " where 'offlineTT(t)' in the offline estimate for TT at time 't',"
						/////
						+ " For 'beta > 0', estimating future TTs at time 't',"
						+ " uses the currently observed TT to correct the offline estimates is made:"
						+ " where 'currentTT' is the currently observed TT,"
						+ " and 'correction = min(1, max(0, 1 - (time - currentTime) / beta))'"
						////
						+ " The rule is that correction decreases linearly from 1 (when 'time = currentTime')"
						+ " to 0 (when 'time = currentTime + beta'"
						+ " For 'time > currentTime + beta' correction is 0,"
						+ " whereas if 'time < currentTime' it is 1."
						////
						+ " If beta is sufficiently large, 'beta >> 0', only the currently observed TT is used.");
		// In DVRP 'time < currentTime' may only happen for backward path search, a adding proper search termination
		// criterion should prevent this from happening
		return map;
	}

	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
	}

	@StringGetter(NETWORK_MODE)
	public String getNetworkMode() {
		return networkMode;
	}

	@StringSetter(NETWORK_MODE)
	public void setNetworkMode(String routingMode) {
		this.networkMode = routingMode;
	}

	@StringGetter(TRAVEL_TIME_ESTIMATION_ALPHA)
	public double getTravelTimeEstimationAlpha() {
		return travelTimeEstimationAlpha;
	}

	@StringSetter(TRAVEL_TIME_ESTIMATION_ALPHA)
	public void setTravelTimeEstimationAlpha(double travelTimeEstimationAlpha) {
		this.travelTimeEstimationAlpha = travelTimeEstimationAlpha;
	}

	@StringGetter(TRAVEL_TIME_ESTIMATION_BETA)
	public double getTravelTimeEstimationBeta() {
		return travelTimeEstimationBeta;
	}

	@StringSetter(TRAVEL_TIME_ESTIMATION_BETA)
	public void setTravelTimeEstimationBeta(double travelTimeEstimationBeta) {
		this.travelTimeEstimationBeta = travelTimeEstimationBeta;
	}
}
