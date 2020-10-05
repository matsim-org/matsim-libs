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

package org.matsim.contrib.taxi.run;

import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingNetworkModule;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.fifo.FifoTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.rules.RuleBasedTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.zonal.ZonalTaxiOptimizerParams;
import org.matsim.contrib.util.ReflectiveConfigGroupWithConfigurableParameterSets;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Verify;

public final class TaxiConfigGroup extends ReflectiveConfigGroupWithConfigurableParameterSets implements Modal {
	private static final Logger log = Logger.getLogger(TaxiConfigGroup.class);

	public static final String GROUP_NAME = "taxi";

	/**
	 * @param config
	 * @return single-mode taxi config (only if there is exactly 1 taxi config group in {@link MultiModeTaxiConfigGroup}.
	 * Otherwise will fail.
	 */
	public static TaxiConfigGroup getSingleModeTaxiConfig(Config config) {
		Collection<TaxiConfigGroup> taxiConfigGroups = MultiModeTaxiConfigGroup.get(config).getModalElements();
		Preconditions.checkArgument(taxiConfigGroups.size() == 1,
				"Supported for only 1 taxi mode in the config. Number of taxi modes: %s", taxiConfigGroups.size());
		return taxiConfigGroups.iterator().next();
	}

	public static final String MODE = "mode";
	static final String MODE_EXP = "Mode which will be handled by PassengerEngine and VrpOptimizer "
			+ "(passengers'/customers' perspective)";

	public static final String USE_MODE_FILTERED_SUBNETWORK = "useModeFilteredSubnetwork";
	static final String USE_MODE_FILTERED_SUBNETWORK_EXP =
			"Limit the operation of vehicles to links (of the 'dvrp_routing'"
					+ " network) with 'allowedModes' containing this 'mode'."
					+ " For backward compatibility, the value is set to false by default"
					+ " -- this means that the vehicles are allowed to operate on all links of the 'dvrp_routing' network."
					+ " The 'dvrp_routing' is defined by DvrpConfigGroup.networkModes)";

	public static final String DESTINATION_KNOWN = "destinationKnown";
	static final String DESTINATION_KNOWN_EXP =
			"If false, the drop-off location remains unknown to the optimizer and scheduler"
					+ " until the end of pickup. False by default.";

	public static final String VEHICLE_DIVERSION = "vehicleDiversion";
	static final String VEHICLE_DIVERSION_EXP = "If true, vehicles can be diverted during empty trips."
			+ " Requires online tracking. False by default.";

	public static final String PICKUP_DURATION = "pickupDuration";
	static final String PICKUP_DURATION_EXP = "Pickup duration. Must be positive.";

	public static final String DROPOFF_DURATION = "dropoffDuration";
	static final String DROPOFF_DURATION_EXP = "Dropoff duration. Must be positive.";

	public static final String ONLINE_VEHICLE_TRACKER = "onlineVehicleTracker";
	static final String ONLINE_VEHICLE_TRACKER_EXP =
			"If true, vehicles are (GPS-like) monitored while moving. This helps in getting more accurate "
					+ "estimates on the time of arrival. Online tracking is necessary for vehicle diversion. "
					+ "False by default.";

	public static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE = "changeStartLinkToLastLinkInSchedule";
	static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP =
			"If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
					+ "day at the link where it stopped operating the day before. False by default.";

	// input
	public static final String TAXIS_FILE = "taxisFile";
	static final String TAXIS_FILE_EXP = "An XML file specifying the taxi fleet."
			+ " The file format according to dvrp_vehicles_v1.dtd";

	// output
	public static final String TIME_PROFILES = "timeProfiles";
	static final String TIME_PROFILES_EXP =
			"If true, writes time profiles of vehicle statuses (i.e. current task type) and the number of unplanned "
					+ "requests are written to a text file (taxi_status_time_profiles) and saved as plots. "
					+ "False by default.";

	public static final String DETAILED_STATS = "detailedStats";
	static final String DETAILED_STATS_EXP = "If true, detailed hourly taxi stats are dumped after each iteration."
			+ " False by default.";

	public static final String BREAK_IF_NOT_ALL_REQUESTS_SERVED = "breakIfNotAllRequestsServed";
	static final String BREAK_IF_NOT_ALL_REQUESTS_SERVED_EXP =
			"Specifies whether the simulation should interrupt if not all requests were performed when"
					+ " an interation ends. Otherwise, a warning is given. True by default.";

	@NotBlank
	private String mode = TransportMode.taxi; // travel mode (passengers'/customers' perspective)

	private boolean useModeFilteredSubnetwork = false;

	private boolean destinationKnown = false;
	private boolean vehicleDiversion = false;

	@Positive
	private double pickupDuration = Double.NaN;// seconds

