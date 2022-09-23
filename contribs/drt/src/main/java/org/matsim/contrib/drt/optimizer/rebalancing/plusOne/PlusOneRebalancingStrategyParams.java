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

package org.matsim.contrib.drt.optimizer.rebalancing.plusOne;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

import jakarta.validation.constraints.NotNull;

/**
 * @author michalm
 */
public final class PlusOneRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "PlusOneRebalancingStrategy";

	// add entry here when additional calculator is implemented
	public enum ZoneFreeRelocationCalculatorType {
		FastHeuristic
	}

	@Parameter("relocationCalculatorType")
	@Comment("Specific the zone free relocation calculator. Default is fast heuristic zone free relocation calculator.")
	@NotNull
	public ZoneFreeRelocationCalculatorType zoneFreeRelocationCalculatorType = ZoneFreeRelocationCalculatorType.FastHeuristic;

	public PlusOneRebalancingStrategyParams() {
		super(SET_NAME);
	}
}
