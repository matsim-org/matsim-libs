/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.util.Map;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author Chengqi Lu
 * @author michalm (Michal Maciejewski)
 */
public final class FeedforwardRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "FeedforwardRebalancingStrategy";

	public static final String TIME_BIN_SIZE = "timeBinSize";
	static final String TIME_BIN_SIZE_EXP = "Specifies the time bin size of the feedforward signal. Within each time bin, constant DRT demand flow is assumed"
			+ " Must be positive. Default is 900 s. Expects an Integer Value";

	public static final String FEEDFORWARD_SIGNAL_STRENGTH = "feedforwardSignalStrength";
	static final String FEEDFORWARD_SIGNAL_STRENGTH_EXP = "Specifies the strength of the feedforward signal. Expect a double value in the range of [0, 1]"
			+ "where 0 means the feedforward signal is completely turned off and 1 means the feedforward signal is turned on at 100%. Default value is 1";

	public static final String FEEDFORWARD_SIGNAL_LEAD = "feedforwardSignalLead";
	static final String FEEDFORWARD_SIGNAL_LEAD_EXP = "Specifies the lead of the feedforward signal. The feedforward signal can lead the actual time in the simulation, so that"
			+ "the time it takes the vehicles to travel can be compensated to some extent. Expect a non-negative integer value. Default value is 0";

	public static final String FEEDBACK_SWITCH = "feedbackSwitch";
	static final String FEEDBACK_SWITCH_EXP =
			"Turn on or off the feedback part in the strategy. Feedback part will mainain a minimum number of vehicles"
					+ " in each zone. Default value is false";

	public static final String MIN_NUM_VEHICLES_PER_ZONE = "minNumVehiclesPerZone";
	static final String MIN_NUM_VEHICLES_PER_ZONE_EXP =
			"The minimum number of vehicles a zone should keep. This value will only be used when feed back "
					+ " switch is true! Expect a non-negative value. Default value is 1";

	@Positive
	private int timeBinSize = 900; // [s]

	@PositiveOrZero
	private double feedforwardSignalStrength = 1;

	@PositiveOrZero
	private int feedforwardSignalLead = 0;

	private boolean feedbackSwitch = false;

	@PositiveOrZero
	private int minNumVehiclesPerZone = 1;

	public FeedforwardRebalancingStrategyParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(TIME_BIN_SIZE, TIME_BIN_SIZE_EXP);
		map.put(FEEDFORWARD_SIGNAL_STRENGTH, FEEDFORWARD_SIGNAL_STRENGTH_EXP);
		map.put(FEEDFORWARD_SIGNAL_LEAD, FEEDFORWARD_SIGNAL_LEAD_EXP);
		map.put(FEEDBACK_SWITCH, FEEDBACK_SWITCH_EXP);
		map.put(MIN_NUM_VEHICLES_PER_ZONE, MIN_NUM_VEHICLES_PER_ZONE_EXP);
		return map;
	}

	/**
	 * @return timeBinSize -- {@value #TIME_BIN_SIZE_EXP}
	 */
	@StringGetter(TIME_BIN_SIZE)
	public int getTimeBinSize() {
		return timeBinSize;
	}

	/**
	 * @param timeBinSize -- {@value #TIME_BIN_SIZE_EXP}
	 */
	@StringSetter(TIME_BIN_SIZE)
	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	/**
	 * @return -- {@value #FEEDFORWARD_SIGNAL_STRENGTH_EXP}
	 */
	@StringGetter(FEEDFORWARD_SIGNAL_STRENGTH)
	public double getFeedforwardSignalStrength() {
		return feedforwardSignalStrength;
	}

	/**
	 * @param feedforwardSignalStrength -- {@value #FEEDFORWARD_SIGNAL_STRENGTH_EXP}
	 */
	@StringSetter(FEEDFORWARD_SIGNAL_STRENGTH)
	public void setFeedforwardSignalStrength(double feedforwardSignalStrength) {
		this.feedforwardSignalStrength = feedforwardSignalStrength;
	}

	/**
	 * @return -- {@value #FEEDFORWARD_SIGNAL_LEAD_EXP}
	 */
	@StringGetter(FEEDFORWARD_SIGNAL_LEAD)
	public int getFeedforwardSignalLead() {
		return feedforwardSignalLead;
	}

	/**
	 * @param feedforwardSignalLead -- {@value #FEEDFORWARD_SIGNAL_LEAD_EXP}
	 */
	@StringSetter(FEEDFORWARD_SIGNAL_LEAD)
	public void setFeedforwardSignalLead(int feedforwardSignalLead) {
		this.feedforwardSignalLead = feedforwardSignalLead;
	}

	/**
	 * @return -- {@value #FEEDBACK_SWITCH_EXP}
	 */
	@StringGetter(FEEDBACK_SWITCH)
	public boolean getFeedbackSwitch() {
		return feedbackSwitch;
	}

	/**
	 * @param feedbackSwitch -- {@value #FEEDBACK_SWITCH_EXP}
	 */
	@StringSetter(FEEDBACK_SWITCH)
	public void setFeedbackSwitch(boolean feedbackSwitch) {
		this.feedbackSwitch = feedbackSwitch;
	}

	/**
	 * @return -- {@value #MIN_NUM_VEHICLES_PER_ZONE_EXP}
	 */
	@StringGetter(MIN_NUM_VEHICLES_PER_ZONE)
	public int getMinNumVehiclesPerZone() {
		return minNumVehiclesPerZone;
	}

	/**
	 * @param minNumVehiclesPerZone -- {@value #MIN_NUM_VEHICLES_PER_ZONE_EXP}
	 */
	@StringSetter(MIN_NUM_VEHICLES_PER_ZONE)
	public void setMinNumVehiclesPerZone(int minNumVehiclesPerZone) {
		this.minNumVehiclesPerZone = minNumVehiclesPerZone;
	}

}
