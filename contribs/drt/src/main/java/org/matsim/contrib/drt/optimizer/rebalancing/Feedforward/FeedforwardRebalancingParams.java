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

package org.matsim.contrib.drt.optimizer.rebalancing.Feedforward;

import java.net.URL;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import com.google.common.base.Verify;

/**
 * 
 * @author michalm, Chengqi Lu
 */
public final class FeedforwardRebalancingParams extends ReflectiveConfigGroup {
	public static final String SET_NAME = "FeedforwardRebalancingStrategy";

	public enum RebalancingZoneGeneration {
		GridFromNetwork, ShapeFile
	}

	public static final String INTERVAL = "interval";
	static final String INTERVAL_EXP = "Specifies how often empty vehicle rebalancing is executed."
			+ " Must be positive. Default is 300 s. Expects an Integer Value";

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

	private static final String REBALANCING_ZONES_SHAPE_FILE = "rebalancingZonesShapeFile";
	private static final String REBALANCING_ZONES_SHAPE_FILE_EXP = "allows to configure rebalancing zones."
			+ "Used with rebalancingZonesGeneration=ShapeFile";

	public static final String TIME_BIN_SIZE = "timeBinSize";
	static final String TIME_BIN_SIZE_EXP = "Specifies the time bin size of the feedforward signal. Within each time bin, constant DRT demand flow is assumed"
			+ " Must be positive. Default is 900 s. Expects an Integer Value";

	public static final String FEEDFORWARD_SIGNAL_STRENGTH = "feedforwardSignalStrength";
	static final String FEEDFORWARD_SIGNAL_STRENGTH_EXP = "Specifies the strength of the feedforward signal. Expect a double value in the range of [0, 1]"
			+ "where 0 means the feedforward signal is completely turned off and 1 means the feedforward signal is turned on at 100%. Default value is 1";

	public static final String FEEDFORWARD_SIGNAL_LEAD = "feedforwardSignalLead";
	static final String FEEDFORWARD_SIGNAL_LEAD_EXP = "Specifies the lead of the feedforward signal. The feedforward signal can lead the actual time in the simulation, so that"
			+ "the time it takes the vehicles to travel can be compensated to some extent. Expect a non-negative integer value. Default value is 0";

	@Positive
	private int interval = 300;// [s]

	@Positive
	private double minServiceTime = 3600;// [s]

	@PositiveOrZero
	private double maxTimeBeforeIdle = 900;// [s], if 0 then soon-idle vehicle will not be considered

	@Positive
	private double cellSize = Double.NaN;// [m]

	@NotNull
	private RebalancingZoneGeneration rebalancingZonesGeneration = RebalancingZoneGeneration.GridFromNetwork;

	@Nullable
	private String rebalancingZonesShapeFile = null;

	@Positive
	private int timeBinSize = 900; // [s]

	@Positive
	private double feedforwardSignalStrength = 1;

	@Nonnegative
	private int feedforwardSignalLead = 0;

	public FeedforwardRebalancingParams() {
		super(SET_NAME);
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (getMinServiceTime() <= getMaxTimeBeforeIdle()) {
			throw new RuntimeException(FeedforwardRebalancingParams.MIN_SERVICE_TIME + " must be greater than "
					+ FeedforwardRebalancingParams.MAX_TIME_BEFORE_IDLE);
		}

		Verify.verify(
				getRebalancingZonesGeneration() != RebalancingZoneGeneration.ShapeFile
						|| getRebalancingZonesShapeFile() != null,
				REBALANCING_ZONES_SHAPE_FILE + " must not be null when " + REBALANCING_ZONES_GENERATION + " is "
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
		map.put(TIME_BIN_SIZE, TIME_BIN_SIZE_EXP);
		map.put(FEEDFORWARD_SIGNAL_STRENGTH, FEEDFORWARD_SIGNAL_STRENGTH_EXP);
		map.put(FEEDFORWARD_SIGNAL_LEAD, FEEDFORWARD_SIGNAL_LEAD_EXP);
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
	 * @param rebalancingZonesGeneration --
	 *                                   {@value #REBALANCING_ZONES_GENERATION_EXP}
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
	public FeedforwardRebalancingParams setRebalancingZonesShapeFile(String rebalancingZonesShapeFile) {
		this.rebalancingZonesShapeFile = rebalancingZonesShapeFile;
		return this;
	}

	/**
	 * @return timeBinSize -- {@value #TIME_BIN_SIZE_EXP}
	 */
	@StringGetter(TIME_BIN_SIZE)
	public int getTimeBinSize() {
		return timeBinSize;
	}

	/**
	 * @param timeBinSize -- {@value #TIME_BIN_SIZE_EXP}
	 */
	@StringSetter(TIME_BIN_SIZE)
	public void setTimeBinSize(int timeBinSize) {
		this.timeBinSize = timeBinSize;
	}

	/**
	 * @return -- {@value #FEEDFORWARD_SIGNAL_STRENGTH_EXP}
	 */
	@StringGetter(FEEDFORWARD_SIGNAL_STRENGTH)
	public double getFeedforwardSignalStrength() {
		return feedforwardSignalStrength;
	}

	/**
	 * @param interval -- {@value #FEEDFORWARD_SIGNAL_STRENGTH_EXP}
	 */
	@StringSetter(FEEDFORWARD_SIGNAL_STRENGTH)
	public void setFeedforwardSignalStrength(double feedforwardSignalStrength) {
		this.feedforwardSignalStrength = feedforwardSignalStrength;
	}

	/**
	 * @return -- {@value #FEEDFORWARD_SIGNAL_LEAD_EXP}
	 */
	@StringGetter(FEEDFORWARD_SIGNAL_LEAD)
	public int getFeedforwardSignalLead() {
		return feedforwardSignalLead;
	}

	/**
	 * @param interval -- {@value #FEEDFORWARD_SIGNAL_LEAD_EXP}
	 */
	@StringSetter(FEEDFORWARD_SIGNAL_LEAD)
	public void setFeedforwardSignalLead(int feedforwardSignalLead) {
		this.feedforwardSignalLead = feedforwardSignalLead;
	}

}
