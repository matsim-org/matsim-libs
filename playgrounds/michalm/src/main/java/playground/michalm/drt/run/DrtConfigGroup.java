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

package playground.michalm.drt.run;

import java.net.URL;
import java.util.Map;

import org.matsim.core.config.*;

public class DrtConfigGroup extends ReflectiveConfigGroup {
	public static final String GROUP_NAME = "drt";
	public static final String DRT_MODE = "drt";

	@SuppressWarnings("deprecation")
	public static DrtConfigGroup get(Config config) {
		return (DrtConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String STOP_DURATION = "stopDuration";
	public static final String MAX_WAIT_TIME = "maxWaitTime";
	public static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE = "changeStartLinkToLastLinkInSchedule";

	private static final String OPERATIONAL_SCHEME = "operationalScheme";
	private static final String MAXIMUM_WALK_DISTANCE = "maximumWalkDistance";
	private static final String ESTIMATED_DRT_SPEED = "estimatedDRTSpeed";
	private static final String ESTIMATED_BEELINE_DISTANCE_FACTOR = "estimatedBeelineDistanceFactor";

	public static final String VEHICLES_FILE = "vehiclesFile";
	private static final String TRANSIT_STOP_FILE = "transitStopFile";
	private static final String PLOT_CUST_STATS = "writeDetailedCustomerStats";
	private static final String PLOT_VEH_STATS = "writeDetailedVehicleStats";
	private static final String DRT_NET_MODE = "drtNetworkMode";
	

	private double stopDuration = Double.NaN;// seconds
	private double maxWaitTime = Double.NaN;// seconds
	private boolean changeStartLinkToLastLinkInSchedule = false;

	private DRTOperationalScheme operationalScheme;
	private double maximumWalkDistance;
	private double estimatedDrtSpeed = 25 / 3.6;
	private double estimatedBeelineDistanceFactor = 1.3;

	private String vehiclesFile = null;
	private String transitStopFile = null;
	private String drtNetworkMode = "car";
	
	private boolean plotDetailedCustomerStats = true;
	private boolean plotDetailedVehicleStats = false;
	
	public enum DRTOperationalScheme {
		stationbased, door2door
	}

	public DrtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(STOP_DURATION, "Bus stop duration. Typically, 60 seconds");
		map.put(MAX_WAIT_TIME, "Max wait time for the bus to come. Typically, 15 minutes");
		map.put(CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE,
				"If true, the startLink is changed to last link in the current schedule, so the taxi starts the next"
						+ " day at the link where it stopped operating the day before");
		map.put(VEHICLES_FILE,
				"An XML file specifying the taxi fleet. The file format according to dvrp_vehicles_v1.dtd");
		map.put(PLOT_CUST_STATS, "Writes out detailed DRT customer stats in each iteration.");
		map.put(PLOT_VEH_STATS, "Writes out detailed vehicle stats in each iteration. Creates one file per vehicle and iteration.");
		map.put(DRT_NET_MODE, "DRT Network Mode. Default = car");
		return map;
	}

	/**
	 * @return the drtNetworkMode
	 */
	@StringGetter(DRT_NET_MODE)
	public String getDrtNetworkMode() {
		return drtNetworkMode;
	}
	
	/**
	 * @param drtNetworkMode the drtNetworkMode to set
	 */
	@StringSetter(DRT_NET_MODE)
	public void setDrtNetworkMode(String drtNetworkMode) {
		this.drtNetworkMode = drtNetworkMode;
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

	/**
	 * @return the operationalScheme
	 */
	@StringGetter(OPERATIONAL_SCHEME)
	public DRTOperationalScheme getOperationalScheme() {
		return operationalScheme;
	}

	/**
	 * @param operationalScheme
	 *            the operationalScheme to set
	 */
	@StringSetter(OPERATIONAL_SCHEME)
	public void setOperationalScheme(String operationalScheme) {

		this.operationalScheme = DRTOperationalScheme.valueOf(operationalScheme);
	}

	/**
	 * @return the transitStopFile
	 */
	@StringGetter(TRANSIT_STOP_FILE)
	public String getTransitStopFile() {
		return transitStopFile;
	}

	/**
	 * @param transitStopFile
	 *            the transitStopFile to set
	 */
	@StringSetter(TRANSIT_STOP_FILE)
	public void setTransitStopFile(String transitStopFile) {
		this.transitStopFile = transitStopFile;
	}

	/**
	 * @return the maximumWalkDistance
	 */
	@StringGetter(MAXIMUM_WALK_DISTANCE)
	public double getMaximumWalkDistance() {
		return maximumWalkDistance;
	}

	/**
	 * @param maximumWalkDistance
	 *            the maximumWalkDistance to set
	 */
	@StringSetter(MAXIMUM_WALK_DISTANCE)
	public void setMaximumWalkDistance(double maximumWalkDistance) {
		this.maximumWalkDistance = maximumWalkDistance;
	}
	
	/**
	 * @return the estimatedSpeed
	 */
	@StringGetter(ESTIMATED_DRT_SPEED)
	public double getEstimatedSpeed() {
		return estimatedDrtSpeed;
	}

	/**
	 * @param estimatedSpeed
	 *            the estimatedSpeed to set
	 */
	@StringSetter(ESTIMATED_DRT_SPEED)
	public void setEstimatedSpeed(double estimatedSpeed) {
		this.estimatedDrtSpeed = estimatedSpeed;
	}

	/**
	 * @return the estimatedBeelineDistanceFactor
	 */
	@StringGetter(ESTIMATED_BEELINE_DISTANCE_FACTOR)
	public double getEstimatedBeelineDistanceFactor() {
		return estimatedBeelineDistanceFactor;
	}

	/**
	 * @param estimatedBeelineDistanceFactor
	 *            the estimatedBeelineDistanceFactor to set
	 */
	@StringSetter(ESTIMATED_BEELINE_DISTANCE_FACTOR)
	public void setEstimatedBeelineDistanceFactor(double estimatedBeelineDistanceFactor) {
		this.estimatedBeelineDistanceFactor = estimatedBeelineDistanceFactor;
	}
	/**
	 * @return the plotDetailedCustomerStats
	 */
	@StringGetter(PLOT_CUST_STATS)
	public boolean isPlotDetailedCustomerStats() {
		return plotDetailedCustomerStats;
	}
	/**
	 * @param plotDetailedCustomerStats the plotDetailedCustomerStats to set
	 */
	@StringSetter(PLOT_CUST_STATS)
	public void setPlotDetailedCustomerStats(boolean plotDetailedCustomerStats) {
		this.plotDetailedCustomerStats = plotDetailedCustomerStats;
	}
	/**
	 * @return the plotDetailedVehicleStats
	 */
	@StringGetter(PLOT_VEH_STATS)
	public boolean isPlotDetailedVehicleStats() {
		return plotDetailedVehicleStats;
	}
	/**
	 * @param plotDetailedVehicleStats the plotDetailedVehicleStats to set
	 */
	@StringSetter(PLOT_VEH_STATS)
	public void setPlotDetailedVehicleStats(boolean plotDetailedVehicleStats) {
		this.plotDetailedVehicleStats = plotDetailedVehicleStats;
	}
}
