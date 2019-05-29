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

package org.matsim.contrib.etaxi.optimizer.assignment;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.Positive;

import org.matsim.contrib.taxi.optimizer.DefaultTaxiOptimizerParams;
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.core.config.ConfigGroup;

public final class AssignmentETaxiOptimizerParams extends DefaultTaxiOptimizerParams {
	public static final String SET_NAME = "AssignmentETaxiOptimizer";

	public static final String MIN_RELATIVE_SOC = "minRelativeSoc";
	static final String MIN_RELATIVE_SOC_EXP = "Taxis with SOC below this level are considered undercharged"
			+ " and should be recharged. The value must be in (0, 1]."
			+ " The default value is 0.3 (used for simulating Nissan Leaf taxis).";
	// 30% SOC (=6 kWh) is enough to travel 40 km (all AUX off);
	// alternatively, in cold winter, it is enough to travel for 1 hour
	// (for approx. 20 km => 3kWh) with 3 kW-heating on
	@Positive
	@DecimalMax("1.0")
	private double minRelativeSoc = 0.3;

	public static final String SOC_CHECK_TIME_STEP = "socCheckTimeStep";
	static final String SOC_CHECK_TIME_STEP_EXP = "Specifies how often idle vehicles are checked if they have become"
			+ " undercharged. The most undercharged ones will be considered in the vehicle-charger assignment procedure."
			+ " The default value is 300 (used for simulating Nissan Leaf taxis).";
	// in cold winter, 3kW heating consumes 1.25% SOC every 5 min
	@Positive
	private int socCheckTimeStep = 300;

	private AssignmentTaxiOptimizerParams assignmentTaxiOptimizerParams;

	public AssignmentETaxiOptimizerParams() {
		super(SET_NAME, true, true);
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		return type.equals(AssignmentTaxiOptimizerParams.SET_NAME) ?
				new AssignmentTaxiOptimizerParams() :
				super.createParameterSet(type);
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set.getName().equals(AssignmentTaxiOptimizerParams.SET_NAME)) {
			assignmentTaxiOptimizerParams = (AssignmentTaxiOptimizerParams)set;
		}
		super.addParameterSet(set);
	}

	AssignmentTaxiOptimizerParams getAssignmentTaxiOptimizerParams() {
		return assignmentTaxiOptimizerParams;
	}

	@Override
	public int getReoptimizationTimeStep() {
		return assignmentTaxiOptimizerParams.getReoptimizationTimeStep();
	}

	@Override
	public void setReoptimizationTimeStep(int reoptimizationTimeStep) {
		assignmentTaxiOptimizerParams.setReoptimizationTimeStep(reoptimizationTimeStep);
	}

	/**
	 * @return {@value #MIN_RELATIVE_SOC_EXP}
	 */
	@StringGetter(MIN_RELATIVE_SOC)
	public double getMinRelativeSoc() {
		return minRelativeSoc;
	}

	/**
	 * @param minRelativeSoc {@value #MIN_RELATIVE_SOC_EXP}
	 */
	@StringSetter(MIN_RELATIVE_SOC)
	public void setMinRelativeSoc(double minRelativeSoc) {
		this.minRelativeSoc = minRelativeSoc;
	}

	/**
	 * @return {@value #SOC_CHECK_TIME_STEP_EXP}
	 */
	@StringGetter(SOC_CHECK_TIME_STEP)
	public int getSocCheckTimeStep() {
		return socCheckTimeStep;
	}

	/**
	 * @param socCheckTimeStep {@value #SOC_CHECK_TIME_STEP_EXP}
	 */
	@StringSetter(SOC_CHECK_TIME_STEP)
	public void setSocCheckTimeStep(int socCheckTimeStep) {
		this.socCheckTimeStep = socCheckTimeStep;
	}
}
