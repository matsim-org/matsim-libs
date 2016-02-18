/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.Time;

/**
 * The micro-simulation parameters.
 *
 * @author rashid_waraich
 */
public class JDEQSimConfigGroup extends ReflectiveConfigGroup {

	public final static String NAME = "JDEQSim";

	// CONSTANTS
	public static final String START_LEG = "start leg";
	public static final String END_LEG = "end leg";
	public static final String ENTER_LINK = "enter link";
	public static final String LEAVE_LINK = "leave link";
	/**
	 *
	 * the priorities of the messages. a higher priority comes first in the
	 * message queue (when same time) usage: for example a person has a enter
	 * road message at the same time as leaving the previous road (need to keep
	 * the messages in right order) for events with same time stamp: <br>
	 * leave < arrival < departure < enter especially for testing this is
	 * important
	 *
	 */
	public static final int PRIORITY_LEAVE_ROAD_MESSAGE = 200;
	public static final int PRIORITY_ARRIVAL_MESSAGE = 150;
	public static final int PRIORITY_DEPARTUARE_MESSAGE = 125;
	public static final int PRIORITY_ENTER_ROAD_MESSAGE = 100;
	public final static String SQUEEZE_TIME = "squeezeTime";
	public final static String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	public final static String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	public final static String MINIMUM_INFLOW_CAPACITY = "minimumInFlowCapacity";
	public final static String CAR_SIZE = "carSize";
	public final static String GAP_TRAVEL_SPEED = "gapTravelSpeed";
	public final static String END_TIME = "endTime";

	// INPUT
	private double simulationEndTime = Double.MAX_VALUE; // in s
	private double gapTravelSpeed = 15.0; // in m/s
	private double flowCapacityFactor = 1.0;
	private double storageCapacityFactor = 1.0;
	private double carSize = 7.5; // in meter
	// in [vehicles/hour] per lane, can be scaled with flow capacity factor
	private double minimumInFlowCapacity = 1800;
	/**
	 * stuckTime is used for deadlock prevention. when a car waits for more than
	 * 'stuckTime' for entering next road, it will enter the next. in seconds
	 */
	private double squeezeTime = 1800;

	public JDEQSimConfigGroup() {
		super(NAME);
	}

	// should garbage collection of messages be activated
	private static boolean GC_MESSAGES = false;

	public static boolean isGC_MESSAGES() {
		return GC_MESSAGES;
	}

	public static void setGC_MESSAGES(boolean gc_messages) {
		GC_MESSAGES = gc_messages;
	}

	@StringGetter(END_TIME)
	public String getSimulationEndTimeAsString() {
		if (simulationEndTime != Double.MAX_VALUE) {
			return Time.writeTime(simulationEndTime);
		}
		else {
			return Time.writeTime(Time.UNDEFINED_TIME);
		}
	}

	@StringSetter(END_TIME)
	public void setSimulationEndTime(String simulationEndTime) {
		double parsedTime = Time.parseTime(simulationEndTime);
		if (parsedTime != Time.UNDEFINED_TIME) {
			this.simulationEndTime = parsedTime;
		} else {
			parsedTime = Double.MAX_VALUE;
		}
	}

	public double getSimulationEndTime() {
		return simulationEndTime;
	}

	@StringGetter(GAP_TRAVEL_SPEED)
	public double getGapTravelSpeed() {
		return gapTravelSpeed;
	}

	@StringSetter(GAP_TRAVEL_SPEED)
	public void setGapTravelSpeed(double gapTravelSpeed) {
		this.gapTravelSpeed = gapTravelSpeed;
	}

	@StringGetter(FLOW_CAPACITY_FACTOR)
	public double getFlowCapacityFactor() {
		return flowCapacityFactor;
	}

	@StringSetter(FLOW_CAPACITY_FACTOR)
	public void setFlowCapacityFactor(double flowCapacityFactor) {
		this.flowCapacityFactor = flowCapacityFactor;
	}

	@StringGetter(STORAGE_CAPACITY_FACTOR)
	public double getStorageCapacityFactor() {
		return storageCapacityFactor;
	}

	@StringSetter(STORAGE_CAPACITY_FACTOR)
	public void setStorageCapacityFactor(double storageCapacityFactor) {
		this.storageCapacityFactor = storageCapacityFactor;
	}

	@StringGetter(CAR_SIZE)
	public double getCarSize() {
		return carSize;
	}

	@StringSetter(CAR_SIZE)
	public void setCarSize(double carSize) {
		this.carSize = carSize;
	}

	@StringGetter(MINIMUM_INFLOW_CAPACITY)
	public double getMinimumInFlowCapacity() {
		return minimumInFlowCapacity;
	}

	@StringSetter(MINIMUM_INFLOW_CAPACITY)
	public void setMinimumInFlowCapacity(double minimumInFlowCapacity) {
		this.minimumInFlowCapacity = minimumInFlowCapacity;
	}

	@StringGetter(SQUEEZE_TIME)
	public double getSqueezeTime() {
		return squeezeTime;
	}

	@StringSetter(SQUEEZE_TIME)
	public void setSqueezeTime(double squeezeTime) {
		this.squeezeTime = squeezeTime;
	}

}
