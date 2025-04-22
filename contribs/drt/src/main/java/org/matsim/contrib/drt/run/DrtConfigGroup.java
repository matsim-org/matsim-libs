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

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.common.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.contrib.common.zones.ZoneSystemParams;
import org.matsim.contrib.common.zones.systems.geom_free_zones.GeometryFreeZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.GISFileZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.h3.H3GridZoneSystemParams;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystemParams;
import org.matsim.contrib.drt.estimator.DrtEstimatorParams;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsParams;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSet;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
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
import org.matsim.core.config.groups.QSimConfigGroup.EndtimeInterpretation;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.config.groups.ScoringConfigGroup;

import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

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
	private String mode = TransportMode.drt; // travel mode (passengers'/customers' perspective)

	@Parameter
	@Comment("Limit the operation of vehicles to links (of the 'dvrp_routing'"
			+ " network) with 'allowedModes' containing this 'mode'."
			+ " For backward compatibility, the value is set to false by default"
			+ " - this means that the vehicles are allowed to operate on all links of the 'dvrp_routing' network."
			+ " The 'dvrp_routing' is defined by DvrpConfigGroup.networkModes)")
	private boolean useModeFilteredSubnetwork = false;

	@Parameter
	@Comment("Caches the travel time matrix data into a binary file. If the file exists, the matrix will be read from the file, if not, the file will be created.")
	private String travelTimeMatrixCachePath = null;

	@Parameter
	@Comment("Minimum vehicle stop duration. Must be positive.")
	@Positive
	private double stopDuration = Double.NaN;// seconds

	@Parameter
	@Comment("If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
			+ "day at the link where it stopped operating the day before. False by default.")
	private boolean changeStartLinkToLastLinkInSchedule = false;

	@Parameter
	@Comment("Idle vehicles return to the nearest of all start links. See: DvrpVehicle.getStartLink()")
	private boolean idleVehiclesReturnToDepots = false;

	@Parameter
	@Comment("Specifies the duration (seconds) a vehicle needs to be idle in order to get send back to the depot." +
		"Please be aware, that returnToDepotEvaluationInterval describes the minimal time a vehicle will be idle before it gets send back to depot.")
	private double returnToDepotTimeout = 60;

	@Parameter
	@Comment("Specifies the time interval (seconds) a vehicle gets evaluated to be send back to depot.")
	private double returnToDepotEvaluationInterval = 60;

	public @NotNull OperationalScheme getOperationalScheme() {
		return operationalScheme;
	}

	public void setOperationalScheme(@NotNull OperationalScheme operationalScheme) {
		this.operationalScheme = operationalScheme;
	}

	@Nullable
	public String getVehiclesFile() {
		return vehiclesFile;
	}

	public void setVehiclesFile(@Nullable String vehiclesFile) {
		this.vehiclesFile = vehiclesFile;
	}

	@Nullable
	public String getTransitStopFile() {
		return transitStopFile;
	}

	public void setTransitStopFile(@Nullable String transitStopFile) {
		this.transitStopFile = transitStopFile;
	}

	@Nullable
	public String getDrtServiceAreaShapeFile() {
		return drtServiceAreaShapeFile;
	}

	public void setDrtServiceAreaShapeFile(@Nullable String drtServiceAreaShapeFile) {
		this.drtServiceAreaShapeFile = drtServiceAreaShapeFile;
	}

	public boolean isPlotDetailedCustomerStats() {
		return plotDetailedCustomerStats;
	}

	public void setPlotDetailedCustomerStats(boolean plotDetailedCustomerStats) {
		this.plotDetailedCustomerStats = plotDetailedCustomerStats;
	}

	@Positive
	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(@Positive int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
	}

	public boolean isStoreUnsharedPath() {
		return storeUnsharedPath;
	}

	public void setStoreUnsharedPath(boolean storeUnsharedPath) {
		this.storeUnsharedPath = storeUnsharedPath;
	}

	public SimulationType getSimulationType() {
		return simulationType;
	}

	public void setSimulationType(SimulationType simulationType) {
		this.simulationType = simulationType;
	}

	public boolean isUpdateRoutes() {
		return updateRoutes;
	}

	public void setUpdateRoutes(boolean updateRoutes) {
		this.updateRoutes = updateRoutes;
	}

	public enum OperationalScheme {
		stopbased, door2door, serviceAreaBased
	}

	@Parameter
	@Comment("Operational Scheme, either of door2door, stopbased or serviceAreaBased. door2door by default")
	@NotNull
	private OperationalScheme operationalScheme = OperationalScheme.door2door;

	@Parameter
	@Comment("An XML file specifying the vehicle fleet."
			+ " The file format according to dvrp_vehicles_v1.dtd"
			+ " If not provided, the vehicle specifications will be created from matsim vehicle file or provided via a custom binding."
			+ " See FleetModule.")
	@Nullable//it is possible to generate a FleetSpecification (instead of reading it from a file)
	private String vehiclesFile = null;

	@Parameter
	@Comment("Stop locations file (transit schedule format, but without lines) for DRT stops. "
			+ "Used only for the stopbased mode")
	@Nullable
	private String transitStopFile = null; // only for stopbased DRT scheme

	@Parameter
	@Comment("Allows to configure a service area per drt mode. Used with serviceArea Operational Scheme")
	@Nullable
	private String drtServiceAreaShapeFile = null; // only for serviceAreaBased DRT scheme

	@Parameter("writeDetailedCustomerStats")
	@Comment("Writes out detailed DRT customer stats in each iteration. True by default.")
	private boolean plotDetailedCustomerStats = true;

	@Parameter
	@Comment("Number of threads used for parallel evaluation of request insertion into existing schedules."
			+ " Scales well up to 4, due to path data provision, the most computationally intensive part,"
			+ " using up to 4 threads."
			+ " Default value is the number of cores available to JVM.")
	@Positive
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	@Parameter
	@Comment("Store planned unshared drt route as a link sequence")
	private boolean storeUnsharedPath = false; // If true, the planned unshared path is stored and exported in plans

	public enum SimulationType {
		fullSimulation, estimateAndTeleport
	}

	@Parameter
	@Comment("Whether full simulation drt is employed")
	private SimulationType simulationType = SimulationType.fullSimulation;

	@Parameter
	@Comment("Defines whether routes along schedules are updated regularly")
	private boolean updateRoutes = false;

	@NotNull
	private DrtInsertionSearchParams drtInsertionSearchParams;

	@NotNull
	private DrtOptimizationConstraintsParams drtOptimizationConstraintsParams;

	@Nullable
	private RebalancingParams rebalancingParams;

	@Nullable
	private DrtFareParams drtFareParams;

	@Nullable
	private DrtSpeedUpParams drtSpeedUpParams;

	@Nullable
	private PrebookingParams prebookingParams;

	@Nullable
	private DrtEstimatorParams drtEstimatorParams;

	@Nullable
	private DvrpLoadParams loadParams;

	@Nullable
	private DrtRequestInsertionRetryParams drtRequestInsertionRetryParams;

	private ZoneSystemParams analysisZoneSystemParams;

	public DrtConfigGroup() {
		this(DrtOptimizationConstraintsSetImpl::new);
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

		addDefinition(SquareGridZoneSystemParams.SET_NAME, SquareGridZoneSystemParams::new,
				() -> analysisZoneSystemParams,
				params -> analysisZoneSystemParams = (SquareGridZoneSystemParams)params);

		addDefinition(GISFileZoneSystemParams.SET_NAME, GISFileZoneSystemParams::new,
				() -> analysisZoneSystemParams,
				params -> analysisZoneSystemParams = (GISFileZoneSystemParams)params);

		addDefinition(H3GridZoneSystemParams.SET_NAME, H3GridZoneSystemParams::new,
				() -> analysisZoneSystemParams,
				params -> analysisZoneSystemParams = (H3GridZoneSystemParams)params);

		addDefinition(GeometryFreeZoneSystemParams.SET_NAME, GeometryFreeZoneSystemParams::new,
				() -> analysisZoneSystemParams,
				params -> analysisZoneSystemParams = (GeometryFreeZoneSystemParams)params);

		addDefinition(ZonalSystemWrapper.SET_NAME, ZonalSystemWrapper::new,
				() -> analysisZoneSystemParams,
				params -> {
					ZoneSystemParams delegate = ((ZonalSystemWrapper) params).delegate;
					super.addParameterSet(delegate);
					params.removeParameterSet(delegate);
                });
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
			Verify.verify(constraintsSet.maxWaitTime >= getStopDuration(),
					"maxWaitTime must not be smaller than stopDuration");
		}

		Verify.verify(getOperationalScheme() != OperationalScheme.stopbased || getTransitStopFile() != null,
				"transitStopFile must not be null when operationalScheme is " + OperationalScheme.stopbased);

		Verify.verify(getOperationalScheme() != OperationalScheme.serviceAreaBased || getDrtServiceAreaShapeFile() != null,
				"drtServiceAreaShapeFile must not be null when operationalScheme is "
						+ OperationalScheme.serviceAreaBased);

		Verify.verify(getNumberOfThreads() <= Runtime.getRuntime().availableProcessors(),
				"numberOfThreads is higher than the number of logical cores available to JVM");

		if (config.global().getNumberOfThreads() < getNumberOfThreads()) {
			log.warn("Consider increasing global.numberOfThreads to at least the value of drt.numberOfThreads"
					+ " in order to speed up the DRT route update during the replanning phase.");
		}

		if (this.isIdleVehiclesReturnToDepots() && this.getReturnToDepotTimeout() < this.getReturnToDepotEvaluationInterval()) {
			log.warn("idleVehiclesReturnToDepots is active and returnToDepotTimeout < returnToDepotEvaluationInterval. " +
				"Vehicles will be send back to depot after {} seconds", getReturnToDepotEvaluationInterval());
		}

		Verify.verify(getParameterSets(MinCostFlowRebalancingStrategyParams.SET_NAME).size() <= 1,
				"More than one rebalancing parameter sets is specified");

		if (isUseModeFilteredSubnetwork()) {
			DvrpModeRoutingNetworkModule.checkUseModeFilteredSubnetworkAllowed(config, getMode());
		}

		if (getSimulationType() == SimulationType.estimateAndTeleport) {
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

	public DvrpLoadParams addOrGetLoadParams() {
		if(this.loadParams == null) {
			this.addParameterSet(new DvrpLoadParams());
		}
		return this.loadParams;
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

	public void setMode(@NotBlank String mode) {
		this.mode = mode;
	}

	public boolean isUseModeFilteredSubnetwork() {
		return useModeFilteredSubnetwork;
	}

	public void setUseModeFilteredSubnetwork(boolean useModeFilteredSubnetwork) {
		this.useModeFilteredSubnetwork = useModeFilteredSubnetwork;
	}

	public String getTravelTimeMatrixCachePath() {
		return travelTimeMatrixCachePath;
	}

	public void setTravelTimeMatrixCachePath(String travelTimeMatrixCachePath) {
		this.travelTimeMatrixCachePath = travelTimeMatrixCachePath;
	}

	@Positive
	public double getStopDuration() {
		return stopDuration;
	}

	public void setStopDuration(@Positive double stopDuration) {
		this.stopDuration = stopDuration;
	}

	public boolean isChangeStartLinkToLastLinkInSchedule() {
		return changeStartLinkToLastLinkInSchedule;
	}

	public void setChangeStartLinkToLastLinkInSchedule(boolean changeStartLinkToLastLinkInSchedule) {
		this.changeStartLinkToLastLinkInSchedule = changeStartLinkToLastLinkInSchedule;
	}

	public boolean isIdleVehiclesReturnToDepots() {
		return idleVehiclesReturnToDepots;
	}

	public void setIdleVehiclesReturnToDepots(boolean idleVehiclesReturnToDepots) {
		this.idleVehiclesReturnToDepots = idleVehiclesReturnToDepots;
	}

	public double getReturnToDepotTimeout() {
		return returnToDepotTimeout;
	}

	public void setReturnToDepotTimeout(double returnToDepotTimeout) {
		this.returnToDepotTimeout = returnToDepotTimeout;
	}

	public double getReturnToDepotEvaluationInterval() {
		return returnToDepotEvaluationInterval;
	}

	public void setReturnToDepotEvaluationInterval(double returnToDepotEvaluationInterval) {
		this.returnToDepotEvaluationInterval = returnToDepotEvaluationInterval;
	}

	public ZoneSystemParams addOrGetAnalysisZoneSystemParams() {
		if (analysisZoneSystemParams == null) {
			ZoneSystemParams params = new SquareGridZoneSystemParams();
			this.addParameterSet(params);
		} else if(analysisZoneSystemParams instanceof ZonalSystemWrapper zonalSystemWrapper) {
			// for backwards compatibility
			return zonalSystemWrapper.delegate;
		}
		return analysisZoneSystemParams;
	}

	/** required for backwards compatibility. Remove at some later point (introduced during code sprint March '25, nkuehnel)
	 only works as long as empty param sets are not written out.
	 Old config formats should automatically be written in the new format (see ReadOldConfigTest)*/
	@Deprecated
	private final class ZonalSystemWrapper extends ZoneSystemParams {

		private final static String SET_NAME = "zonalSystem";
		private ZoneSystemParams delegate;

		public ZonalSystemWrapper() {
			super(SET_NAME);
			initSingletonDefs();
		}

		private void initSingletonDefs() {
			addDefinition(SquareGridZoneSystemParams.SET_NAME, SquareGridZoneSystemParams::new,
					() -> delegate,
					params -> delegate = (SquareGridZoneSystemParams)params);

			addDefinition(GISFileZoneSystemParams.SET_NAME, GISFileZoneSystemParams::new,
					() -> delegate,
					params -> delegate = (GISFileZoneSystemParams)params);

			addDefinition(H3GridZoneSystemParams.SET_NAME, H3GridZoneSystemParams::new,
					() -> delegate,
					params -> delegate = (H3GridZoneSystemParams)params);

			addDefinition(GeometryFreeZoneSystemParams.SET_NAME, GeometryFreeZoneSystemParams::new,
					() -> delegate,
					params -> delegate = (GeometryFreeZoneSystemParams)params);
		}


		@Override
		public void handleAddUnknownParam(String paramName, String value) {
			switch (paramName) {
				case "zoneTargetLinkSelection": {
					log.warn("Param " + paramName + " is no longer supported as part of the deprecated zonal system params. Please set this param in the" +
							" rebalancing params section in the future. The setting will be IGNORED in this execution.");
					break;
				}
				case "zonesGeneration": {
					if (delegate == null) {
						switch (value) {
							case "ShapeFile": {
								addParameterSet(createParameterSet(GISFileZoneSystemParams.SET_NAME));
								break;
							}
							case "GridFromNetwork": {
								addParameterSet(createParameterSet(SquareGridZoneSystemParams.SET_NAME));
								break;
							}
							case "H3": {
								addParameterSet(createParameterSet(H3GridZoneSystemParams.SET_NAME));
								break;
							}
							case "GeometryFree":{
								addParameterSet(createParameterSet(GeometryFreeZoneSystemParams.SET_NAME));
							}
							default:
								super.handleAddUnknownParam(paramName, value);
						}
					}
					break;
				}
				case "cellSize": {
					SquareGridZoneSystemParams squareGridParams;
					if(delegate == null) {
						squareGridParams = (SquareGridZoneSystemParams) createParameterSet(SquareGridZoneSystemParams.SET_NAME);
						addParameterSet(squareGridParams);
					} else {
						squareGridParams = (SquareGridZoneSystemParams) delegate;
					}
					squareGridParams.setCellSize(Double.parseDouble(value));
					break;
				}
				case "zonesShapeFile": {
					GISFileZoneSystemParams gisFileParams;
					if(delegate == null) {
						gisFileParams = (GISFileZoneSystemParams) createParameterSet(GISFileZoneSystemParams.SET_NAME);
						addParameterSet(gisFileParams);
					} else {
						gisFileParams = (GISFileZoneSystemParams) delegate;
					}
					gisFileParams.setZonesShapeFile(value);
					break;
				}
				case "h3Resolution": {
					H3GridZoneSystemParams h3GridParams;
					if(delegate == null) {
						h3GridParams = (H3GridZoneSystemParams) createParameterSet(GISFileZoneSystemParams.SET_NAME);
						addParameterSet(h3GridParams);
					} else {
						h3GridParams = (H3GridZoneSystemParams) delegate;
					}
					h3GridParams.setH3Resolution(Integer.parseInt(value));
					break;
				}
				default:
					super.handleAddUnknownParam(paramName, value);
			}
		}
	}
}
