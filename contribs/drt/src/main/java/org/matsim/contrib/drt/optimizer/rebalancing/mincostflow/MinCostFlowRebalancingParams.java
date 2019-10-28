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

package org.matsim.contrib.drt.optimizer.rebalancing.mincostflow;

import java.util.Map;

import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author michalm
 */
public final class MinCostFlowRebalancingParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "minCostFlowRebalancing";

	public static final String INTERVAL = "interval";
	static final String INTERVAL_EXP = "Specifies how often empty vehicle rebalancing is executed."
			+ " Must be positive. Default is 1800 s. Expects an Integer Value";

	public static final String MIN_SERVICE_TIME = "minServiceTime";
	static final String MIN_SERVICE_TIME_EXP = //
			"Minimum remaining service time of an idle/busy vehicle to be considered as rebalancable/soon-idle (respectively)."
					+ " Default is 3600 s. In general, should be higher than interval (e.g. 2 x interval).";

	public static final String MAX_TIME_BEFORE_IDLE = "maxTimeBeforeIdle";
	static final String MAX_TIME_BEFORE_IDLE_EXP = //
			"Maximum remaining time before busy vehicle becomes idle to be considered as soon-idle vehicle."
					+ " Default is 900 s. In general should be lower than interval (e.g. 0.5 x interval)";

	public static final String TARGET_ALPHA = "targetAlpha";
	static final String TARGET_ALPHA_EXP = "alpha coefficient in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty milleage.";

	public static final String TARGET_BETA = "targetBeta";
	static final String TARGET_BETA_EXP = "beta constant in linear target calculation."
			+ " In general, should be lower than 1.0 to prevent over-reacting and high empty milleage.";

	public static final String CELL_SIZE = "cellSize";
	static final String CELL_SIZE_EXP = "size of square cells used for demand aggregation."
			+ " Depends on demand, supply and network. Often used with values in the range of 500 - 2000 m";

	@Positive
	private int interval = 1800;// [s]

	@Positive
	private double minServiceTime = 2 * interval;// [s]

	@PositiveOrZero
	private double maxTimeBeforeIdle = 0.5 * interval;// [s], if 0 then soon-idle vehicle will not be considered

	@PositiveOrZero
	private double targetAlpha = Double.NaN;

	@PositiveOrZero
	private double targetBeta = Double.NaN;

	@Positive
	private double cellSize = Double.NaN;// [m]

	public MinCostFlowRebalancingParams() {
		super(SET_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (getMinServiceTime() <= getMaxTimeBeforeIdle()) {
			throw new RuntimeException(MinCostFlowRebalancingParams.MIN_SERVICE_TIME
					+ " must be greater than "
					+ MinCostFlowRebalancingParams.MAX_TIME_BEFORE_IDLE);
		}
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(INTERVAL, INTERVAL_EXP);
		map.put(MIN_SERVICE_TIME, MIN_SERVICE_TIME_EXP);
		map.put(MAX_TIME_BEFORE_IDLE, MAX_TIME_BEFORE_IDLE_EXP);
		map.put(TARGET_ALPHA, TARGET_ALPHA_EXP);
		map.put(TARGET_BETA, TARGET_BETA_EXP);
		map.put(CELL_SIZE, CELL_SIZE_EXP);
		return map;
	}

	/**
	 * @return -- {@value #INTERVAL_EXP}
	 */
	@StringGetter(INTERVAL)
	public int getInterval() {
		return interval;
	}

	/**
	 * @param interval -- {@value #INTERVAL_EXP}
	 */
	@StringSetter(INTERVAL)
	public void setInterval(int interval) {
		this.interval = interval;
	}

	/**
	 * @return -- {@value #MIN_SERVICE_TIME_EXP}
	 */
	@StringGetter(MIN_SERVICE_TIME)
	public double getMinServiceTime() {
		return minServiceTime;
	}

	/**
	 * @param minServiceTime -- {@value #MIN_SERVICE_TIME_EXP}
	 */
	@StringSetter(MIN_SERVICE_TIME)
	public void setMinServiceTime(double minServiceTime) {
		this.minServiceTime = minServiceTime;
	}

	/**
	 * @return -- {@value #MAX_TIME_BEFORE_IDLE_EXP}
	 */
	@StringGetter(MAX_TIME_BEFORE_IDLE)
	public double getMaxTimeBeforeIdle() {
		return maxTimeBeforeIdle;
	}

	/**
	 * @param maxTimeBeforeIdle-- {@value #MAX_TIME_BEFORE_IDLE_EXP}
	 */
	@StringSetter(MAX_TIME_BEFORE_IDLE)
	public void setMaxTimeBeforeIdle(double maxTimeBeforeIdle) {
		this.maxTimeBeforeIdle = maxTimeBeforeIdle;
	}

	/**
	 * @return -- {@value #TARGET_ALPHA_EXP}
	 */
	@StringGetter(TARGET_ALPHA)
	public double getTargetAlpha() {
		return targetAlpha;
	}

	/**
	 * @param targetAlpha -- {@value #TARGET_ALPHA_EXP}
	 */
	@StringSetter(TARGET_ALPHA)
	public void setTargetAlpha(double targetAlpha) {
		this.targetAlpha = targetAlpha;
	}

	/**
	 * @return -- {@value #TARGET_BETA_EXP}
	 */
	@StringGetter(TARGET_BETA)
	public double getTargetBeta() {
		return targetBeta;
	}

	/**
	 * @param targetBeta -- {@value #TARGET_BETA_EXP}
	 */
	@StringSetter(TARGET_BETA)
	public void setTargetBeta(double targetBeta) {
		this.targetBeta = targetBeta;
	}

	/**
	 * @return -- {@value #CELL_SIZE_EXP}
	 */
	@StringGetter(CELL_SIZE)
	public double getCellSize() {
		return cellSize;
	}

	/**
	 * @param cellSize -- {@value #CELL_SIZE_EXP}
	 */
	@StringSetter(CELL_SIZE)
	public void setCellSize(double cellSize) {
		this.cellSize = cellSize;
	}
}
