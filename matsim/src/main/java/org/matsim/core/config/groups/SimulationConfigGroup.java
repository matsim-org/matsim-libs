/* *********************************************************************** *
 * project: org.matsim.*
 * SimulationConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.misc.Time;

public class SimulationConfigGroup extends ConfigGroup implements MobsimConfigGroupI {

	private final static Logger log = Logger.getLogger(SimulationConfigGroup.class);

	public static final String GROUP_NAME = "simulation";

	private static final String START_TIME = "startTime";
	private static final String END_TIME = "endTime";
	private static final String TIME_STEP_SIZE = "timeStepSize";
	private static final String SNAPSHOT_PERIOD = "snapshotperiod";
	private static final String SNAPSHOT_STYLE = "snapshotStyle";
	private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	private static final String STUCK_TIME = "stuckTime";
	private static final String REMOVE_STUCK_VEHICLES = "removeStuckVehicles";
	private static final String EXTERNAL_EXE = "externalExe";
	private static final String TIMEOUT = "timeout";

	private double startTime = Time.UNDEFINED_TIME;
	private double endTime = Time.UNDEFINED_TIME;
	private double timeStepSize = 1.0;
	private double snapshotPeriod = 0; // off, no snapshots
	private String snapshotStyle = "equiDist"; // currently supported: queue, equiDist
	private double flowCapFactor = 1.0;
	private double stroageCapFactor = 1.0;
	private double stuckTime = 10;
	private boolean removeStuckVehicles = false;
	private String externalExe = null;
	private int timeOut = 3600;

	public SimulationConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		if ( "value".equalsIgnoreCase( value ) ) return;
		if (START_TIME.equals(key)) {
			setStartTime(Time.parseTime(value));
		} else if (END_TIME.equals(key)) {
			setEndTime(Time.parseTime(value));
		} else if (TIME_STEP_SIZE.equals(key)) {
			setTimeStepSize(Time.parseTime(value));
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			setSnapshotPeriod(Time.parseTime(value));
		} else if (SNAPSHOT_STYLE.equals(key)) {
			setSnapshotStyle(value);
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			setFlowCapFactor(Double.parseDouble(value));
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			setStorageCapFactor(Double.parseDouble(value));
		} else if (STUCK_TIME.equals(key)) {
			setStuckTime(Double.parseDouble(value));
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			setRemoveStuckVehicles("true".equals(value) || "yes".equals(value));
		} else if (EXTERNAL_EXE.equals(key)) {
			setExternalExe(value);
		} else if (TIMEOUT.equals(key)) {
			setExternalTimeOut(Integer.parseInt(value));
		} else if ( "snapshotFormat".equals(key) ) {
			log.error( "The config entry `snapshotFormat' was removed from the simulation config group. " +
					"It is now in the controler config group; please move it there.  Aborting ...") ;
			throw new IllegalArgumentException(key);
		} else {
			throw new IllegalArgumentException(key);
		}
	}


	@Override
	public final String getValue(final String key) {
		if (START_TIME.equals(key)) {
			return Time.writeTime(getStartTime());
		} else if (END_TIME.equals(key)) {
			return Time.writeTime(getEndTime());
		} else if (TIME_STEP_SIZE.equals(key)) {
			return Time.writeTime(getTimeStepSize());
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			return Time.writeTime(getSnapshotPeriod());
		} else if (SNAPSHOT_STYLE.equals(key)) {
			return getSnapshotStyle();
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			return Double.toString(getFlowCapFactor());
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			return Double.toString(getStorageCapFactor());
		} else if (STUCK_TIME.equals(key)) {
			return Double.toString(getStuckTime());
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			return (isRemoveStuckVehicles() ? "true" : "false");
		} else if (EXTERNAL_EXE.equals(key)) {
			return getExternalExe();
		} else if (TIMEOUT.equals(key)) {
			return Integer.toString(getExternalTimeOut());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(START_TIME, getValue(START_TIME));
		map.put(END_TIME, getValue(END_TIME));
		map.put(TIME_STEP_SIZE, getValue(TIME_STEP_SIZE));
		map.put(SNAPSHOT_PERIOD, getValue(SNAPSHOT_PERIOD));
		map.put(SNAPSHOT_STYLE, getValue(SNAPSHOT_STYLE));
		map.put(FLOW_CAPACITY_FACTOR, getValue(FLOW_CAPACITY_FACTOR));
		map.put(STORAGE_CAPACITY_FACTOR, getValue(STORAGE_CAPACITY_FACTOR));
		map.put(STUCK_TIME, getValue(STUCK_TIME));
		map.put(REMOVE_STUCK_VEHICLES, getValue(REMOVE_STUCK_VEHICLES));
		if (this.externalExe != null) {
			map.put(EXTERNAL_EXE, getValue(EXTERNAL_EXE));
		}
		map.put(TIMEOUT, getValue(TIMEOUT));
		return map;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(SNAPSHOT_STYLE,"snapshotStyle: `equiDist' (vehicles equidistant on link) or `queue' (vehicles queued at end of link)");
		map.put(REMOVE_STUCK_VEHICLES, QSimConfigGroup.REMOVE_STUCK_VEHICLES_STRING ) ;
		map.put(STUCK_TIME, QSimConfigGroup.STUCK_TIME_STRING ) ;
		return map ;
	}


	/* direct access */

	public void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getStartTime()
	 */
	@Override
	public double getStartTime() {
		return this.startTime;
	}

	public void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getEndTime()
	 */
	@Override
	public double getEndTime() {
		return this.endTime;
	}

	/**
	 * Sets the number of seconds the simulation should advance from one simulated time step to the next.
	 *
	 * @param seconds
	 */
	public void setTimeStepSize(final double seconds) {
		this.timeStepSize = seconds;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getTimeStepSize()
	 */
	@Override
	public double getTimeStepSize() {
		return this.timeStepSize;
	}

	public void setSnapshotPeriod(final double snapshotPeriod) {
		this.snapshotPeriod = snapshotPeriod;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getSnapshotPeriod()
	 */
	@Override
	public double getSnapshotPeriod() {
		return this.snapshotPeriod;
	}

	public void setFlowCapFactor(final double flowCapFactor) {
		this.flowCapFactor = flowCapFactor;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getFlowCapFactor()
	 */
	@Override
	public double getFlowCapFactor() {
		return this.flowCapFactor;
	}

	public void setStorageCapFactor(final double stroageCapFactor) {
		this.stroageCapFactor = stroageCapFactor;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getStorageCapFactor()
	 */
	@Override
	public double getStorageCapFactor() {
		return this.stroageCapFactor;
	}

	public void setStuckTime(final double stuckTime) {
		this.stuckTime = stuckTime;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getStuckTime()
	 */
	@Override
	public double getStuckTime() {
		return this.stuckTime;
	}

	public void setRemoveStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#isRemoveStuckVehicles()
	 */
	@Override
	public boolean isRemoveStuckVehicles() {
		return this.removeStuckVehicles;
	}

	public void setExternalExe(final String externalExe) {
		this.externalExe = externalExe;
	}

	public String getExternalExe() {
		return this.externalExe;
	}

	public void setExternalTimeOut(final int timeOut) {
		this.timeOut = timeOut;
	}

	public int getExternalTimeOut() {
		return this.timeOut;
	}

	/** See {@link #getComments()} for options. */
	public void setSnapshotStyle(final String style) {
		this.snapshotStyle = style.intern();
		if (!"equiDist".equals(this.snapshotStyle) && !"queue".equals(this.snapshotStyle)) {
			log.warn("The snapshotStyle \"" + style + "\" is not one of the known ones (queue, equiDist).");
		}
	}

	/* (non-Javadoc)
	 * @see org.matsim.core.config.groups.MobsimConfigGroupI#getSnapshotStyle()
	 */
	@Override
	public String getSnapshotStyle() {
		return this.snapshotStyle;
	}

}
