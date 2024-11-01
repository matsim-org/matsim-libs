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

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.dynagent.run.DynQSimConfigConsistencyChecker;
import org.matsim.contrib.zone.skims.DvrpTravelTimeMatrixParams;
import org.matsim.core.config.Config;

import javax.annotation.Nullable;
import java.util.Set;

public class DvrpConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets {
	private static final Logger log = LogManager.getLogger(DvrpConfigGroup.class);

	public static final String GROUP_NAME = "dvrp";

	@SuppressWarnings("deprecation")
	public static DvrpConfigGroup get(Config config) {
		return (DvrpConfigGroup)config.getModule(GROUP_NAME);// will fail if not in the config
	}

	@Parameter
	@Comment("Set of modes of which the network will be used for DVRP travel time "
			+ "estimation and routing DVRP vehicles. "
			+ "Each specific DVRP mode may use a subnetwork of this network for routing vehicles (e.g. DRT buses "
			+ "travelling only along a specified links or serving a limited area). "
			+ "Default is \"car\" (i.e. single-element set of modes), i.e. the car network is used. "
			+ "Empty value \"\" (i.e. empty set of modes) means no network filtering, i.e. "
			+ "the original scenario.network is used")
	// used for building route; empty ==> no filtering (routing network equals scenario.network)
	@NotNull
	public Set<String> networkModes = Set.of(TransportMode.car);

	@Parameter
	@Comment("Mode of which the network will be used for throwing events and hence calculating travel times. "
			+ "Default is car.")
	@NotBlank
	public String mobsimMode = TransportMode.car;// used for events throwing and thus calculating travel times, etc.

	@Parameter
	@Comment("Used for OFFLINE estimation of travel times for VrpOptimizer"
			+ " by means of the exponential moving average."
			+ " The weighting decrease, alpha, must be in [0,1]."
			+ " We suggest small values of alpha, e.g. 0.05."
			+ " The averaging starts from the initial travel time estimates. If not provided,"
			+ " the free-speed TTs is used as the initial estimates. If alpha is set to 0, the initial"
			+ " travel times stay fixed.")
	@PositiveOrZero
	@DecimalMax("1.0")
	public double travelTimeEstimationAlpha = 0.05; // [-], 1 ==> TTs from the last iteration only, 0 ==> initial TTs only

	@Parameter
	@Comment(""
			+ "Used for ONLINE estimation of travel times for VrpOptimizer"
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
			+ " If beta is sufficiently large, 'beta >> 0', only the currently observed TT is used.")
	// In DVRP 'time < currentTime' may only happen for backward path search, a adding proper search termination
	// criterion should prevent this from happening
	@PositiveOrZero
	public double travelTimeEstimationBeta = 0; // [s], 0 ==> only offline TT estimation

	@Parameter
	@Comment("File containing the initial link travel time estimates. Ignored if null")
	@Nullable
	public String initialTravelTimesFile = null;

	@Nullable
	private DvrpTravelTimeMatrixParams travelTimeMatrixParams;

	public DvrpConfigGroup() {
		super(GROUP_NAME);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//travel time matrix (optional)
		addDefinition(DvrpTravelTimeMatrixParams.SET_NAME, DvrpTravelTimeMatrixParams::new,
				() -> travelTimeMatrixParams, params -> travelTimeMatrixParams = (DvrpTravelTimeMatrixParams)params);
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
		if (!config.eventsManager().getSynchronizeOnSimSteps()) {
			throw new RuntimeException("Synchronization on sim steps is required");
		}
		if(initialTravelTimesFile == null && travelTimeEstimationAlpha == 0.0) {
			throw new RuntimeException("Initial travel times file is required if travel times should not be updated.");
		}
		if(travelTimeEstimationAlpha == 0.0 && travelTimeEstimationBeta > 0) {
			throw new RuntimeException("Online estimation beta should be 0 if travel time should not be updated.");
		}
	}

	public DvrpTravelTimeMatrixParams getTravelTimeMatrixParams() {
		if (travelTimeMatrixParams == null) {
			addParameterSet(new DvrpTravelTimeMatrixParams());
		}
		return travelTimeMatrixParams;
	}
}
