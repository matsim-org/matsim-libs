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

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

/**
 * @author ikaddoura
 */
public class DrtSpeedUpParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "drtSpeedUp";

	private static final String SWITCH_OFF_FRACTION_ITERATION = "fractionOfIterationsSwitchOff";
	private static final String SWITCH_ON_FRACTION_ITERATION = "fractionOfIterationsSwitchOn";
	private static final String DETAILED_ITERATION_INTERVAL = "intervalDetailedIteration";
	private static final String INITIAL_WAITING_TIME = "initialWaitingTime";
	private static final String INITIAL_IN_VEHICLE_BEELINE_SPEED = "initialInVehicleBeelineSpeed";
	private static final String FIRST_SIMULATED_DRT_ITERATION_TO_REPLACE_INITIAL_DRT_PERFORMANCE_PARAMS = "firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams";
	private static final String WAITING_TIME_UPDATE_DURING_SPEED_UP = "waitingTimeUpdateDuringSpeedUp";
	private static final String MOVING_AVERAGE_SIZE = "movingAverageSize";

	public DrtSpeedUpParams() {
		super(SET_NAME);
	}

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private double fractionOfIterationsSwitchOff = 0.99;

	@DecimalMin("0.0")
	@DecimalMax("1.0")
	private double fractionOfIterationsSwitchOn = 0.;

	@Positive
	private int intervalDetailedIteration = 10;

	@PositiveOrZero
	private double initialWaitingTime = 900.;
	@Positive
	private double initialInVehicleBeelineSpeed = 4.16667;
	@PositiveOrZero
	private int firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams = 0;

	@NotNull
	private WaitingTimeUpdateDuringSpeedUp waitingTimeUpdateDuringSpeedUp = WaitingTimeUpdateDuringSpeedUp.Disabled;

	@Positive
	private int movingAverageSize = 1;

	public enum WaitingTimeUpdateDuringSpeedUp {
		Disabled, LinearRegression
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Preconditions.checkArgument(fractionOfIterationsSwitchOn <= fractionOfIterationsSwitchOff,
				"fractionOfIterationsSwitchOn (%s) must be less than or equal to fractionOfIterationsSwitchOff (%s)",
				fractionOfIterationsSwitchOn, fractionOfIterationsSwitchOff);
	}

	@StringGetter(SWITCH_OFF_FRACTION_ITERATION)
	public double getFractionOfIterationsSwitchOff() {
		return fractionOfIterationsSwitchOff;
	}

	@StringSetter(SWITCH_OFF_FRACTION_ITERATION)
	public void setFractionOfIterationsSwitchOff(double fractionOfIterationsSwitchOff) {
		this.fractionOfIterationsSwitchOff = fractionOfIterationsSwitchOff;
	}

	@StringGetter(SWITCH_ON_FRACTION_ITERATION)
	public double getFractionOfIterationsSwitchOn() {
		return fractionOfIterationsSwitchOn;
	}

	@StringSetter(SWITCH_ON_FRACTION_ITERATION)
	public void setFractionOfIterationsSwitchOn(double fractionOfIterationsSwitchOn) {
		this.fractionOfIterationsSwitchOn = fractionOfIterationsSwitchOn;
	}

	@StringGetter(DETAILED_ITERATION_INTERVAL)
	public int getIntervalDetailedIteration() {
		return intervalDetailedIteration;
	}

	@StringSetter(DETAILED_ITERATION_INTERVAL)
	public void setIntervalDetailedIteration(int intervalDetailedIteration) {
		this.intervalDetailedIteration = intervalDetailedIteration;
	}

	@StringGetter(INITIAL_WAITING_TIME)
	public double getInitialWaitingTime() {
		return initialWaitingTime;
	}

	@StringSetter(INITIAL_WAITING_TIME)
	public void setInitialWaitingTime(double initialWaitingTime) {
		this.initialWaitingTime = initialWaitingTime;
	}

	@StringGetter(INITIAL_IN_VEHICLE_BEELINE_SPEED)
	public double getInitialInVehicleBeelineSpeed() {
		return initialInVehicleBeelineSpeed;
	}

	@StringSetter(INITIAL_IN_VEHICLE_BEELINE_SPEED)
	public void setInitialInVehicleBeelineSpeed(double initialInVehicleBeelineSpeed) {
		this.initialInVehicleBeelineSpeed = initialInVehicleBeelineSpeed;
	}

	@StringGetter(FIRST_SIMULATED_DRT_ITERATION_TO_REPLACE_INITIAL_DRT_PERFORMANCE_PARAMS)
	public int getFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams() {
		return firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams;
	}

	@StringSetter(FIRST_SIMULATED_DRT_ITERATION_TO_REPLACE_INITIAL_DRT_PERFORMANCE_PARAMS)
	public void setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(
			int firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams) {
		this.firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams = firstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams;
	}

	@StringGetter(WAITING_TIME_UPDATE_DURING_SPEED_UP)
	public WaitingTimeUpdateDuringSpeedUp getWaitingTimeUpdateDuringSpeedUp() {
		return waitingTimeUpdateDuringSpeedUp;
	}

	@StringSetter(WAITING_TIME_UPDATE_DURING_SPEED_UP)
	public void setWaitingTimeUpdateDuringSpeedUp(WaitingTimeUpdateDuringSpeedUp waitingTimeUpdateDuringSpeedUp) {
		this.waitingTimeUpdateDuringSpeedUp = waitingTimeUpdateDuringSpeedUp;
	}

	@StringGetter(MOVING_AVERAGE_SIZE)
	public int getMovingAverageSize() {
		return movingAverageSize;
	}

	@StringSetter(MOVING_AVERAGE_SIZE)
	public void setMovingAverageSize(int movingAverageSize) {
		this.movingAverageSize = movingAverageSize;
	}
}

