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

import java.util.Map;

import jakarta.validation.constraints.NotNull;

import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 *
 * @author michalm
 */
public final class PlusOneRebalancingStrategyParams extends ReflectiveConfigGroup
		implements RebalancingParams.RebalancingStrategyParams {
	public static final String SET_NAME = "PlusOneRebalancingStrategy";

	public static final String RELOCATION_CALCULATOR_TYPE = "relocationCalculatorType";
	static final String RELOCATION_CALCULATOR_TYPE_EXP = "specific the zone free relocation calculator. Default is fast heuristic zone free relocation calculator";

	// add entry here when additional calculator is implemented
	public enum ZoneFreeRelocationCalculatorType {
		FastHeuristic
	}

	@NotNull
	private ZoneFreeRelocationCalculatorType zoneFreeRelocationCalculatorType = ZoneFreeRelocationCalculatorType.FastHeuristic;

	public PlusOneRebalancingStrategyParams() {
		super(SET_NAME);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(RELOCATION_CALCULATOR_TYPE, RELOCATION_CALCULATOR_TYPE_EXP);
		return map;
	}

	/**
	 * @return -- {@value #RELOCATION_CALCULATOR_TYPE_EXP}
	 */
	@StringGetter(RELOCATION_CALCULATOR_TYPE)
	public ZoneFreeRelocationCalculatorType getZoneFreeRelocationCalculatorType() {
		return zoneFreeRelocationCalculatorType;
	}

	/**
	 * @param zoneFreeRelocationCalculatorType -- {@value #RELOCATION_CALCULATOR_TYPE_EXP}
	 */
	@StringSetter(RELOCATION_CALCULATOR_TYPE)
	public void setRelocationCalculatorType(ZoneFreeRelocationCalculatorType zoneFreeRelocationCalculatorType) {
		this.zoneFreeRelocationCalculatorType = zoneFreeRelocationCalculatorType;
	}

}
