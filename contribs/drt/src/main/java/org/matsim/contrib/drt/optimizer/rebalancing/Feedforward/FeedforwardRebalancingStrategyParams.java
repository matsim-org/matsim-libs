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

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author Chengqi Lu
 * @author michalm (Michal Maciejewski)
 */
public final class FeedforwardRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "FeedforwardRebalancingStrategy";

	@Parameter
	@Comment("Specifies the time bin size of the feedforward signal. Within each time bin,"
			+ "constant DRT demand flow is assumed."
			+ " Must be positive. Default is 900 s. Expects an Integer Value")
	@Positive
	public int timeBinSize = 900; // [s]

	@Parameter
	@Comment("Specifies the strength of the feedforward signal. Expect a double value in the range of [0, 1],"
			+ " where 0 means the feedforward signal is completely turned off and 1 means the feedforward signal is turned on at 100%."
			+ " Default value is 1")
	@PositiveOrZero
	public double feedforwardSignalStrength = 1;

	@Parameter
	@Comment("Specifies the lead of the feedforward signal. The feedforward signal can lead the actual time"
			+ "in the simulation, so that the time it takes the vehicles to travel can be compensated to some extent."
			+ " Expect a non-negative integer value. Default value is 0")
	@PositiveOrZero
	public int feedforwardSignalLead = 0;

	@Parameter
	@Comment("Turn on or off the feedback part in the strategy. Feedback part will mainain a minimum number of vehicles"
			+ " in each zone. Default value is false")
	public boolean feedbackSwitch = false;

	@Parameter
	@Comment("The minimum number of vehicles a zone should keep. This value will only be used when feed back "
			+ " switch is true! Expect a non-negative value. Default value is 1")
	@PositiveOrZero
	public int minNumVehiclesPerZone = 1;

	public FeedforwardRebalancingStrategyParams() {
		super(SET_NAME);
	}
}
