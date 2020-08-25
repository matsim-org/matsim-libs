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

package org.matsim.contrib.drt.optimizer.rebalancing;

import java.net.URL;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Preconditions;

/**
 * @author michalm
 */
public final class RebalancingParams extends ReflectiveConfigGroup {
	public interface RebalancingStrategyParams {
	}

	public static final String SET_NAME = "rebalancing";

	public enum RebalancingZoneGeneration {GridFromNetwork, ShapeFile}

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

	public static final String CELL_SIZE = "cellSize";
	static final String CELL_SIZE_EXP = "size of square cells used for demand aggregation."
			+ " Depends on demand, supply and network. Often used with values in the range of 500 - 2000 m";

	public static final String REBALANCING_ZONES_GENERATION = "rebalancingZonesGeneration";
	static final String REBALANCING_ZONES_GENERATION_EXP = "Logic for generation of zones for demand estimation while rebalancing. Value can be GridFromNetwork or ShapeFile Default is GridFromNetwork";

	public static final String REBALANCING_ZONES_SHAPE_FILE = "rebalancingZonesShapeFile";
	private static final String REBALANCING_ZONES_SHAPE_FILE_EXP = "allows to configure rebalancing zones."
			+ "Used with rebalancingZonesGeneration=ShapeFile";

	@Positive
	private int interval = 1800;// [s]

	@Positive
	private double minServiceTime = 2 * interval;// [s]

	@PositiveOrZero
	private double maxTimeBeforeIdle = 0.5 * interval;// [s], if 0 then soon-idle vehicle will not be considered

	@Positive
	private double cellSize = Double.NaN;// [m]

	@NotNull
	private RebalancingZoneGeneration rebalancingZonesGeneration = RebalancingZoneGeneration.GridFromNetwork;

	@Nullable
	private String rebalancingZonesShapeFile = null;

	@NotNull
	private RebalancingStrategyParams rebalancingStrategyParams;

	public RebalancingParams() {
		super(SET_NAME);
	}

	public RebalancingStrategyParams getRebalancingStrategyParams() {
		return rebalancingStrategyParams;
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		Preconditions.checkArgument(getMinServiceTime() > getMaxTimeBeforeIdle(),
				RebalancingParams.MIN_SERVICE_TIME + "must be greater than" + RebalancingParams.MAX_TIME_BEFORE_IDLE);

		Preconditions.checkArgument(getRebalancingZonesGeneration() != RebalancingZoneGeneration.ShapeFile
				|| getRebalancingZonesShapeFile() != null, REBALANCING_ZONES_SHAPE_FILE
				+ " must not be null when "
				+ REBALANCING_ZONES_GENERATION
				+ " is "
				+ RebalancingZoneGeneration.ShapeFile);
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(INTERVAL, INTERVAL_EXP);
		map.put(MIN_SERVICE_TIME, MIN_SERVICE_TIME_EXP);
		map.put(MAX_TIME_BEFORE_IDLE, MAX_TIME_BEFORE_IDLE_EXP);
		map.put(CELL_SIZE, CELL_SIZE_EXP);
		map.put(REBALANCING_ZONES_GENERATION, REBALANCING_ZONES_GENERATION_EXP);
		map.put(REBALANCING_ZONES_SHAPE_FILE, REBALANCING_ZONES_SHAPE_FILE_EXP);
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

	/**
	 * @return -- {@value #REBALANCING_ZONES_GENERATION_EXP}
	 */
	@StringGetter(REBALANCING_ZONES_GENERATION)
	public RebalancingZoneGeneration getRebalancingZonesGeneration() {
		return rebalancingZonesGeneration;
	}

	/**
	 * @param rebalancingZonesGeneration -- {@value #REBALANCING_ZONES_GENERATION_EXP}
	 */
	@StringSetter(REBALANCING_ZONES_GENERATION)
	public void setRebalancingZonesGeneration(RebalancingZoneGeneration rebalancingZonesGeneration) {
		this.rebalancingZonesGeneration = rebalancingZonesGeneration;
	}

	/**
	 * @return {@link #REBALANCING_ZONES_SHAPE_FILE_EXP}
	 */
	@StringGetter(REBALANCING_ZONES_SHAPE_FILE)
	public String getRebalancingZonesShapeFile() {
		return rebalancingZonesShapeFile;
	}

	public URL getRebalancingZonesShapeFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, rebalancingZonesShapeFile);
	}

	/**
	 * @param rebalancingZonesShapeFile -- {@link #REBALANCING_ZONES_SHAPE_FILE_EXP}
	 */
	@StringSetter(REBALANCING_ZONES_SHAPE_FILE)
	public void setRebalancingZonesShapeFile(String rebalancingZonesShapeFile) {
		this.rebalancingZonesShapeFile = rebalancingZonesShapeFile;
	}

	@Override
	public ConfigGroup createParameterSet(String type) {
		switch (type) {
			case MinCostFlowRebalancingStrategyParams.SET_NAME:
				return new MinCostFlowRebalancingStrategyParams();
		}

		return super.createParameterSet(type);
	}

	@Override
	public void addParameterSet(ConfigGroup set) {
		if (set instanceof RebalancingStrategyParams) {
			Preconditions.checkState(rebalancingStrategyParams == null,
					"Remove the existing rebalancingStrategyParams before adding a new one");
			this.rebalancingStrategyParams = (RebalancingStrategyParams)set;
		}

		super.addParameterSet(set);
	}

	@Override
	public boolean removeParameterSet(ConfigGroup set) {
		if (set instanceof RebalancingStrategyParams) {
			Preconditions.checkState(rebalancingStrategyParams.equals(set),
					"The existing rebalancingStrategyParams is null. Cannot remove it.");
			rebalancingStrategyParams = null;
		}

		return super.removeParameterSet(set);
	}
}
