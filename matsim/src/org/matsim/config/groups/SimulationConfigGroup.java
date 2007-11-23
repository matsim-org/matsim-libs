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

package org.matsim.config.groups;

import java.util.TreeMap;

import org.matsim.config.Module;
import org.matsim.gbl.Gbl;

public class SimulationConfigGroup extends Module {

	public static final String GROUP_NAME = "simulation";

	private static final String START_TIME = "startTime";
	private static final String END_TIME = "endTime";
	private static final String SNAPSHOT_PERIOD = "snapshotperiod";
	private static final String SNAPSHOT_FORMAT = "snapshotFormat";
	private static final String SNAPSHOT_FILE = "snapshotfile";
	private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	private static final String STUCK_TIME = "stuckTime";
	private static final String REMOVE_STUCK_VEHICLES = "removeStuckVehicles";
	private static final String EVACUATION_TIME = "evacuationTime";
	private static final String EXTERNAL_EXE = "externalExe";
	private static final String TIMEOUT = "timeout";

	private static final String SHELLTYPE = "shellType"; // TODO [MR,DS] should be moved to its own config group
	private static final String JAVACLASSPATH = "classPath"; // _TODO dito...
	private static final String JVMOPTIONS = "JVMOptions";
	private static final String CLIENTLIST = "clientList";
	private static final String LOCALCONFIG = "localConfig";
	private static final String LOCALCONFIGDTD = "localConfigDTD";
	private static final String EXE_PATH = "exePath";

	private double startTime = Gbl.UNDEFINED_TIME;
	private double endTime = Gbl.UNDEFINED_TIME;
	private double snapshotPeriod = 0; // off, no snapshots
	private String snapshotFormat = "";
	private String snapshotFile = "Snapshot";
	private double flowCapFactor = 1.0;
	private double stroageCapFactor = 1.0;
	private double stuckTime = 100;
	private double evacuationTime = 8*3600; // TODO [MR,GL] should be moved to its own config group
	private boolean removeStuckVehicles = true;
	private String externalExe = null;
	private int timeOut = 3600;

	public SimulationConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		if (START_TIME.equals(key)) {
			setStartTime(Gbl.parseTime(value));
		} else if (END_TIME.equals(key)) {
			setEndTime(Gbl.parseTime(value));
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			setSnapshotPeriod(Gbl.parseTime(value));
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			setSnapshotFormat(value);
		} else if (SNAPSHOT_FILE.equals(key)) {
			setSnapshotFile(value);
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			setFlowCapFactor(Double.parseDouble(value));
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			setStorageCapFactor(Double.parseDouble(value));
		} else if (STUCK_TIME.equals(key)) {
			setStuckTime(Double.parseDouble(value));
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			removeStuckVehicles("true".equals(value) || "yes".equals(value));
		} else if (EVACUATION_TIME.equals(key)) {
			setEvacuationTime(Gbl.parseTime(value));
		} else if (EXTERNAL_EXE.equals(key)) {
			setExternalExe(value);
		} else if (TIMEOUT.equals(key)) {
			setExternalTimeOut(Integer.parseInt(value));
		} else if (SHELLTYPE.equals(key) || JAVACLASSPATH.equals(key) || JVMOPTIONS.equals(key)
				|| CLIENTLIST.equals(key) || LOCALCONFIG.equals(key) || LOCALCONFIGDTD.equals(key) || EXE_PATH.equals(key)) {
			System.err.println("WARNING: The config options for the parallel mobsim are no longer supported.");
		} else {
			throw new IllegalArgumentException(key);
		}
	}


	@Override
	public final String getValue(final String key) {
		if (START_TIME.equals(key)) {
			return Gbl.writeTime(getStartTime());
		} else if (END_TIME.equals(key)) {
			return Gbl.writeTime(getEndTime());
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			return Gbl.writeTime(getSnapshotPeriod());
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			return getSnapshotFormat();
		} else if (SNAPSHOT_FILE.equals(key)) {
			return getSnapshotFile();
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			return Double.toString(getFlowCapFactor());
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			return Double.toString(getStorageCapFactor());
		} else if (STUCK_TIME.equals(key)) {
			return Double.toString(getStuckTime());
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			return (removeStuckVehicles() ? "true" : "false");
		} else if (EVACUATION_TIME.equals(key)) {
			return Double.toString(getEvacuationTime());
		} else if (EXTERNAL_EXE.equals(key)) {
			return getExternalExe();
		} else if (TIMEOUT.equals(key)) {
			return Integer.toString(getExternalTimeOut());
		} else {
			throw new IllegalArgumentException(key);
		}
	}

	@Override
	protected final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(START_TIME, getValue(START_TIME));
		map.put(END_TIME, getValue(END_TIME));
		map.put(SNAPSHOT_PERIOD, getValue(SNAPSHOT_PERIOD));
		map.put(SNAPSHOT_FORMAT, getValue(SNAPSHOT_FORMAT));
		map.put(SNAPSHOT_FILE, getValue(SNAPSHOT_FILE));
		map.put(FLOW_CAPACITY_FACTOR, getValue(FLOW_CAPACITY_FACTOR));
		map.put(STORAGE_CAPACITY_FACTOR, getValue(STORAGE_CAPACITY_FACTOR));
		map.put(STUCK_TIME, getValue(STUCK_TIME));
		map.put(REMOVE_STUCK_VEHICLES, getValue(REMOVE_STUCK_VEHICLES));
		map.put(EVACUATION_TIME, getValue(EVACUATION_TIME));
		if (this.externalExe != null) {
			map.put(EXTERNAL_EXE, getValue(EXTERNAL_EXE));
		}
		map.put(TIMEOUT, getValue(TIMEOUT));
		return map;
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

	public void setSnapshotFile(final String snapshotFile) {
		this.snapshotFile = snapshotFile.replace('\\', '/');
	}

	public String getSnapshotFile() {
		return this.snapshotFile;
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

	public void setEvacuationTime(final double evactionTime) {
		this.evacuationTime = evactionTime;
	}

	public double getEvacuationTime() {
		return this.evacuationTime;
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


}
