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

import org.matsim.contrib.taxi.optimizer.AbstractTaxiOptimizerParams;

import jakarta.validation.constraints.Positive;

public final class FifoTaxiOptimizerParams extends AbstractTaxiOptimizerParams {
	public static final String SET_NAME = "FifoTaxiOptimizer";

	@Parameter
	@Comment(REOPTIMIZATION_TIME_STEP_EXP)
	@Positive
	private int reoptimizationTimeStep = 1;

	public FifoTaxiOptimizerParams() {
		super(SET_NAME, true, true);
	}

	/**
	 * {@value #REOPTIMIZATION_TIME_STEP_EXP}
	 */
	public int getReoptimizationTimeStep() {
		return reoptimizationTimeStep;
	}

	public void setReoptimizationTimeStep(@Positive int reoptimizationTimeStep) {
		this.reoptimizationTimeStep = reoptimizationTimeStep;
	}
}
