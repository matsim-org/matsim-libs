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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.drt.analysis.zonal.DrtZoneSystemParams;
import org.matsim.contrib.drt.estimator.DrtEstimatorParams;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.constraints.DefaultDrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.repeatedselective.RepeatedSelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.prebooking.PrebookingParams;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.load.DvrpLoadParams;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup.Parameter;
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

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
	@Comment("Caches the travel time matrix data into a binary file. If the file exists, the matrix will be read from the file, if not, the file will be created.")
	public String travelTimeMatrixCachePath = null;

	@Parameter
	@Comment("Minimum vehicle stop duration. Must be positive.")
	@Positive
	public double stopDuration = Double.NaN;// seconds

	@Parameter
	@Comment("If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
			+ "day at the link where it stopped operating the day before. False by default.")
	public boolean changeStartLinkToLastLinkInSchedule = false;

	@Parameter
	@Comment("Idle vehicles return to the nearest of all start links. See: DvrpVehicle.getStartLink()")
	public boolean idleVehiclesReturnToDepots = false;

	@Parameter
	@Comment("Specifies the duration (seconds) a vehicle needs to be idle in order to get send back to the depot." +
		"Please be aware, that returnToDepotEvaluationInterval describes the minimal time a vehicle will be idle before it gets send back to depot.")
	public double returnToDepotTimeout = 60;

	@Parameter
	@Comment("Specifies the time interval (seconds) a vehicle gets evaluated to be send back to depot.")
	public double returnToDepotEvaluationInterval = 60;

	public enum OperationalScheme {
		stopbased, door2door, serviceAreaBased
	}

	@Parameter
	@Comment("Operational Scheme, either of door2door, stopbased or serviceAreaBased. door2door by default")
	@NotNull
	public OperationalScheme operationalScheme = OperationalScheme.door2door;

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

	public enum SimulationType {
		fullSimulation, estimateAndTeleport
	}

	@Parameter
	@Comment("Whether full simulation drt is employed")
	public SimulationType simulationType = SimulationType.fullSimulation;

	@Parameter
	@Comment("Defines whether routes along schedules are updated regularly")
	public boolean updateRoutes = false;

	@NotNull
	private DrtInsertionSearchParams drtInsertionSearchParams;

	@NotNull
	private DrtOptimizationConstraintsParams drtOptimizationConstraintsParams;

	@Nullable
	private DrtZoneSystemParams zonalSystemParams;

	@Nullable
	private RebalancingParams rebalancingParams;

	@Nullable
	private DrtFareParams drtFareParams;

	@Nullable
	private DrtSpeedUpParams drtSpeedUpParams;

	@Nullable
	private PrebookingParams prebookingParams;

	@Nullable
	private DrtEstimatorParams drtEstimatorParams = new DrtEstimatorParams();

	public DvrpLoadParams loadParams = new DvrpLoadParams();

	@Nullable
	private DrtRequestInsertionRetryParams drtRequestInsertionRetryParams;

	public DrtConfigGroup() {
		this(DefaultDrtOptimizationConstraintsSet::new);
	}

	public DrtConfigGroup(Supplier<DrtOptimizationConstraintsSet> constraintsSetSupplier) {
		super(GROUP_NAME);
		initSingletonParameterSets(constraintsSetSupplier);
	}

	private void initSingletonParameterSets(Supplier<DrtOptimizationConstraintsSet> constraintsSetSupplier) {

		//optimization constraints (mandatory)
		addDefinition(DrtOptimizationConstraintsParams.SET_NAME, () -> new DrtOptimizationConstraintsParams(constraintsSetSupplier),
				() -> drtOptimizationConstraintsParams,
				params -> drtOptimizationConstraintsParams = (DrtOptimizationConstraintsParams) params);

		//rebalancing (optional)
		addDefinition(RebalancingParams.SET_NAME, RebalancingParams::new, () -> rebalancingParams,
				params -> rebalancingParams = (RebalancingParams)params);

		//zonal system (optional)
		addDefinition(DrtZoneSystemParams.SET_NAME, DrtZoneSystemParams::new, () -> zonalSystemParams,
				params -> zonalSystemParams = (DrtZoneSystemParams)params);

		//insertion search params (one of: extensive, selective, repeated selective)
		addDefinition(ExtensiveInsertionSearchParams.SET_NAME, ExtensiveInsertionSearchParams::new,
				() -> drtInsertionSearchParams,
				params -> drtInsertionSearchParams = (ExtensiveInsertionSearchParams)params);
		addDefinition(SelectiveInsertionSearchParams.SET_NAME, SelectiveInsertionSearchParams::new,
				() -> drtInsertionSearchParams,
				params -> drtInsertionSearchParams = (SelectiveInsertionSearchParams)params);
		addDefinition(RepeatedSelectiveInsertionSearchParams.SET_NAME, RepeatedSelectiveInsertionSearchParams::new,
				() -> drtInsertionSearchParams,
				params -> drtInsertionSearchParams = (RepeatedSelectiveInsertionSearchParams)params);

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

		//prebooking (optional)
		addDefinition(PrebookingParams.SET_NAME, PrebookingParams::new,
				() -> prebookingParams,
				params -> prebookingParams = (PrebookingParams)params);

		// estimator (optional)
		addDefinition(DrtEstimatorParams.SET_NAME, DrtEstimatorParams::new,
			() -> drtEstimatorParams,
			params -> drtEstimatorParams = (DrtEstimatorParams) params);

		// load
		addDefinition(DvrpLoadParams.SET_NAME, DvrpLoadParams::new,
			() -> loadParams,
			params -> loadParams = (DvrpLoadParams) params);
	}

	/**
	 * for backwards compatibility with old drt config groups
	 */
	public void handleAddUnknownParam(final String paramName, final String value) {
		switch (paramName) {
			case "maxWaitTime":
			case "maxTravelTimeAlpha":
			case "maxTravelTimeBeta":
			case "maxAbsoluteDetour":
			case "maxDetourAlpha":
			case "maxDetourBeta":
			case "maxAllowedPickupDelay":
			case "rejectRequestIfMaxWaitOrTravelTimeViolated":
			case "maxWalkDistance":
				addOrGetDrtOptimizationConstraintsParams().addOrGetDefaultDrtOptimizationConstraintsSet().addParam(paramName, value);
            	break;
            default:
                super.handleAddUnknownParam(paramName, value);
        }
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

		List<DrtOptimizationConstraintsSet> drtOptimizationConstraintsSets = addOrGetDrtOptimizationConstraintsParams().getDrtOptimizationConstraintsSets();
		for (DrtOptimizationConstraintsSet constraintsSet : drtOptimizationConstraintsSets) {
			Verify.verify(constraintsSet.maxWaitTime >= stopDuration,
					"maxWaitTime must not be smaller than stopDuration");
		}

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

		if (this.idleVehiclesReturnToDepots && this.returnToDepotTimeout < this.returnToDepotEvaluationInterval) {
			log.warn("idleVehiclesReturnToDepots is active and returnToDepotTimeout < returnToDepotEvaluationInterval. " +
				"Vehicles will be send back to depot after {} seconds",returnToDepotEvaluationInterval);
		}

		Verify.verify(getParameterSets(MinCostFlowRebalancingStrategyParams.SET_NAME).size() <= 1,
				"More than one rebalancing parameter sets is specified");

		if (useModeFilteredSubnetwork) {
			DvrpModeRoutingNetworkModule.checkUseModeFilteredSubnetworkAllowed(config, mode);
		}

		if (simulationType == SimulationType.estimateAndTeleport) {
			Verify.verify(drtSpeedUpParams == null, "Simulation type is estimateAndTeleport, but drtSpeedUpParams is set. " +
				"Please remove drtSpeedUpParams from the config, as these two functionalities are not compatible.");
		}
	}

	@Override
	public String getMode() {
		return mode;
	}



	public DrtInsertionSearchParams getDrtInsertionSearchParams() {
		return drtInsertionSearchParams;
	}

	public DrtOptimizationConstraintsParams addOrGetDrtOptimizationConstraintsParams() {
		if (drtOptimizationConstraintsParams == null) {
			DrtOptimizationConstraintsParams params = new DrtOptimizationConstraintsParams();
			this.addParameterSet(params);
		}
		return drtOptimizationConstraintsParams;
	}

	public Optional<DrtZoneSystemParams> getZonalSystemParams() {
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

	public Optional<PrebookingParams> getPrebookingParams() {
		return Optional.ofNullable(prebookingParams);
	}

	public Optional<DrtEstimatorParams> getDrtEstimatorParams() {
		return Optional.ofNullable(drtEstimatorParams);
	}

	/**
	 * Convenience method that brings syntax closer to syntax in, e.g., {@link RoutingConfigGroup} or {@link ScoringConfigGroup}
	 *
	 * @deprecated -- use {@link #setDrtInsertionSearchParams(DrtInsertionSearchParams) instead}
	 */
	@Deprecated
	public final void addDrtInsertionSearchParams(final DrtInsertionSearchParams pars) {
		addParameterSet(pars);
	}
	/**
	 * Convenience method that brings syntax closer to syntax in, e.g., {@link RoutingConfigGroup} or {@link ScoringConfigGroup}
	 */
	public final void setDrtInsertionSearchParams(final DrtInsertionSearchParams pars) {
		addParameterSet(pars);
	}
}
