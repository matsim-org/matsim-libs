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
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class TaxiConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "taxi";

	@SuppressWarnings("deprecation")
	public static TaxiConfigGroup get(Config config) {
		return (TaxiConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String MODE = "mode";
	static final String MODE_EXP = "Mode which will be handled by PassengerEngine and VrpOptimizer "
			+ "(passengers'/customers' perspective)";

	public static final String DESTINATION_KNOWN = "destinationKnown";
	static final String DESTINATION_KNOWN_EXP =
			"If false, the drop-off location remains unknown to the optimizer and scheduler"
					+ " until the end of pickup. False by default.";

	public static final String VEHICLE_DIVERSION = "vehicleDiversion";
	static final String VEHICLE_DIVERSION_EXP = "If true, vehicles can be diverted during empty trips."
			+ " Requires online tracking. False by default.";

	public static final String PICKUP_DURATION = "pickupDuration";
	static final String PICKUP_DURATION_EXP = "Typically, 120 seconds";

	public static final String DROPOFF_DURATION = "dropoffDuration";
	static final String DROPOFF_DURATION_EXP = "Typically, 60 seconds";

	public static final String A_STAR_EUCLIDEAN_OVERDO_FACTOR = "AStarEuclideanOverdoFactor";
	static final String A_STAR_EUCLIDEAN_OVERDO_FACTOR_EXP =
			"Used in AStarEuclidean for shortest path search for occupied drives. Default value is 1.0. "
					+ "Values above 1.0 (typically, 1.5 to 3.0) speed up search, "
					+ "but at the cost of obtaining longer paths";

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
	
	public static final String PRINT_WARNINGS = "plotDetailedWarnings";
	static final String PRINT_WARNINGS_EXP = "Prints detailed warnings for taxi customers that cannot be served or routed. True by default.";

	public static final String BREAK_IF_NOT_ALL_REQUESTS_SERVED = "breakIfNotAllRequestsServed";
	static final String BREAK_IF_NOT_ALL_REQUESTS_SERVED_EXP =
			"Specifies whether the simulation should interrupt if not all requests were performed when"
					+ " an interation ends. Otherwise, a warning is given. True by default.";

	public static final String OPTIMIZER_PARAMETER_SET = "optimizer";
	static final String OPTIMIZER_PARAMETER_SET_EXP =
			"Specifies the type and parameters of the TaxiOptimizer. See: TaxiOptimizerParams and classes"
					+ " implementing it, e.g. AbstractTaxiOptimizerParams.";

	@NotBlank
	private String mode = TransportMode.taxi; // travel mode (passengers'/customers' perspective)

	private boolean destinationKnown = false;
	private boolean vehicleDiversion = false;

	@PositiveOrZero
	private double pickupDuration = Double.NaN;// seconds

	@PositiveOrZero
	private double dropoffDuration = Double.NaN;// seconds

	@Min(1)
	private double AStarEuclideanOverdoFactor = 1.;

	private boolean onlineVehicleTracker = false;
	private boolean changeStartLinkToLastLinkInSchedule = false;

	@NotBlank
	private String taxisFile = null;

	private boolean timeProfiles = false;
	private boolean detailedStats = false;

	private boolean breakSimulationIfNotAllRequestsServed = true;
	
	private boolean printDetailedWarnings = true;

	public TaxiConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE, MODE_EXP);
		map.put(DESTINATION_KNOWN, DESTINATION_KNOWN_EXP);
		map.put(VEHICLE_DIVERSION, VEHICLE_DIVERSION_EXP);
		map.put(PICKUP_DURATION, PICKUP_DURATION_EXP);
		map.put(DROPOFF_DURATION, DROPOFF_DURATION_EXP);
		map.put(A_STAR_EUCLIDEAN_OVERDO_FACTOR, A_STAR_EUCLIDEAN_OVERDO_FACTOR_EXP);
		map.put(ONLINE_VEHICLE_TRACKER, ONLINE_VEHICLE_TRACKER_EXP);
		map.put(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE, CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE_EXP);
		map.put(TAXIS_FILE, TAXIS_FILE_EXP);
		map.put(TIME_PROFILES, TIME_PROFILES_EXP);
		map.put(DETAILED_STATS, DETAILED_STATS_EXP);
		map.put(BREAK_IF_NOT_ALL_REQUESTS_SERVED, BREAK_IF_NOT_ALL_REQUESTS_SERVED_EXP);
		map.put(OPTIMIZER_PARAMETER_SET, OPTIMIZER_PARAMETER_SET_EXP);
		map.put(PRINT_WARNINGS, PRINT_WARNINGS_EXP);
		return map;
	}

	/**
	 * @return {@value #MODE_EXP}
	 */
	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	/**
	 * @param mode {@value #MODE_EXP}
	 */
	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
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
	public void setDestinationKnown(boolean destinationKnown) {
		this.destinationKnown = destinationKnown;
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
	public void setVehicleDiversion(boolean vehicleDiversion) {
		this.vehicleDiversion = vehicleDiversion;
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
	public void setPickupDuration(double pickupDuration) {
		this.pickupDuration = pickupDuration;
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
	public void setDropoffDuration(double dropoffDuration) {
		this.dropoffDuration = dropoffDuration;
	}

	/**
	 * @return {@value #A_STAR_EUCLIDEAN_OVERDO_FACTOR_EXP}
	 */
	@StringGetter(A_STAR_EUCLIDEAN_OVERDO_FACTOR)
	public double getAStarEuclideanOverdoFactor() {
		return AStarEuclideanOverdoFactor;
	}

	/**
	 * @param aStarEuclideanOverdoFactor {@value #A_STAR_EUCLIDEAN_OVERDO_FACTOR_EXP}
	 */
	@StringSetter(A_STAR_EUCLIDEAN_OVERDO_FACTOR)
	public void setAStarEuclideanOverdoFactor(double aStarEuclideanOverdoFactor) {
		AStarEuclideanOverdoFactor = aStarEuclideanOverdoFactor;
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
	public void setOnlineVehicleTracker(boolean onlineVehicleTracker) {
		this.onlineVehicleTracker = onlineVehicleTracker;
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
	public void setChangeStartLinkToLastLinkInSchedule(boolean changeStartLinkToLastLinkInSchedule) {
		this.changeStartLinkToLastLinkInSchedule = changeStartLinkToLastLinkInSchedule;
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
	public void setTaxisFile(String taxisFile) {
		this.taxisFile = taxisFile;
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
	public void setTimeProfiles(boolean timeProfiles) {
		this.timeProfiles = timeProfiles;
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
	public void setDetailedStats(boolean detailedStats) {
		this.detailedStats = detailedStats;
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
	public void setBreakSimulationIfNotAllRequestsServed(boolean breakSimulationIfNotAllRequestsServed) {
		this.breakSimulationIfNotAllRequestsServed = breakSimulationIfNotAllRequestsServed;
	}

	public ConfigGroup getOptimizerConfigGroup() {
		return getParameterSets(OPTIMIZER_PARAMETER_SET).iterator().next();
	}

	public void setOptimizerConfigGroup(ConfigGroup optimizerCfg) {
		clearParameterSetsForType(OPTIMIZER_PARAMETER_SET);
		addParameterSet(optimizerCfg);
	}

	public URL getTaxisFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.taxisFile);
	}

	@StringGetter(PRINT_WARNINGS)
	public boolean isPrintDetailedWarnings() {
		return printDetailedWarnings;
	}

	@StringSetter(PRINT_WARNINGS)
	public void setPrintDetailedWarnings(boolean printDetailedWarnings) {
		this.printDetailedWarnings = printDetailedWarnings;
	}
}
