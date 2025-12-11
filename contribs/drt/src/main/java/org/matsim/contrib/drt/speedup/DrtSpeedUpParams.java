/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.speedup;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * @author ikaddoura
 */
public class DrtSpeedUpParams extends ReflectiveConfigGroup {

	public static final String SET_NAME = "drtSpeedUp";

	public DrtSpeedUpParams() {
		super(SET_NAME);
	}

	@Parameter
	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private double fractionOfIterationsSwitchOff = 0.99;

	@Parameter
	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private double fractionOfIterationsSwitchOn = 0.;

	@Parameter
	@Positive
	private int intervalDetailedIteration = 10;

	@Parameter
	@PositiveOrZero
	private double initialWaitingTime = 900.;

	@Parameter
	@Positive
	private double initialInVehicleBeelineSpeed = 4.16667;

	@Parameter
	@PositiveOrZero
	private int firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams = 0;

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	public double getFractionOfIterationsSwitchOff() {
		return fractionOfIterationsSwitchOff;
	}

	public void setFractionOfIterationsSwitchOff(@DecimalMin("0.0") @DecimalMax("1.0") double fractionOfIterationsSwitchOff) {
		this.fractionOfIterationsSwitchOff = fractionOfIterationsSwitchOff;
	}

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	public double getFractionOfIterationsSwitchOn() {
		return fractionOfIterationsSwitchOn;
	}

	public void setFractionOfIterationsSwitchOn(@DecimalMin("0.0") @DecimalMax("1.0") double fractionOfIterationsSwitchOn) {
		this.fractionOfIterationsSwitchOn = fractionOfIterationsSwitchOn;
	}

	@Positive
	public int getIntervalDetailedIteration() {
		return intervalDetailedIteration;
	}

	public void setIntervalDetailedIteration(@Positive int intervalDetailedIteration) {
		this.intervalDetailedIteration = intervalDetailedIteration;
	}

	@PositiveOrZero
	public double getInitialWaitingTime() {
		return initialWaitingTime;
	}

	public void setInitialWaitingTime(@PositiveOrZero double initialWaitingTime) {
		this.initialWaitingTime = initialWaitingTime;
	}

	@Positive
	public double getInitialInVehicleBeelineSpeed() {
		return initialInVehicleBeelineSpeed;
	}

	public void setInitialInVehicleBeelineSpeed(@Positive double initialInVehicleBeelineSpeed) {
		this.initialInVehicleBeelineSpeed = initialInVehicleBeelineSpeed;
	}

	@PositiveOrZero
	public int getFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams() {
		return firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams;
	}

	public void setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(@PositiveOrZero int firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams) {
		this.firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams = firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams;
	}

	public enum WaitingTimeUpdateDuringSpeedUp {
		Disabled, LinearRegression
	}

	@Parameter
	@NotNull
	public WaitingTimeUpdateDuringSpeedUp waitingTimeUpdateDuringSpeedUp = WaitingTimeUpdateDuringSpeedUp.Disabled;

	@Parameter
	@Positive
	public int movingAverageSize = 1;

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);
		Preconditions.checkArgument(getFractionOfIterationsSwitchOn() <= getFractionOfIterationsSwitchOff(),
				"fractionOfIterationsSwitchOn (%s) must be less than or equal to fractionOfIterationsSwitchOff (%s)",
				getFractionOfIterationsSwitchOn(), getFractionOfIterationsSwitchOff());
	}
}

