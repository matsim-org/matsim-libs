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

package playground.marcel.config.groups;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.utils.misc.Time;

import playground.marcel.config.ConfigGroupI;
import playground.marcel.config.ConfigListI;

public class SimulationConfigGroup implements ConfigGroupI {

	public static final String GROUP_NAME = "simulation";

	private static final String START_TIME = "startTime";
	private static final String END_TIME = "endTime";
	private static final String SNAPSHOT_PERIOD = "snapshotPeriod";
	private static final String SNAPSHOT_FORMAT = "snapshotFormat";
	private static final String FLOW_CAPACITY_FACTOR = "flowCapFactor";
	private static final String STORAGE_CAPACITY_FACTOR = "storageCapFactor";
	private static final String STUCK_TIME = "stuckTime";
	private static final String REMOVE_STUCK_VEHICLES = "removeStuckVehicles";

	private double startTime = Time.UNDEFINED_TIME;
	private double endTime = Time.UNDEFINED_TIME;
	private double snapshotPeriod = 60*60; // 1 hour
	private String snapshotFormat = "netvis";
	private double flowCapFactor = 1.0;
	private double stroageCapFactor = 1.0;
	private double stuckTime = 100;
	private boolean removeStuckVehicles = true;

	private final Set<String> paramKeySet = new LinkedHashSet<String>();
	private final Set<String> listKeySet = new LinkedHashSet<String>();

	public SimulationConfigGroup() {
		this.paramKeySet.add(START_TIME);
		this.paramKeySet.add(END_TIME);
		this.paramKeySet.add(SNAPSHOT_PERIOD);
		this.paramKeySet.add(SNAPSHOT_FORMAT);
		this.paramKeySet.add(FLOW_CAPACITY_FACTOR);
		this.paramKeySet.add(STORAGE_CAPACITY_FACTOR);
		this.paramKeySet.add(STUCK_TIME);
		this.paramKeySet.add(REMOVE_STUCK_VEHICLES);
	}

	public String getName() {
		return GROUP_NAME;
	}

	public String getValue(final String key) {
		if (START_TIME.equals(key)) {
			return Time.writeTime(getStartTime());
		} else if (END_TIME.equals(key)) {
			return Time.writeTime(getEndTime());
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			return Time.writeTime(getSnapshotPeriod());
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			return getSnapshotFormat();
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			return Double.toString(getFlowCapFactor());
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			return Double.toString(getStorageCapFactor());
		} else if (STUCK_TIME.equals(key)) {
			return Double.toString(getStuckTime());
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			return (removeStuckVehicles() ? "true" : "false");
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public void setValue(final String key, final String value) {
		if (START_TIME.equals(key)) {
			setStartTime(Time.parseTime(value));
		} else if (END_TIME.equals(key)) {
			setEndTime(Time.parseTime(value));
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			setSnapshotPeriod(Time.parseTime(value));
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			setSnapshotFormat(value);
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			setFlowCapFactor(Double.parseDouble(value));
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			setStorageCapFactor(Double.parseDouble(value));
		} else if (STUCK_TIME.equals(key)) {
			setStuckTime(Double.parseDouble(value));
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			removeStuckVehicles(value.equals("true") || value.equals("yes"));
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	public ConfigListI getList(final String key) {
		throw new UnsupportedOperationException();
	}

	public Set<String> listKeySet() {
		return this.listKeySet;
	}

	public Set<String> paramKeySet() {
		return this.paramKeySet;
	}

	/* direct access */

	public void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	public double getStartTime() {
		return this.startTime;
	}

	public void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

	public double getEndTime() {
		return this.endTime;
	}

	public void setSnapshotPeriod(final double snapshotPeriod) {
		this.snapshotPeriod = snapshotPeriod;
	}

	public double getSnapshotPeriod() {
		return this.snapshotPeriod;
	}

	public void setSnapshotFormat(final String snapshotFormat) {
		this.snapshotFormat = snapshotFormat;
	}

	public String getSnapshotFormat() {
		return this.snapshotFormat;
	}

	public void setFlowCapFactor(final double flowCapFactor) {
		this.flowCapFactor = flowCapFactor;
	}

	public double getFlowCapFactor() {
		return this.flowCapFactor;
	}

	public void setStorageCapFactor(final double stroageCapFactor) {
		this.stroageCapFactor = stroageCapFactor;
	}

	public double getStorageCapFactor() {
		return this.stroageCapFactor;
	}

	public void setStuckTime(final double stuckTime) {
		this.stuckTime = stuckTime;
	}

	public double getStuckTime() {
		return this.stuckTime;
	}

	public void removeStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	public boolean removeStuckVehicles() {
		return this.removeStuckVehicles;
	}

}
