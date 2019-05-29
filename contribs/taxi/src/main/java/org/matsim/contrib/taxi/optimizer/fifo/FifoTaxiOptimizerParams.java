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

package org.matsim.contrib.taxi.optimizer.fifo;

import javax.validation.constraints.Positive;

import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerParams;

public class FifoTaxiOptimizerParams extends DefaultTaxiOptimizerParams {
	public static final String SET_NAME = "FifoTaxiOptimizer";
	@Positive
	private int reoptimizationTimeStep = 1;

	public FifoTaxiOptimizerParams() {
		super(SET_NAME, true, true);
	}

	@StringGetter(REOPTIMIZATION_TIME_STEP)
	public int getReoptimizationTimeStep() {
		return reoptimizationTimeStep;
	}

	@StringSetter(REOPTIMIZATION_TIME_STEP)
	public void setReoptimizationTimeStep(int reoptimizationTimeStep) {
		this.reoptimizationTimeStep = reoptimizationTimeStep;
	}
}