	@Positive
	private double dropoffDuration = Double.NaN;// seconds

	private boolean onlineVehicleTracker = false;
	private boolean changeStartLinkToLastLinkInSchedule = false;

	@Nullable//it is possible to generate a FleetSpecification (instead of reading it from a file)
	private String taxisFile = null;

	private boolean timeProfiles = false;
	private boolean detailedStats = false;

	private boolean breakSimulationIfNotAllRequestsServed = true;

	@NotNull
	private AbstractTaxiOptimizerParams taxiOptimizerParams;

	public TaxiConfigGroup() {
		super(GROUP_NAME);
		initSingletonParameterSets();
	}

	private void initSingletonParameterSets() {
		//optimiser params (one of: assignment, fifo, rule-based, zonal)
		addOptimizerParamsDefinition(AssignmentTaxiOptimizerParams.SET_NAME, AssignmentTaxiOptimizerParams::new);
		addOptimizerParamsDefinition(FifoTaxiOptimizerParams.SET_NAME, FifoTaxiOptimizerParams::new);
		addOptimizerParamsDefinition(RuleBasedTaxiOptimizerParams.SET_NAME, RuleBasedTaxiOptimizerParams::new);
		addOptimizerParamsDefinition(ZonalTaxiOptimizerParams.SET_NAME, ZonalTaxiOptimizerParams::new);
	}

