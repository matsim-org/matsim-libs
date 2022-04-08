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

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.DrtRequestInsertionRetryParams;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.insertion.selective.SelectiveInsertionSearchParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public final class DrtConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {
	private static final Logger log = Logger.getLogger(DrtConfigGroup.class);

	public static final String GROUP_NAME = "drt";

	public static DrtConfigGroup getSingleModeDrtConfig(Config config) {
		Collection<DrtConfigGroup> drtConfigGroups = MultiModeDrtConfigGroup.get(config).getModalElements();
		Preconditions.checkArgument(drtConfigGroups.size() == 1,
				"Supported for only 1 DRT mode in the config. Number of DRT modes: %s", drtConfigGroups.size());
		return drtConfigGroups.iterator().next();
	}

	public static final String MODE = "mode";
	static final String MODE_EXP = "Mode which will be handled by PassengerEngine and VrpOptimizer "
			+ "(passengers'/customers' perspective)";

	public static final String USE_MODE_FILTERED_SUBNETWORK = "useModeFilteredSubnetwork";
	static final String USE_MODE_FILTERED_SUBNETWORK_EXP =
			"Limit the operation of vehicles to links (of the 'dvrp_routing'"
					+ " network) with 'allowedModes' containing this 'mode'."
					+ " For backward compatibility, the value is set to false by default"
					+ " - this means that the vehicles are allowed to operate on all links of the 'dvrp_routing' network."
					+ " The 'dvrp_routing' is defined by DvrpConfigGroup.networkModes)";

	public static final String STOP_DURATION = "stopDuration";
	static final String STOP_DURATION_EXP = "Bus stop duration. Must be positive.";

	public static final String MAX_WAIT_TIME = "maxWaitTime";
	static final String MAX_WAIT_TIME_EXP = "Max wait time for the bus to come (optimisation constraint).";

	public static final String MAX_TRAVEL_TIME_ALPHA = "maxTravelTimeAlpha";
	static final String MAX_TRAVEL_TIME_ALPHA_EXP =
			"Defines the slope of the maxTravelTime estimation function (optimisation constraint), i.e. "
					+ "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
					+ "Alpha should not be smaller than 1.";

	public static final String MAX_TRAVEL_TIME_BETA = "maxTravelTimeBeta";
	static final String MAX_TRAVEL_TIME_BETA_EXP =
			"Defines the shift of the maxTravelTime estimation function (optimisation constraint), i.e. "
					+ "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
					+ "Beta should not be smaller than 0.";

	public static final String MAX_ABSOLUTE_DETOUR = "maxAbsoluteDetour";
	static final String MAX_ABSOLUTE_DETOUR_EXP =
			"Defines the maximum allowed absolute detour in seconds of the maxTravelTime estimation function (optimisation constraint), i.e. "
					+ "min(unsharedRideTime + maxAbsoluteDetour, maxTravelTimeAlpha * unsharedRideTime + maxTravelTimeBeta). "
					+ "maxAbsoluteDetour should not be smaller than 0. and should be higher than the offset maxTravelTimeBeta.";

	public static final String REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED = "rejectRequestIfMaxWaitOrTravelTimeViolated";
	static final String REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED_EXP =
			"If true, the max travel and wait times of a submitted request"
					+ " are considered hard constraints (the request gets rejected if one of the constraints is violated)."
					+ " If false, the max travel and wait times are considered soft constraints (insertion of a request that"
					+ " violates one of the constraints is allowed, but its cost is increased by additional penalty to make"
					+ " it relatively less attractive). Penalisation of insertions can be customised by injecting a customised"
					+ " InsertionCostCalculator.PenaltyCalculator";

	public static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE = "changeStartLinkToLastLinkInSchedule";
	static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP =
			"If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
					+ "day at the link where it stopped operating the day before. False by default.";

	public static final String IDLE_VEHICLES_RETURN_TO_DEPOTS = "idleVehiclesReturnToDepots";
	static final String IDLE_VEHICLES_RETURN_TO_DEPOTS_EXP = "Idle vehicles return to the nearest of all start links. See: DvrpVehicle.getStartLink()";

	public static final String OPERATIONAL_SCHEME = "operationalScheme";
	static final String OPERATIONAL_SCHEME_EXP = "Operational Scheme, either of door2door, stopbased or serviceAreaBased. door2door by default";

	//TODO consider renaming maxWalkDistance to max access/egress distance (or even have 2 separate params)
	public static final String MAX_WALK_DISTANCE = "maxWalkDistance";
	static final String MAX_WALK_DISTANCE_EXP = "Maximum beeline distance (in meters) to next stop location in stopbased system for access/egress walk leg to/from drt. If no stop can be found within this maximum distance will return null (in most cases caught by fallback routing module).";

	public static final String VEHICLES_FILE = "vehiclesFile";
	static final String VEHICLES_FILE_EXP = "An XML file specifying the vehicle fleet."
			+ " The file format according to dvrp_vehicles_v1.dtd"
			+ " If not provided, the vehicle specifications will be created from matsim vehicle file or provided via a custom binding."
			+ " See FleetModule.";

	public static final String TRANSIT_STOP_FILE = "transitStopFile";
	static final String TRANSIT_STOP_FILE_EXP =
			"Stop locations file (transit schedule format, but without lines) for DRT stops. "
					+ "Used only for the stopbased mode";

	private static final String DRT_SERVICE_AREA_SHAPE_FILE = "drtServiceAreaShapeFile";
	private static final String DRT_SERVICE_AREA_SHAPE_FILE_EXP = "allows to configure a service area per drt mode."
			+ "Used with serviceArea Operational Scheme";

	public static final String WRITE_DETAILED_CUSTOMER_STATS = "writeDetailedCustomerStats";
	static final String WRITE_DETAILED_CUSTOMER_STATS_EXP = "Writes out detailed DRT customer stats in each iteration. True by default.";

	public static final String NUMBER_OF_THREADS = "numberOfThreads";
	static final String NUMBER_OF_THREADS_EXP =
			"Number of threads used for parallel evaluation of request insertion into existing schedules."
					+ " Scales well up to 4, due to path data provision, the most computationally intensive part,"
					+ " using up to 4 threads."
					+ " Default value is the number of cores available to JVM.";

	public static final String STORE_UNSHARED_PATH = "storeUnsharedPath";
	static final String STORE_UNSHARED_PATH_EXP = "Store planned unshared drt route as a link sequence";

	@NotBlank
	private String mode = TransportMode.drt; // travel mode (passengers'/customers' perspective)

	private boolean useModeFilteredSubnetwork = false;

	@Positive
	private double stopDuration = Double.NaN;// seconds

	@PositiveOrZero
	private double maxWaitTime = Double.NaN;// seconds

	// max arrival time defined as:
	// min(unshared_ride_travel_time(fromLink, toLink) + maxAbsoluteDetour, maxTravelTimeAlpha * unshared_ride_travel_time(fromLink, toLink) + maxTravelTimeBeta),
	// where unshared_ride_travel_time(fromLink, toLink) is calculated during replanning (see: DrtRouteCreator)
	@DecimalMin("1.0")
	private double maxTravelTimeAlpha = Double.NaN;// [-]

	@PositiveOrZero
	private double maxTravelTimeBeta = Double.NaN;// [s]

	@PositiveOrZero
	private double maxAbsoluteDetour = Double.POSITIVE_INFINITY;// [s]

	private boolean rejectRequestIfMaxWaitOrTravelTimeViolated = true;

	private boolean changeStartLinkToLastLinkInSchedule = false;

	private boolean idleVehiclesReturnToDepots = false;

	@NotNull
	private OperationalScheme operationalScheme = OperationalScheme.door2door;

	@PositiveOrZero // used only for stopbased DRT scheme
	private double maxWalkDistance = Double.MAX_VALUE;// [m];

	@Nullable//it is possible to generate a FleetSpecification (instead of reading it from a file)
	private String vehiclesFile = null;

	@Nullable
	private String transitStopFile = null; // only for stopbased DRT scheme

	@Nullable
	private String drtServiceAreaShapeFile = null; // only for serviceAreaBased DRT scheme

	private boolean plotDetailedCustomerStats = true;

	@Positive
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	@PositiveOrZero
	private double advanceRequestPlanningHorizon = 0; // beta-feature; planning horizon for advance (prebooked) requests

	private boolean storeUnsharedPath = false; // If true, the planned unshared path is stored and exported in plans

	public enum OperationalScheme {
		stopbased, door2door, serviceAreaBased
	}

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

		Verify.verify(getMaxWaitTime() >= getStopDuration(),
				MAX_WAIT_TIME + " must not be smaller than " + STOP_DURATION);

		Verify.verify(getOperationalScheme() != OperationalScheme.stopbased || getTransitStopFile() != null,
				TRANSIT_STOP_FILE
						+ " must not be null when "
						+ OPERATIONAL_SCHEME
						+ " is "
						+ OperationalScheme.stopbased);

		Verify.verify(
				getOperationalScheme() != OperationalScheme.serviceAreaBased || getDrtServiceAreaShapeFile() != null,
				DRT_SERVICE_AREA_SHAPE_FILE
						+ " must not be null when "
						+ OPERATIONAL_SCHEME
						+ " is "
						+ OperationalScheme.serviceAreaBased);

		Verify.verify(getNumberOfThreads() <= Runtime.getRuntime().availableProcessors(),
				NUMBER_OF_THREADS + " is higher than the number of logical cores available to JVM");

		if (config.global().getNumberOfThreads() < getNumberOfThreads()) {
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
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, MODE_EXP);
		map.put(USE_MODE_FILTERED_SUBNETWORK, USE_MODE_FILTERED_SUBNETWORK_EXP);
		map.put(STOP_DURATION, STOP_DURATION_EXP);
		map.put(MAX_WAIT_TIME, MAX_WAIT_TIME_EXP);
		map.put(MAX_TRAVEL_TIME_ALPHA, MAX_TRAVEL_TIME_ALPHA_EXP);
		map.put(MAX_TRAVEL_TIME_BETA, MAX_TRAVEL_TIME_BETA_EXP);
		map.put(MAX_ABSOLUTE_DETOUR, MAX_ABSOLUTE_DETOUR_EXP);
		map.put(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE, CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP);
		map.put(VEHICLES_FILE, VEHICLES_FILE_EXP);
		map.put(WRITE_DETAILED_CUSTOMER_STATS, WRITE_DETAILED_CUSTOMER_STATS_EXP);
		map.put(IDLE_VEHICLES_RETURN_TO_DEPOTS, IDLE_VEHICLES_RETURN_TO_DEPOTS_EXP);
		map.put(OPERATIONAL_SCHEME, OPERATIONAL_SCHEME_EXP);
		map.put(MAX_WALK_DISTANCE, MAX_WALK_DISTANCE_EXP);
		map.put(TRANSIT_STOP_FILE, TRANSIT_STOP_FILE_EXP);
		map.put(NUMBER_OF_THREADS, NUMBER_OF_THREADS_EXP);
		map.put(REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED,
				REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED_EXP);
		map.put(DRT_SERVICE_AREA_SHAPE_FILE, DRT_SERVICE_AREA_SHAPE_FILE_EXP);
		map.put(STORE_UNSHARED_PATH, STORE_UNSHARED_PATH_EXP);
		return map;
	}

	/**
	 * @return {@value #MODE_EXP}
	 */
	@Override
	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	/**
	 * @param mode {@value #MODE_EXP}
	 */
	@StringSetter(MODE)
	public DrtConfigGroup setMode(String mode) {
		this.mode = mode;
		return this;
	}

	/**
	 * @return {@value #USE_MODE_FILTERED_SUBNETWORK_EXP}
	 */
	@StringGetter(USE_MODE_FILTERED_SUBNETWORK)
	public boolean isUseModeFilteredSubnetwork() {
		return useModeFilteredSubnetwork;
	}

	/**
	 * @param useModeFilteredSubnetwork {@value #USE_MODE_FILTERED_SUBNETWORK_EXP}
	 */
	@StringSetter(USE_MODE_FILTERED_SUBNETWORK)
	public DrtConfigGroup setUseModeFilteredSubnetwork(boolean useModeFilteredSubnetwork) {
		this.useModeFilteredSubnetwork = useModeFilteredSubnetwork;
		return this;
	}

	/**
	 * @return -- {@value #STOP_DURATION_EXP}
	 */
	@StringGetter(STOP_DURATION)
	public double getStopDuration() {
		return stopDuration;
	}

	/**
	 * @param -- {@value #STOP_DURATION_EXP}
	 */
	@StringSetter(STOP_DURATION)
	public DrtConfigGroup setStopDuration(double stopDuration) {
		this.stopDuration = stopDuration;
		return this;
	}

	/**
	 * @return -- {@value #MAX_WAIT_TIME_EXP}
	 */
	@StringGetter(MAX_WAIT_TIME)
	public double getMaxWaitTime() {
		return maxWaitTime;
	}

	/**
	 * @param maxWaitTime -- {@value #MAX_WAIT_TIME_EXP}
	 */
	@StringSetter(MAX_WAIT_TIME)
	public DrtConfigGroup setMaxWaitTime(double maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
		return this;
	}

	/**
	 * @return {@link #DRT_SERVICE_AREA_SHAPE_FILE_EXP}
	 */
	@StringGetter(DRT_SERVICE_AREA_SHAPE_FILE)
	public String getDrtServiceAreaShapeFile() {
		return drtServiceAreaShapeFile;
	}

	public URL getDrtServiceAreaShapeFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, drtServiceAreaShapeFile);
	}

	/**
	 * @param getDrtServiceAreaShapeFile -- {@link #DRT_SERVICE_AREA_SHAPE_FILE_EXP}
	 */
	@StringSetter(DRT_SERVICE_AREA_SHAPE_FILE)
	public DrtConfigGroup setDrtServiceAreaShapeFile(String getDrtServiceAreaShapeFile) {
		this.drtServiceAreaShapeFile = getDrtServiceAreaShapeFile;
		return this;
	}

	/**
	 * @return -- {@value #MAX_TRAVEL_TIME_ALPHA_EXP}
	 */
	@StringGetter(MAX_TRAVEL_TIME_ALPHA)
	public double getMaxTravelTimeAlpha() {
		return maxTravelTimeAlpha;
	}

	/**
	 * @param maxTravelTimeAlpha {@value #MAX_TRAVEL_TIME_ALPHA_EXP}
	 */
	@StringSetter(MAX_TRAVEL_TIME_ALPHA)
	public DrtConfigGroup setMaxTravelTimeAlpha(double maxTravelTimeAlpha) {
		this.maxTravelTimeAlpha = maxTravelTimeAlpha;
		return this;
	}

	/**
	 * @return -- {@value #MAX_TRAVEL_TIME_BETA_EXP}
	 */
	@StringGetter(MAX_TRAVEL_TIME_BETA)
	public double getMaxTravelTimeBeta() {
		return maxTravelTimeBeta;
	}

	/**
	 * @param maxTravelTimeBeta -- {@value #MAX_TRAVEL_TIME_BETA_EXP}
	 */
	@StringSetter(MAX_TRAVEL_TIME_BETA)
	public DrtConfigGroup setMaxTravelTimeBeta(double maxTravelTimeBeta) {
		this.maxTravelTimeBeta = maxTravelTimeBeta;
		return this;
	}

	/**
	 * @return -- {@value #MAX_ABSOLUTE_DETOUR_EXP}
	 */
	@StringGetter(MAX_ABSOLUTE_DETOUR)
	public double getMaxAbsoluteDetour() {
		return maxAbsoluteDetour;
	}

	/**
	 * @param maxAbsoluteDetour -- {@value #MAX_ABSOLUTE_DETOUR_EXP}
	 */
	@StringSetter(MAX_ABSOLUTE_DETOUR)
	public DrtConfigGroup setMaxAbsoluteDetour(double maxAbsoluteDetour) {
		this.maxAbsoluteDetour = maxAbsoluteDetour;
		return this;
	}

	/**
	 * @return -- {@value #REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED_EXP}
	 */
	@StringGetter(REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED)
	public boolean isRejectRequestIfMaxWaitOrTravelTimeViolated() {
		return rejectRequestIfMaxWaitOrTravelTimeViolated;
	}

	/**
	 * @param rejectRequestIfMaxWaitOrTravelTimeViolated -- {@value #REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED_EXP}
	 */
	@StringSetter(REJECT_REQUEST_IF_MAX_WAIT_OR_TRAVEL_TIME_VIOLATED)
	public DrtConfigGroup setRejectRequestIfMaxWaitOrTravelTimeViolated(
			boolean rejectRequestIfMaxWaitOrTravelTimeViolated) {
		this.rejectRequestIfMaxWaitOrTravelTimeViolated = rejectRequestIfMaxWaitOrTravelTimeViolated;
		return this;
	}

	/**
	 * @return -- {@value #CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP}
	 */
	@StringGetter(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE)
	public boolean isChangeStartLinkToLastLinkInSchedule() {
		return changeStartLinkToLastLinkInSchedule;
	}

	/**
	 * @param changeStartLinkToLastLinkInSchedule -- {@value #CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP}
	 */
	@StringSetter(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE)
	public DrtConfigGroup setChangeStartLinkToLastLinkInSchedule(boolean changeStartLinkToLastLinkInSchedule) {
		this.changeStartLinkToLastLinkInSchedule = changeStartLinkToLastLinkInSchedule;
		return this;
	}

	/**
	 * @return -- {@value #VEHICLES_FILE_EXP}
	 */
	@StringGetter(VEHICLES_FILE)
	public String getVehiclesFile() {
		return vehiclesFile;
	}

	/**
	 * @param vehiclesFile -- {@value #VEHICLES_FILE_EXP}
	 */
	@StringSetter(VEHICLES_FILE)
	public DrtConfigGroup setVehiclesFile(String vehiclesFile) {
		this.vehiclesFile = vehiclesFile;
		return this;
	}

	/**
	 * @return -- {@value #VEHICLES_FILE_EXP}
	 */
	public URL getVehiclesFileUrl(URL context) {
		return vehiclesFile == null ? null : ConfigGroup.getInputFileURL(context, vehiclesFile);
	}

	/**
	 * @return -- {@value #IDLE_VEHICLES_RETURN_TO_DEPOTS_EXP}}
	 */
	@StringGetter(IDLE_VEHICLES_RETURN_TO_DEPOTS)
	public boolean getIdleVehiclesReturnToDepots() {
		return idleVehiclesReturnToDepots;
	}

	/**
	 * @param idleVehiclesReturnToDepots -- {@value #IDLE_VEHICLES_RETURN_TO_DEPOTS_EXP}
	 */
	@StringSetter(IDLE_VEHICLES_RETURN_TO_DEPOTS)
	public DrtConfigGroup setIdleVehiclesReturnToDepots(boolean idleVehiclesReturnToDepots) {
		this.idleVehiclesReturnToDepots = idleVehiclesReturnToDepots;
		return this;
	}

	/**
	 * @return -- {@value #OPERATIONAL_SCHEME_EXP}
	 */
	@StringGetter(OPERATIONAL_SCHEME)
	public OperationalScheme getOperationalScheme() {
		return operationalScheme;
	}

	/**
	 * @param operationalScheme -- {@value #OPERATIONAL_SCHEME_EXP}
	 */
	@StringSetter(OPERATIONAL_SCHEME)
	public DrtConfigGroup setOperationalScheme(OperationalScheme operationalScheme) {
		this.operationalScheme = operationalScheme;
		return this;
	}

	/**
	 * @return -- {@value #TRANSIT_STOP_FILE_EXP}
	 */
	@StringGetter(TRANSIT_STOP_FILE)
	public String getTransitStopFile() {
		return transitStopFile;
	}

	/**
	 * @return -- {@value #TRANSIT_STOP_FILE_EXP}
	 */
	public URL getTransitStopsFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, transitStopFile);
	}

	/**
	 * @param-- {@value #TRANSIT_STOP_FILE_EXP}
	 */
	@StringSetter(TRANSIT_STOP_FILE)
	public DrtConfigGroup setTransitStopFile(String transitStopFile) {
		this.transitStopFile = transitStopFile;
		return this;
	}

	/**
	 * @return -- {@value #MAX_WALK_DISTANCE_EXP}
	 */
	@StringGetter(MAX_WALK_DISTANCE)
	public double getMaxWalkDistance() {
		return maxWalkDistance;
	}

	/**
	 * @param-- {@value #MAX_WALK_DISTANCE_EXP}
	 */
	@StringSetter(MAX_WALK_DISTANCE)
	public DrtConfigGroup setMaxWalkDistance(double maximumWalkDistance) {
		this.maxWalkDistance = maximumWalkDistance;
		return this;
	}

	/**
	 * @return -- {@value #WRITE_DETAILED_CUSTOMER_STATS_EXP}
	 */
	@StringGetter(WRITE_DETAILED_CUSTOMER_STATS)
	public boolean isPlotDetailedCustomerStats() {
		return plotDetailedCustomerStats;
	}

	/**
	 * @param -- {@value #WRITE_DETAILED_CUSTOMER_STATS_EXP}
	 */
	@StringSetter(WRITE_DETAILED_CUSTOMER_STATS)
	public DrtConfigGroup setPlotDetailedCustomerStats(boolean plotDetailedCustomerStats) {
		this.plotDetailedCustomerStats = plotDetailedCustomerStats;
		return this;
	}

	/**
	 * @return -- {@value #NUMBER_OF_THREADS_EXP}
	 */
	@StringGetter(NUMBER_OF_THREADS)
	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	/**
	 * @param-- {@value #NUMBER_OF_THREADS_EXP}
	 */
	@StringSetter(NUMBER_OF_THREADS)
	public DrtConfigGroup setNumberOfThreads(final int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
		return this;
	}

	/**
	 * @return -- {@value #STORE_UNSHARED_PATH_EXP}
	 */
	@StringGetter(STORE_UNSHARED_PATH)
	public boolean getStoreUnsharedPath() {
		return storeUnsharedPath;
	}

	/**
	 * @return -- {@value #STORE_UNSHARED_PATH_EXP}
	 */
	@StringSetter(STORE_UNSHARED_PATH)
	void setStoreUnsharedPath(boolean storeUnsharedPath) {
		this.storeUnsharedPath = storeUnsharedPath;
	}

	public double getAdvanceRequestPlanningHorizon() {
		return advanceRequestPlanningHorizon;
	}

	public DrtConfigGroup setAdvanceRequestPlanningHorizon(double advanceRequestPlanningHorizon) {
		this.advanceRequestPlanningHorizon = advanceRequestPlanningHorizon;
		return this;
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
