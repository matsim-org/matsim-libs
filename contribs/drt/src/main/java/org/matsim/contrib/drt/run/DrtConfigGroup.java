/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.run;

import static org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;

import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class DrtConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {
	private static final Logger log = LogManager.getLogger(DrtConfigGroup.class);

	public static final String GROUP_NAME = "drt";

	public static DrtConfigGroup getSingleModeDrtConfig(Config config) {
		Collection<DrtConfigGroup> drtConfigGroups = MultiModeDrtConfigGroup.get(config).getModalElements();
		Preconditions.checkArgument(drtConfigGroups.size() == 1,
				"Supported for only 1 DRT mode in the config. Number of DRT modes: %s", drtConfigGroups.size());
		return drtConfigGroups.iterator().next();
	}

	@Parameter
	@Comment("Mode which will be handled by PassengerEngine and VrpOptimizer (passengers'/customers' perspective)")
	@NotBlank
	public String mode = TransportMode.drt; // travel mode (passengers'/customers' perspective)

	@Parameter
	@Comment("Limit the operation of vehicles to links (of the 'dvrp_routing'"
			+ " network) with 'allowedModes' containing this 'mode'."
			+ " For backward compatibility, the value is set to false by default"
			+ " - this means that the vehicles are allowed to operate on all links of the 'dvrp_routing' network."
			+ " The 'dvrp_routing' is defined by DvrpConfigGroup.networkModes)")
	public boolean useModeFilteredSubnetwork = false;

	@Parameter
	@Comment("Bus stop duration. Must be positive.")
	@Positive
	public double stopDuration = Double.NaN;// seconds

	@Parameter
	@Comment("Max wait time for the bus to come (optimisation constraint).")
	@PositiveOrZero
	public double maxWaitTime = Double.NaN;// seconds

	@Parameter
	@Comment("Defines the slope of the maxTravelTime estimation function (optimisation constraint), i.e. "
			+ "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
			+ "Alpha should not be smaller than 1.")
	@DecimalMin("1.0")
	public double maxTravelTimeAlpha = Double.NaN;// [-]

	@Parameter
	@Comment("Defines the shift of the maxTravelTime estimation function (optimisation constraint), i.e. "
			+ "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
			+ "Beta should not be smaller than 0.")
	@PositiveOrZero
	public double maxTravelTimeBeta = Double.NaN;// [s]

	@Parameter
	@Comment(
			"Defines the maximum allowed absolute detour in seconds of the maxTravelTime estimation function (optimisation constraint), i.e. "
					+ "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
					+ "maxAbsoluteDetour should not be smaller than 0. and should be higher than the offset maxTravelTimeBeta.")
	@PositiveOrZero
	public double maxAbsoluteDetour = Double.POSITIVE_INFINITY;// [s]

	@Parameter
	@Comment("If true, the max travel and wait times of a submitted request"
			+ " are considered hard constraints (the request gets rejected if one of the constraints is violated)."
			+ " If false, the max travel and wait times are considered soft constraints (insertion of a request that"
			+ " violates one of the constraints is allowed, but its cost is increased by additional penalty to make"
			+ " it relatively less attractive). Penalisation of insertions can be customised by injecting a customised"
			+ " InsertionCostCalculator.PenaltyCalculator")
	public boolean rejectRequestIfMaxWaitOrTravelTimeViolated = true;

	@Parameter
	@Comment("If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
			+ "day at the link where it stopped operating the day before. False by default.")
	public boolean changeStartLinkToLastLinkInSchedule = false;

	@Parameter
	@Comment("Idle vehicles return to the nearest of all start links. See: DvrpVehicle.getStartLink()")
	public boolean idleVehiclesReturnToDepots = false;

	public enum OperationalScheme {
		stopbased, door2door, serviceAreaBased
	}

	@Parameter
	@Comment("Operational Scheme, either of door2door, stopbased or serviceAreaBased. door2door by default")
	@NotNull
	public OperationalScheme operationalScheme = OperationalScheme.door2door;

	//TODO consider renaming maxWalkDistance to max access/egress distance (or even have 2 separate params)
	@Parameter
	@Comment(
			"Maximum beeline distance (in meters) to next stop location in stopbased system for access/egress walk leg to/from drt."
					+ " If no stop can be found within this maximum distance will return null (in most cases caught by fallback routing module).")
	@PositiveOrZero // used only for stopbased DRT scheme
	public double maxWalkDistance = Double.MAX_VALUE;// [m];

	@Parameter
	@Comment("An XML file specifying the vehicle fleet."
			+ " The file format according to dvrp_vehicles_v1.dtd"
			+ " If not provided, the vehicle specifications will be created from matsim vehicle file or provided via a custom binding."
			+ " See FleetModule.")
	@Nullable//it is possible to generate a FleetSpecification (instead of reading it from a file)
	public String vehiclesFile = null;

	@Parameter
	@Comment("Stop locations file (transit schedule format, but without lines) for DRT stops. "
			+ "Used only for the stopbased mode")
	@Nullable
	public String transitStopFile = null; // only for stopbased DRT scheme

	@Parameter
	@Comment("Allows to configure a service area per drt mode. Used with serviceArea Operational Scheme")
	@Nullable
	public String drtServiceAreaShapeFile = null; // only for serviceAreaBased DRT scheme

	@Parameter("writeDetailedCustomerStats")
	@Comment("Writes out detailed DRT customer stats in each iteration. True by default.")
	public boolean plotDetailedCustomerStats = true;

	@Parameter
	@Comment("Number of threads used for parallel evaluation of request insertion into existing schedules."
			+ " Scales well up to 4, due to path data provision, the most computationally intensive part,"
			+ " using up to 4 threads."
			+ " Default value is the number of cores available to JVM.")
	@Positive
	public int numberOfThreads = Runtime.getRuntime().availableProcessors();

	@Parameter
	@Comment("Store planned unshared drt route as a link sequence")
	public boolean storeUnsharedPath = false; // If true, the planned unshared path is stored and exported in plans

	@PositiveOrZero
	public double advanceRequestPlanningHorizon = 0; // beta-feature; planning horizon for advance (prebooked) requests

	@NotNull
	private DrtInsertionSearchParams drtInsertionSearchParams;

	@Nullable
	private DrtZonalSystemParams zonalSystemParams;

	@Nullable
	private RebalancingParams rebalancingParams;

	@Nullable
	private DrtFareParams drtFareParams;

	@Nullable
	private DrtSpeedUpParams drtSpeedUpParams;

	@Nullable
	private DrtRequestInsertionRetryParams drtRequestInsertionRetryParams;

	public DrtConfigGroup() {
		super(GROUP_NAME);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//rebalancing (optional)
		addDefinition(RebalancingParams.SET_NAME, RebalancingParams::new, () -> rebalancingParams,
				params -> rebalancingParams = (RebalancingParams)params);

		//zonal system (optional)
		addDefinition(DrtZonalSystemParams.SET_NAME, DrtZonalSystemParams::new, () -> zonalSystemParams,
				params -> zonalSystemParams = (DrtZonalSystemParams)params);

		//insertion search params (one of: extensive, selective)
		addDefinition(ExtensiveInsertionSearchParams.SET_NAME, ExtensiveInsertionSearchParams::new,
				() -> drtInsertionSearchParams,
				params -> drtInsertionSearchParams = (ExtensiveInsertionSearchParams)params);
		addDefinition(SelectiveInsertionSearchParams.SET_NAME, SelectiveInsertionSearchParams::new,
				() -> drtInsertionSearchParams,
				params -> drtInsertionSearchParams = (SelectiveInsertionSearchParams)params);

		//drt fare (optional)
		addDefinition(DrtFareParams.SET_NAME, DrtFareParams::new, () -> drtFareParams,
				params -> drtFareParams = (DrtFareParams)params);

		//drt speedup (optional)
		addDefinition(DrtSpeedUpParams.SET_NAME, DrtSpeedUpParams::new, () -> drtSpeedUpParams,
				params -> drtSpeedUpParams = (DrtSpeedUpParams)params);

		//request retry handling (optional)
		addDefinition(DrtRequestInsertionRetryParams.SET_NAME, DrtRequestInsertionRetryParams::new,
				() -> drtRequestInsertionRetryParams,
				params -> drtRequestInsertionRetryParams = (DrtRequestInsertionRetryParams)params);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (config.qsim().getEndTime().isUndefined()
				|| config.qsim().getSimEndtimeInterpretation() != EndtimeInterpretation.onlyUseEndtime) {
			// Not an issue if all request rejections are immediate (i.e. happen during request submission)
			log.warn("qsim.endTime should be specified and qsim.simEndtimeInterpretation should be 'onlyUseEndtime'"
					+ " if postponed request rejection is allowed. Otherwise, rejected passengers"
					+ " (who are stuck endlessly waiting for a DRT vehicle) will prevent QSim from stopping."
					+ " Keep also in mind that not setting an end time may result in agents "
					+ "attempting to travel without vehicles being available.");
		}

		Verify.verify(maxWaitTime >= stopDuration, "maxWaitTime must not be smaller than stopDuration");

		Verify.verify(operationalScheme != OperationalScheme.stopbased || transitStopFile != null,
				"transitStopFile must not be null when operationalScheme is " + OperationalScheme.stopbased);

		Verify.verify(operationalScheme != OperationalScheme.serviceAreaBased || drtServiceAreaShapeFile != null,
				"drtServiceAreaShapeFile must not be null when operationalScheme is "
						+ OperationalScheme.serviceAreaBased);

		Verify.verify(numberOfThreads <= Runtime.getRuntime().availableProcessors(),
				"numberOfThreads is higher than the number of logical cores available to JVM");

		if (config.global().getNumberOfThreads() < numberOfThreads) {
			log.warn("Consider increasing global.numberOfThreads to at least the value of drt.numberOfThreads"
					+ " in order to speed up the DRT route update during the replanning phase.");
		}

		Verify.verify(getParameterSets(MinCostFlowRebalancingStrategyParams.SET_NAME).size() <= 1,
				"More than one rebalancing parameter sets is specified");

		if (useModeFilteredSubnetwork) {
			DvrpModeRoutingNetworkModule.checkUseModeFilteredSubnetworkAllowed(config, mode);
		}
	}

	@Override
	public String getMode() {
		return mode;
	}

	public DrtInsertionSearchParams getDrtInsertionSearchParams() {
		return drtInsertionSearchParams;
	}

	public Optional<DrtZonalSystemParams> getZonalSystemParams() {
		return Optional.ofNullable(zonalSystemParams);
	}

	public Optional<RebalancingParams> getRebalancingParams() {
		return Optional.ofNullable(rebalancingParams);
	}

	public Optional<DrtFareParams> getDrtFareParams() {
		return Optional.ofNullable(drtFareParams);
	}

	public Optional<DrtSpeedUpParams> getDrtSpeedUpParams() {
		return Optional.ofNullable(drtSpeedUpParams);
	}

	public Optional<DrtRequestInsertionRetryParams> getDrtRequestInsertionRetryParams() {
		return Optional.ofNullable(drtRequestInsertionRetryParams);
	}

	/**
	 * Convenience method that brings syntax closer to syntax in, e.g., {@link PlansCalcRouteConfigGroup} or {@link PlanCalcScoreConfigGroup}
	 */
	public final void addDrtInsertionSearchParams(final DrtInsertionSearchParams pars) {
		addParameterSet(pars);
	}
}
