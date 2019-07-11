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

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dynagent.run.DynQSimConfigConsistencyChecker;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.StringUtils;

import com.google.common.collect.ImmutableSet;

public final class DvrpConfigGroup extends ReflectiveConfigGroup {
	private static final Logger log = Logger.getLogger(DvrpConfigGroup.class);

	public static final String GROUP_NAME = "dvrp";

	@SuppressWarnings("deprecation")
	public static DvrpConfigGroup get(Config config) {
		return (DvrpConfigGroup)config.getModule(GROUP_NAME);// will fail if not in the config
	}

	public static final String NETWORK_MODES = "networkModes";
	static final String NETWORK_MODES_EXP = "Set of modes of which the network will be used for DVRP travel time "
			+ "estimation and routing DVRP vehicles. "
			+ "Each specific DVRP mode may use a subnetwork of this network for routing vehicles (e.g. DRT buses "
			+ "travelling only along a specified links or serving a limited area). "
			+ "Default is \"car\" (i.e. single-element set of modes), i.e. the car network is used. "
			+ "Empty value \"\" (i.e. empty set of modes) means no network filtering, i.e. "
			+ "the original scenario.network is used";

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

	// used for building route; empty ==> no filtering (routing network equals scenario.network)
	@NotNull
	private ImmutableSet<String> networkModes = ImmutableSet.of(TransportMode.car);

	@NotBlank
	private String mobsimMode = TransportMode.car;// used for events throwing and thus calculating travel times, etc.

	@Positive
	@DecimalMax("1.0")
	private double travelTimeEstimationAlpha = 0.05; // [-], 1 ==> TTs from the last iteration only

	@PositiveOrZero
	private double travelTimeEstimationBeta = 0; // [s], 0 ==> only offline TT estimation

	public DvrpConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		new DynQSimConfigConsistencyChecker().checkConsistency(config);

		if (!config.qsim().isInsertingWaitingVehiclesBeforeDrivingVehicles()) {
			// Typically, vrp paths are calculated from startLink to endLink
			// (not from startNode to endNode). That requires making some assumptions
			// on how much time travelling on the first and last links takes.
			// The current implementation assumes:
			// (a) free-flow travelling on the last link, which is actually the case in QSim, and
			// (b) a 1-second stay on the first link (spent on moving over the first node).
			// The latter expectation is assumes that departing vehicles must be inserted before driving ones
			// (though that still does not guarantee 1-second stay since the vehicle may need to wait if the next
			// link is fully congested)
			log.warn(" 'QSim.insertingWaitingVehiclesBeforeDrivingVehicles' should be true in order to get"
					+ " more precise travel time estimates. See comments in DvrpConfigGroup.checkConsistency()");
		}
		if (config.qsim().isRemoveStuckVehicles()) {
			throw new RuntimeException("Stuck DynAgents cannot be removed from simulation");
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(NETWORK_MODES, NETWORK_MODES_EXP);
		map.put(MOBSIM_MODE, MOBSIM_MODE_EXP);
		map.put(TRAVEL_TIME_ESTIMATION_ALPHA, TRAVEL_TIME_ESTIMATION_ALPHA_EXP);
		map.put(TRAVEL_TIME_ESTIMATION_BETA, TRAVEL_TIME_ESTIMATION_BETA_EXP);
		return map;
	}

	/**
	 * @return {@value #NETWORK_MODES_EXP}
	 */
	@StringGetter(NETWORK_MODES)
	public String getNetworkModesAsString() {
		return String.join(",", networkModes);
	}

	public ImmutableSet<String> getNetworkModes() {
		return networkModes;
	}

	/**
	 * @param networkModesString {@value #NETWORK_MODES_EXP}
	 */
	@StringSetter(NETWORK_MODES)
	public void setNetworkModesAsString(String networkModesString) {
		this.networkModes = ImmutableSet.copyOf(StringUtils.explode(networkModesString, ','));
	}

	public void setNetworkModes(ImmutableSet<String> networkModes) {
		this.networkModes = networkModes;
	}

	/**
	 * @return {@value #MOBSIM_MODE_EXP}
	 */
	@StringGetter(MOBSIM_MODE)
	public String getMobsimMode() {
		return mobsimMode;
	}

	/**
	 * @param networkMode {@value #MOBSIM_MODE_EXP}
	 */
	@StringSetter(MOBSIM_MODE)
	public void setMobsimMode(String networkMode) {
		this.mobsimMode = networkMode;
	}

	/**
	 * @return {@value #TRAVEL_TIME_ESTIMATION_ALPHA_EXP}
	 */
	@StringGetter(TRAVEL_TIME_ESTIMATION_ALPHA)
	public double getTravelTimeEstimationAlpha() {
		return travelTimeEstimationAlpha;
	}

	/**
	 * @param travelTimeEstimationAlpha {@value #TRAVEL_TIME_ESTIMATION_ALPHA_EXP}
	 */
	@StringSetter(TRAVEL_TIME_ESTIMATION_ALPHA)
	public void setTravelTimeEstimationAlpha(double travelTimeEstimationAlpha) {
		this.travelTimeEstimationAlpha = travelTimeEstimationAlpha;
	}

	/**
	 * @return {@value #TRAVEL_TIME_ESTIMATION_BETA_EXP}
	 */
	@StringGetter(TRAVEL_TIME_ESTIMATION_BETA)
	public double getTravelTimeEstimationBeta() {
		return travelTimeEstimationBeta;
	}

	/**
	 * @param travelTimeEstimationBeta {@value #TRAVEL_TIME_ESTIMATION_BETA_EXP}
	 */
	@StringSetter(TRAVEL_TIME_ESTIMATION_BETA)
	public void setTravelTimeEstimationBeta(double travelTimeEstimationBeta) {
		this.travelTimeEstimationBeta = travelTimeEstimationBeta;
	}
}
