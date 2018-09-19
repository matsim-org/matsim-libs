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

import javax.annotation.Nullable;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

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
	static final String MODE_EXP = "Mode which will be handled by PassengerEngine and VrpOptimizer "
			+ "(passengers'/customers' perspective)";

	public static final String NETWORK_MODE = "networkMode";
	static final String NETWORK_MODE_EXP = "Mode of which the network will be used for routing vehicles. "
			+ "Default is car, i.e. the car network is used. "
			+ "'null' means no network filtering - the scenario.network is used";

	public static final String MOBSIM_MODE = "mobsimMode";
	static final String MOBSIM_MODE_EXP =
			"Mode of which the network will be used for throwing events and hence calculating travel times. "
					+ "Default is car.";

	public static final String TRAVEL_TIME_ESTIMATION_ALPHA = "travelTimeEstimationAlpha";
	static final String TRAVEL_TIME_ESTIMATION_ALPHA_EXP =
			"Used for OFFLINE estimation of travel times for VrpOptimizer"
					+ " by means of the exponential moving average."
					+ " The weighting decrease, alpha, must be in (0,1]."
					+ " We suggest small values of alpha, e.g. 0.05."
					+ " The averaging starts from the initial travel time estimates. If not provided,"
					+ " the free-speed TTs is used as the initial estimates";

	public static final String TRAVEL_TIME_ESTIMATION_BETA = "travelTimeEstimationBeta";
	static final String TRAVEL_TIME_ESTIMATION_BETA_EXP = "Used for ONLINE estimation of travel times for VrpOptimizer"
			+ " by combining WithinDayTravelTime and DvrpOfflineTravelTimeEstimator."
			+ " The beta coefficient is provided in seconds and should be either 0 (no online estimation)"
			+ " or positive (mixed online-offline estimation)."
			/////
			+ " For 'beta = 0', only the offline estimate is used:"
			+ " 'onlineTT(t) = offlineTT(t)',"
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
			+ " If beta is sufficiently large, 'beta >> 0', only the currently observed TT is used.";
	// In DVRP 'time < currentTime' may only happen for backward path search, a adding proper search termination
	// criterion should prevent this from happening

	@NotBlank
	private String mode = null; // travel mode (passengers'/customers' perspective)

	@Nullable
	private String networkMode = TransportMode.car; // used for building route; null ==> no filtering (routing network equals scenario.network)

	@NotBlank
	private String mobsimMode = TransportMode.car;// used for events throwing and thus calculating travel times, etc.

	@Positive
	@Max(1)
	private double travelTimeEstimationAlpha = 0.05; // [-], 1 ==> TTs from the last iteration only

	@PositiveOrZero
	private double travelTimeEstimationBeta = 0; // [s], 0 ==> only offline TT estimation

	public DvrpConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, MODE_EXP);
		map.put(NETWORK_MODE, NETWORK_MODE_EXP);
		map.put(MOBSIM_MODE, MOBSIM_MODE_EXP);
		map.put(TRAVEL_TIME_ESTIMATION_ALPHA, TRAVEL_TIME_ESTIMATION_ALPHA_EXP);
		map.put(TRAVEL_TIME_ESTIMATION_BETA, TRAVEL_TIME_ESTIMATION_BETA_EXP);
		return map;
	}

	/**
	 * @return -- {@value #MODE_EXP}}
	 */
	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	/**
	 * @param -- {@value #MODE_EXP}
	 */
	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
	}

	/**
	 * @return -- {@value #NETWORK_MODE_EXP}
	 */
	@StringGetter(NETWORK_MODE)
	public String getNetworkMode() {
		return networkMode;
	}

	/**
	 * @param -- {@value #NETWORK_MODE_EXP}
	 */
	@StringSetter(NETWORK_MODE)
	public void setNetworkMode(String networkMode) {
		this.networkMode = networkMode;
	}

	/**
	 * @return -- {@value #MOBSIM_MODE_EXP}
	 */
	@StringGetter(MOBSIM_MODE)
	public String getMobsimMode() {
		return mobsimMode;
	}

	/**
	 * @param -- {@value #MOBSIM_MODE_EXP}
	 */
	@StringSetter(MOBSIM_MODE)
	public void setMobsimMode(String networkMode) {
		this.mobsimMode = networkMode;
	}

	/**
	 * @return -- {@value #TRAVEL_TIME_ESTIMATION_ALPHA_EXP}
	 */
	@StringGetter(TRAVEL_TIME_ESTIMATION_ALPHA)
	public double getTravelTimeEstimationAlpha() {
		return travelTimeEstimationAlpha;
	}

	/**
	 * @value -- {@value #TRAVEL_TIME_ESTIMATION_ALPHA_EXP}
	 */
	@StringSetter(TRAVEL_TIME_ESTIMATION_ALPHA)
	public void setTravelTimeEstimationAlpha(double travelTimeEstimationAlpha) {
		this.travelTimeEstimationAlpha = travelTimeEstimationAlpha;
	}

	/**
	 * @return -- {@value #TRAVEL_TIME_ESTIMATION_BETA_EXP}
	 */
	@StringGetter(TRAVEL_TIME_ESTIMATION_BETA)
	public double getTravelTimeEstimationBeta() {
		return travelTimeEstimationBeta;
	}

	/**
	 * @param -- {@value #TRAVEL_TIME_ESTIMATION_BETA_EXP}
	 */
	@StringSetter(TRAVEL_TIME_ESTIMATION_BETA)
	public void setTravelTimeEstimationBeta(double travelTimeEstimationBeta) {
		this.travelTimeEstimationBeta = travelTimeEstimationBeta;
	}
}
