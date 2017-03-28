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
	public static final String DRTMODE = "drt";

	@SuppressWarnings("deprecation")
	public static DrtConfigGroup get(Config config) {
		return (DrtConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String STOP_DURATION = "stopDuration";
	public static final String MAX_WAIT_TIME = "maxWaitTime";
	public static final String CHANGE_START_LINK_TO_LAST_LINK_IN_SCHEDULE = "changeStartLinkToLastLinkInSchedule";

	private static final String SCHEME = "operationalScheme";
	private static final String TRANSITSCHEDULEFILE = "transitStopFile";
	private static final String WALKDISTANCE = "maximumWalkDistance";
	private static final String SPEEDESTIMATE = "estimatedDRTSpeed";
	private static final String BEELINEDISTANCEESTIMATE = "estimatedBeelineDistanceFactor";
	
	
	// input
	public static final String VEHICLES_FILE = "vehiclesFile";

	private double stopDuration = Double.NaN;// seconds
	private double maxWaitTime = Double.NaN;// seconds
	private boolean changeStartLinkToLastLinkInSchedule = false;

	private String vehiclesFile = null;
	
	private DRTOperationalScheme operationalScheme;			
	private String transitStopFile = null;
	private double maximumWalkDistance;
	private double estimatedSpeed = 25/3.6;
	private double estimatedBeelineDistanceFactor = 1.3;

	
	public enum DRTOperationalScheme {
				stationbased,
				door2door
				};

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
	@StringGetter(SCHEME)
	public DRTOperationalScheme getOperationalScheme() {
		return operationalScheme;
	}
	
	
	/**
	 * @param operationalScheme the operationalScheme to set
	 */
	@StringSetter(SCHEME)
	public void setOperationalScheme(String operationalScheme) {
		
		this.operationalScheme = DRTOperationalScheme.valueOf(operationalScheme);
	}
	
	/**
	 * @return the transitStopFile
	 */
	@StringGetter(TRANSITSCHEDULEFILE)
	public String getTransitStopFile() {
		return transitStopFile;
	}
	
	/**
	 * @param transitStopFile the transitStopFile to set
	 */
	@StringSetter(TRANSITSCHEDULEFILE)
	public void setTransitStopFile(String transitStopFile) {
		this.transitStopFile = transitStopFile;
	}
	
	/**
	 * @return the maximumWalkDistance
	 */
	@StringGetter(WALKDISTANCE)
	public double getMaximumWalkDistance() {
		return maximumWalkDistance;
	}
	
	/**
	 * @param maximumWalkDistance the maximumWalkDistance to set
	 */
	@StringSetter(WALKDISTANCE)
	public void setMaximumWalkDistance(double maximumWalkDistance) {
		this.maximumWalkDistance = maximumWalkDistance;
	}

	/**
	 * @return the estimatedSpeed
	 */
	@StringGetter(SPEEDESTIMATE)
	public double getEstimatedSpeed() {
		return estimatedSpeed;
	}
	
	/**
	 * @param estimatedSpeed the estimatedSpeed to set
	 */
	@StringSetter(SPEEDESTIMATE)
	public void setEstimatedSpeed(double estimatedSpeed) {
		this.estimatedSpeed = estimatedSpeed;
	}
	
	/**
	 * @return the estimatedBeelineDistanceFactor
	 */
	@StringGetter(BEELINEDISTANCEESTIMATE)
	public double getEstimatedBeelineDistanceFactor() {
		return estimatedBeelineDistanceFactor;
	}
	
	/**
	 * @param estimatedBeelineDistanceFactor the estimatedBeelineDistanceFactor to set
	 */
	@StringSetter(BEELINEDISTANCEESTIMATE)
	public void setEstimatedBeelineDistanceFactor(double estimatedBeelineDistanceFactor) {
		this.estimatedBeelineDistanceFactor = estimatedBeelineDistanceFactor;
	}
}
