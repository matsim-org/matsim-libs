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

import org.matsim.core.config.*;

public class DvrpConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "dvrp";

	@SuppressWarnings("deprecation")
	public static DvrpConfigGroup get(Config config) {
		return (DvrpConfigGroup)config.getModule(GROUP_NAME);// will fail if not in the config
	}

	public static final String MODE = "mode";
	public static final String NETWORK_MODE = "networkMode";
	public static final String TRAVEL_TIME_ESTIMATION_ALPHA = "travelTimeEstimationAlpha";

	private String mode = null; // travel mode (passengers'/customers' perspective)
	private String networkMode = null; // used for building routes, calculating travel times, etc.
										// (dispatcher's perspective)
	private double travelTimeEstimationAlpha = 0.05; // between 0 and 1; 0=> no averaging, only the initial time is used

	public DvrpConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, "Mode which will be handled by PassengerEngine and VrpOptimizer "
				+ "(passengers'/customers' perspective)");
		map.put(NETWORK_MODE,
				"Mode of which the network will be used for routing vehicles, calculating trave times, "
						+ "etc. (fleet operator's perspective). "
						+ "If null, no mode filtering is done; the standard network (Scenario.getNetwork()) is used");
		map.put(TRAVEL_TIME_ESTIMATION_ALPHA,
				"Used for estimation of travel times for VrpOptimizer by means of the exponential moving average."
						+ " The weighting decrease, alpha, must be in (0,1]."
						+ " We suggest small values of alpha, e.g. 0.05."
						+ " The averaging starts from the initial travel time estimates. If not provided,"
						+ " the free-speed TTs is used as the initial estimates"
						+ " For more info see comments in: VrpTravelTimeEstimator, VrpTravelTimeModules, DvrpModule.");
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
}
