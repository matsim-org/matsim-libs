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

import java.net.URL;
import java.util.Map;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

public class DrtConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "drt";
	public static final String DRT_MODE = "drt";

	@SuppressWarnings("deprecation")
	public static DrtConfigGroup get(Config config) {
		return (DrtConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String STOP_DURATION = "stopDuration";
	public static final String MAX_WAIT_TIME = "maxWaitTime";
	public static final String MAX_TRAVEL_TIME_ALPHA = "maxTravelTimeAlpha";
	public static final String MAX_TRAVEL_TIME_BETA = "maxTravelTimeBeta";
	public static final String A_STAR_EUCLIDEAN_OVERDO_FACTOR = "AStarEuclideanOverdoFactor";
	public static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE = "changeStartLinkToLastLinkInSchedule";

	public static final String REBALANCING_INTERVAL = "rebalancingInterval";
	public static final String IDLE_VEHICLES_RETURN_TO_DEPOTS = "idleVehiclesReturnToDepots";
	public static final String OPERATIONAL_SCHEME = "operationalScheme";

	public static final String MAX_WALK_DISTANCE = "maxWalkDistance";
	public static final String ESTIMATED_DRT_SPEED = "estimatedDrtSpeed";
	public static final String ESTIMATED_BEELINE_DISTANCE_FACTOR = "estimatedBeelineDistanceFactor";

	public static final String VEHICLES_FILE = "vehiclesFile";
	public static final String TRANSIT_STOP_FILE = "transitStopFile";
	public static final String PLOT_CUST_STATS = "writeDetailedCustomerStats";
	public static final String PLOT_VEH_STATS = "writeDetailedVehicleStats";
	public static final String PRINT_WARNINGS = "plotDetailedWarnings";
	public static final String NUMBER_OF_THREADS = "numberOfThreads";

	@PositiveOrZero
	private double stopDuration = Double.NaN;// seconds

	@PositiveOrZero
	private double maxWaitTime = Double.NaN;// seconds

	// max arrival time defined as:
	// maxTravelTimeAlpha * unshared_ride_travel_time(fromLink, toLink) + maxTravelTimeBeta,
	// where unshared_ride_travel_time(fromLink, toLink) is calculated with FastAStarEuclidean
	// (hence AStarEuclideanOverdoFactor needs to be specified)
	@Min(1)
	private double maxTravelTimeAlpha = Double.NaN;// [-]

	@PositiveOrZero
	private double maxTravelTimeBeta = Double.NaN;// [s]

	@Min(1)
	private double AStarEuclideanOverdoFactor = 1.;

	private boolean changeStartLinkToLastLinkInSchedule = false;

	@PositiveOrZero
	private int rebalancingInterval = 0;// [s], if 0 then no rebalancing

	private boolean idleVehiclesReturnToDepots = false;

	@NotNull
	private OperationalScheme operationalScheme = OperationalScheme.door2door;

	@PositiveOrZero // used only for stationbased DRT scheme
	private double maxWalkDistance = 0;// [m];

	@PositiveOrZero
	private double estimatedDrtSpeed = 25. / 3.6;// [m/s]

	@Min(1)
	private double estimatedBeelineDistanceFactor = 1.3;// [-]

	@NotNull
	private String vehiclesFile = null;

	// used only for stationbased DRT scheme
	private String transitStopFile = null; // only for stationbased DRT scheme

	private boolean plotDetailedCustomerStats = true;
	private boolean plotDetailedVehicleStats = false;
	private boolean printDetailedWarnings = false;

	@Positive
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	public enum OperationalScheme {
		stationbased, door2door
	}

	public DrtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(STOP_DURATION, "Bus stop duration.");
		map.put(MAX_WAIT_TIME, "Max wait time for the bus to come (optimisation constraint).");
		map.put(MAX_TRAVEL_TIME_ALPHA,
				"Defines the slope of the maxTravelTime estimation function (optimisation constraint), i.e. "
						+ "maxTravelTimeAlpha * estimated_drt_travel_time + maxTravelTimeBeta. "
						+ "Alpha should not be smaller than 1.");
		map.put(MAX_TRAVEL_TIME_BETA,
				"Defines the shift of the maxTravelTime estimation function (optimisation constraint), i.e. "
						+ "maxTravelTimeAlpha * estimated_drt_travel_time + maxTravelTimeBeta. "
						+ "Beta should not be smaller than 0.");
		map.put(A_STAR_EUCLIDEAN_OVERDO_FACTOR,
				"Used in AStarEuclidean for shortest path search for unshared (== optimistic) rides. "
						+ "Default value is 1.0. Values above 1.0 (typically, 1.5 to 3.0) speed up search, "
						+ "but at the cost of obtaining longer paths");
		map.put(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE,
				"If true, the startLink is changed to last link in the current schedule, so the taxi starts the next "
						+ "day at the link where it stopped operating the day before. False by default.");
		map.put(VEHICLES_FILE,
				"An XML file specifying the vehicle fleet. The file format according to dvrp_vehicles_v1.dtd");
		map.put(PLOT_CUST_STATS, "Writes out detailed DRT customer stats in each iteration. True by default.");
		map.put(PLOT_VEH_STATS,
				"Writes out detailed vehicle stats in each iteration. Creates one file per vehicle and iteration. "
						+ "False by default.");
		map.put(REBALANCING_INTERVAL,
				"Specifies how often empty vehicle rebalancing is executed. 0 means no rebalancing (the default).");
		map.put(IDLE_VEHICLES_RETURN_TO_DEPOTS,
				"Idle vehicles return to the nearest of all start links. See: Vehicle.getStartLink()");
		map.put(OPERATIONAL_SCHEME, "Operational Scheme, either door2door or stationbased. door2door by default");
		map.put(MAX_WALK_DISTANCE, "Maximum walk distance (in meters) to next stop location in stationbased system.");
		map.put(TRANSIT_STOP_FILE, "Stop locations file (transit schedule format, but without lines) for DRT stops. "
				+ "Used only for the stationbased mode");
		map.put(ESTIMATED_DRT_SPEED, "Beeline-speed estimate for DRT. Used in analysis, optimisation constraints "
				+ "and in plans file, [m/s]. The default value is 25 km/h");
		map.put(ESTIMATED_BEELINE_DISTANCE_FACTOR,
				"Beeline distance factor for DRT. Used in analyis and in plans file. The default value is 1.3.");
		map.put(NUMBER_OF_THREADS,
				"Number of threads used for parallel evaluation of request insertion into existing schedules. "
						+ "If unset, the number of threads is equal to the number of logical cores available to JVM.");
		map.put(PRINT_WARNINGS,
				"Prints detailed warnings for DRT customers that cannot be served or routed. Default is false.");
		return map;
	}

	@StringGetter(STOP_DURATION)
	public double getStopDuration() {
		return stopDuration;
	}

	@StringSetter(STOP_DURATION)
	public void setStopDuration(double stopDuration) {
		this.stopDuration = stopDuration;
	}

	@StringGetter(MAX_WAIT_TIME)
	public double getMaxWaitTime() {
		return maxWaitTime;
	}

	@StringSetter(MAX_WAIT_TIME)
	public void setMaxWaitTime(double maxWaitTime) {
		this.maxWaitTime = maxWaitTime;
	}

	@StringGetter(MAX_TRAVEL_TIME_ALPHA)
	public double getMaxTravelTimeAlpha() {
		return maxTravelTimeAlpha;
	}

	@StringSetter(MAX_TRAVEL_TIME_ALPHA)
	public void setMaxTravelTimeAlpha(double maxTravelTimeAlpha) {
		this.maxTravelTimeAlpha = maxTravelTimeAlpha;
	}

	@StringGetter(MAX_TRAVEL_TIME_BETA)
	public double getMaxTravelTimeBeta() {
		return maxTravelTimeBeta;
	}

	@StringSetter(MAX_TRAVEL_TIME_BETA)
	public void setMaxTravelTimeBeta(double maxTravelTimeBeta) {
		this.maxTravelTimeBeta = maxTravelTimeBeta;
	}

	@StringGetter(A_STAR_EUCLIDEAN_OVERDO_FACTOR)
	public double getAStarEuclideanOverdoFactor() {
		return AStarEuclideanOverdoFactor;
	}

	@StringSetter(A_STAR_EUCLIDEAN_OVERDO_FACTOR)
	public void setAStarEuclideanOverdoFactor(double aStarEuclideanOverdoFactor) {
		AStarEuclideanOverdoFactor = aStarEuclideanOverdoFactor;
	}

	@StringGetter(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE)
	public boolean isChangeStartLinkToLastLinkInSchedule() {
		return changeStartLinkToLastLinkInSchedule;
	}

	@StringSetter(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE)
	public void setChangeStartLinkToLastLinkInSchedule(boolean changeStartLinkToLastLinkInSchedule) {
		this.changeStartLinkToLastLinkInSchedule = changeStartLinkToLastLinkInSchedule;
	}

	@StringGetter(VEHICLES_FILE)
	public String getVehiclesFile() {
		return vehiclesFile;
	}

	@StringSetter(VEHICLES_FILE)
	public void setVehiclesFile(String vehiclesFile) {
		this.vehiclesFile = vehiclesFile;
	}

	public URL getVehiclesFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.vehiclesFile);
	}

	@StringGetter(REBALANCING_INTERVAL)
	public int getRebalancingInterval() {
		return rebalancingInterval;
	}

	@StringSetter(REBALANCING_INTERVAL)
	public void setRebalancingInterval(int rebalancingInterval) {
		this.rebalancingInterval = rebalancingInterval;
	}

	@StringGetter(IDLE_VEHICLES_RETURN_TO_DEPOTS)
	public boolean getIdleVehiclesReturnToDepots() {
		return idleVehiclesReturnToDepots;
	}

	@StringSetter(IDLE_VEHICLES_RETURN_TO_DEPOTS)
	public void setIdleVehiclesReturnToDepots(boolean idleVehiclesReturnToDepots) {
		this.idleVehiclesReturnToDepots = idleVehiclesReturnToDepots;
	}

	@StringGetter(OPERATIONAL_SCHEME)
	public OperationalScheme getOperationalScheme() {
		return operationalScheme;
	}

	@StringSetter(OPERATIONAL_SCHEME)
	public void setOperationalScheme(String operationalScheme) {

		this.operationalScheme = OperationalScheme.valueOf(operationalScheme);
	}

	@StringGetter(TRANSIT_STOP_FILE)
	public String getTransitStopFile() {
		return transitStopFile;
	}

	public URL getTransitStopsFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.transitStopFile);
	}

	@StringSetter(TRANSIT_STOP_FILE)
	public void setTransitStopFile(String transitStopFile) {
		this.transitStopFile = transitStopFile;
	}

	@StringGetter(MAX_WALK_DISTANCE)
	public double getMaxWalkDistance() {
		return maxWalkDistance;
	}

	@StringSetter(MAX_WALK_DISTANCE)
	public void setMaxWalkDistance(double maximumWalkDistance) {
		this.maxWalkDistance = maximumWalkDistance;
	}

	@StringGetter(ESTIMATED_DRT_SPEED)
	public double getEstimatedDrtSpeed() {
		return estimatedDrtSpeed;
	}

	@StringSetter(ESTIMATED_DRT_SPEED)
	public void setEstimatedSpeed(double estimatedSpeed) {
		this.estimatedDrtSpeed = estimatedSpeed;
	}

	@StringGetter(ESTIMATED_BEELINE_DISTANCE_FACTOR)
	public double getEstimatedBeelineDistanceFactor() {
		return estimatedBeelineDistanceFactor;
	}

	@StringSetter(ESTIMATED_BEELINE_DISTANCE_FACTOR)
	public void setEstimatedBeelineDistanceFactor(double estimatedBeelineDistanceFactor) {
		this.estimatedBeelineDistanceFactor = estimatedBeelineDistanceFactor;
	}

	@StringGetter(PLOT_CUST_STATS)
	public boolean isPlotDetailedCustomerStats() {
		return plotDetailedCustomerStats;
	}

	@StringSetter(PLOT_CUST_STATS)
	public void setPlotDetailedCustomerStats(boolean plotDetailedCustomerStats) {
		this.plotDetailedCustomerStats = plotDetailedCustomerStats;
	}

	@StringGetter(PLOT_VEH_STATS)
	public boolean isPlotDetailedVehicleStats() {
		return plotDetailedVehicleStats;
	}

	@StringSetter(PLOT_VEH_STATS)
	public void setPlotDetailedVehicleStats(boolean plotDetailedVehicleStats) {
		this.plotDetailedVehicleStats = plotDetailedVehicleStats;
	}

	@StringGetter(NUMBER_OF_THREADS)
	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	@StringSetter(NUMBER_OF_THREADS)
	public void setNumberOfThreads(final int numberOfThreads) {
		this.numberOfThreads = numberOfThreads;
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