	public void addOptimizerParamsDefinition(String name, Supplier<AbstractTaxiOptimizerParams> creator) {
		addDefinition(name, creator, () -> taxiOptimizerParams,
				params -> taxiOptimizerParams = (AbstractTaxiOptimizerParams)params);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (config.qsim().getNumberOfThreads() > 1) {
			log.warn("EXPERIMENTAL FEATURE: Running taxi with a multi-threaded QSim");
		}

		Verify.verify(!isVehicleDiversion() || isOnlineVehicleTracker(),
				TaxiConfigGroup.VEHICLE_DIVERSION + " requires " + TaxiConfigGroup.ONLINE_VEHICLE_TRACKER);

		if (useModeFilteredSubnetwork) {
			DvrpModeRoutingNetworkModule.checkUseModeFilteredSubnetworkAllowed(config, mode);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, MODE_EXP);
		map.put(USE_MODE_FILTERED_SUBNETWORK, USE_MODE_FILTERED_SUBNETWORK_EXP);
		map.put(DESTINATION_KNOWN, DESTINATION_KNOWN_EXP);
		map.put(VEHICLE_DIVERSION, VEHICLE_DIVERSION_EXP);
		map.put(PICKUP_DURATION, PICKUP_DURATION_EXP);
		map.put(DROPOFF_DURATION, DROPOFF_DURATION_EXP);
		map.put(ONLINE_VEHICLE_TRACKER, ONLINE_VEHICLE_TRACKER_EXP);
		map.put(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE, CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP);
		map.put(TAXIS_FILE, TAXIS_FILE_EXP);
		map.put(TIME_PROFILES, TIME_PROFILES_EXP);
		map.put(DETAILED_STATS, DETAILED_STATS_EXP);
		map.put(BREAK_IF_NOT_ALL_REQUESTS_SERVED, BREAK_IF_NOT_ALL_REQUESTS_SERVED_EXP);
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
	public TaxiConfigGroup setMode(String mode) {
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
	public TaxiConfigGroup setUseModeFilteredSubnetwork(boolean useModeFilteredSubnetwork) {
		this.useModeFilteredSubnetwork = useModeFilteredSubnetwork;
		return this;
	}

	/**
	 * @return {@value #DESTINATION_KNOWN_EXP}
	 */
	@StringGetter(DESTINATION_KNOWN)
	public boolean isDestinationKnown() {
		return destinationKnown;
	}

	/**
	 * @param destinationKnown {@value #DESTINATION_KNOWN_EXP}
	 */
	@StringSetter(DESTINATION_KNOWN)
	public TaxiConfigGroup setDestinationKnown(boolean destinationKnown) {
		this.destinationKnown = destinationKnown;
		return this;
	}

	/**
	 * @return {@value #VEHICLE_DIVERSION_EXP}
	 */
	@StringGetter(VEHICLE_DIVERSION)
	public boolean isVehicleDiversion() {
		return vehicleDiversion;
	}

	/**
	 * @param vehicleDiversion {@value #VEHICLE_DIVERSION_EXP}
	 */
	@StringSetter(VEHICLE_DIVERSION)
	public TaxiConfigGroup setVehicleDiversion(boolean vehicleDiversion) {
		this.vehicleDiversion = vehicleDiversion;
		return this;
	}

	/**
	 * @return {@value #PICKUP_DURATION_EXP}
	 */
	@StringGetter(PICKUP_DURATION)
	public double getPickupDuration() {
		return pickupDuration;
	}

	/**
	 * @param pickupDuration {@value #PICKUP_DURATION_EXP}
	 */
	@StringSetter(PICKUP_DURATION)
	public TaxiConfigGroup setPickupDuration(double pickupDuration) {
		this.pickupDuration = pickupDuration;
		return this;
	}

	/**
	 * @return {@value #DROPOFF_DURATION_EXP}
	 */
	@StringGetter(DROPOFF_DURATION)
	public double getDropoffDuration() {
		return dropoffDuration;
	}

	/**
	 * @param dropoffDuration {@value #DROPOFF_DURATION_EXP}
	 */
	@StringSetter(DROPOFF_DURATION)
	public TaxiConfigGroup setDropoffDuration(double dropoffDuration) {
		this.dropoffDuration = dropoffDuration;
		return this;
	}

	/**
	 * @return {@value #ONLINE_VEHICLE_TRACKER_EXP}
	 */
	@StringGetter(ONLINE_VEHICLE_TRACKER)
	public boolean isOnlineVehicleTracker() {
		return onlineVehicleTracker;
	}

	/**
	 * @param onlineVehicleTracker {@value #ONLINE_VEHICLE_TRACKER_EXP}
	 */
	@StringSetter(ONLINE_VEHICLE_TRACKER)
	public TaxiConfigGroup setOnlineVehicleTracker(boolean onlineVehicleTracker) {
		this.onlineVehicleTracker = onlineVehicleTracker;
		return this;
	}

	/**
	 * @return {@value #CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP}
	 */
	@StringGetter(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE)
	public boolean isChangeStartLinkToLastLinkInSchedule() {
		return changeStartLinkToLastLinkInSchedule;
	}

	/**
	 * @param changeStartLinkToLastLinkInSchedule {@value #CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP}
	 */
	@StringSetter(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE)
	public TaxiConfigGroup setChangeStartLinkToLastLinkInSchedule(boolean changeStartLinkToLastLinkInSchedule) {
		this.changeStartLinkToLastLinkInSchedule = changeStartLinkToLastLinkInSchedule;
		return this;
	}

	/**
	 * @return {@value #TAXIS_FILE_EXP}
	 */
	@StringGetter(TAXIS_FILE)
	public String getTaxisFile() {
		return taxisFile;
	}

	/**
	 * @param taxisFile {@value #TAXIS_FILE_EXP}
	 */
	@StringSetter(TAXIS_FILE)
	public TaxiConfigGroup setTaxisFile(String taxisFile) {
		this.taxisFile = taxisFile;
		return this;
	}

	/**
	 * @return {@value #TIME_PROFILES_EXP}
	 */
	@StringGetter(TIME_PROFILES)
	public boolean getTimeProfiles() {
		return timeProfiles;
	}

	/**
	 * @param timeProfiles {@value #TIME_PROFILES_EXP}
	 */
	@StringSetter(TIME_PROFILES)
	public TaxiConfigGroup setTimeProfiles(boolean timeProfiles) {
		this.timeProfiles = timeProfiles;
		return this;
	}

	/**
	 * @return {@value #DETAILED_STATS_EXP}
	 */
	@StringGetter(DETAILED_STATS)
	public boolean getDetailedStats() {
		return detailedStats;
	}

	/**
	 * @param detailedStats {@value #DETAILED_STATS_EXP}
	 */
	@StringSetter(DETAILED_STATS)
	public TaxiConfigGroup setDetailedStats(boolean detailedStats) {
		this.detailedStats = detailedStats;
		return this;
	}

	/**
	 * @return {@value #BREAK_IF_NOT_ALL_REQUESTS_SERVED_EXP}
	 */
	@StringGetter(BREAK_IF_NOT_ALL_REQUESTS_SERVED)
	public boolean isBreakSimulationIfNotAllRequestsServed() {
		return breakSimulationIfNotAllRequestsServed;
	}

	/**
	 * @param breakSimulationIfNotAllRequestsServed {@value #BREAK_IF_NOT_ALL_REQUESTS_SERVED_EXP}
	 */
	@StringSetter(BREAK_IF_NOT_ALL_REQUESTS_SERVED)
	public TaxiConfigGroup setBreakSimulationIfNotAllRequestsServed(boolean breakSimulationIfNotAllRequestsServed) {
		this.breakSimulationIfNotAllRequestsServed = breakSimulationIfNotAllRequestsServed;
		return this;
	}

	public AbstractTaxiOptimizerParams getTaxiOptimizerParams() {
		return taxiOptimizerParams;
	}

	public URL getTaxisFileUrl(URL context) {
		return taxisFile == null ? null : ConfigGroup.getInputFileURL(context, taxisFile);
	}
}
