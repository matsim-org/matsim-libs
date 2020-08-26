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

package org.matsim.contrib.drt.optimizer.rebalancing.adaptiveRealTime;

import java.util.Map;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * 
 * @author michalm, Chengqi Lu
 */
// TODO change this based on the algorithm!!!
public final class AdaptiveRealTimeRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "AdaptiveRealTimeRebalncingStrategy";

	public AdaptiveRealTimeRebalancingStrategyParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		return map;
	}

}
