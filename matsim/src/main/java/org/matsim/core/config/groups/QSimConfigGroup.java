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
import org.matsim.core.config.Module;
import org.matsim.core.utils.misc.Time;

public class QSimConfigGroup extends Module {

	private static final long serialVersionUID = 1L;

	public static final String GROUP_NAME = "qsim";

	private static final String START_TIME = "startTime";
	private static final String END_TIME = "endTime";
	private static final String TIME_STEP_SIZE = "timeStepSize";
	private static final String SNAPSHOT_PERIOD = "snapshotperiod";
	private static final String SNAPSHOT_FORMAT = "snapshotFormat";
	private static final String SNAPSHOT_STYLE = "snapshotStyle";
	private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	private static final String STUCK_TIME = "stuckTime";
	private static final String REMOVE_STUCK_VEHICLES = "removeStuckVehicles";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String TRAFFIC_DYNAMICS = "trafficDynamics" ;

	private double startTime = Time.UNDEFINED_TIME;
	private double endTime = Time.UNDEFINED_TIME;
	private double timeStepSize = 1.0;
	private double snapshotPeriod = 0; // off, no snapshots
	private String snapshotFormat = "";
	private String snapshotStyle = "equiDist"; // currently supported: queue, equiDist
	private double flowCapFactor = 1.0;
	private double stroageCapFactor = 1.0;
	private double stuckTime = 100;
	private boolean removeStuckVehicles = true;
	private int numberOfThreads = 1;
	private String trafficDynamics = "queue" ;

	public QSimConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	public final void addParam(final String key, final String value) {
		if (START_TIME.equals(key)) {
			setStartTime(Time.parseTime(value));
		} else if (END_TIME.equals(key)) {
			setEndTime(Time.parseTime(value));
		} else if (TIME_STEP_SIZE.equals(key)) {
			setTimeStepSize(Time.parseTime(value));
		} else if (SNAPSHOT_PERIOD.equals(key)) {
			setSnapshotPeriod(Time.parseTime(value));
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			setSnapshotFormat(value);
		} else if (SNAPSHOT_STYLE.equals(key)) {
			setSnapshotStyle(value);
		} else if (FLOW_CAPACITY_FACTOR.equals(key)) {
			setFlowCapFactor(Double.parseDouble(value));
		} else if (STORAGE_CAPACITY_FACTOR.equals(key)) {
			setStorageCapFactor(Double.parseDouble(value));
		} else if (STUCK_TIME.equals(key)) {
			setStuckTime(Double.parseDouble(value));
		} else if (REMOVE_STUCK_VEHICLES.equals(key)) {
			setRemoveStuckVehicles(Boolean.parseBoolean(value));
		} else if (NUMBER_OF_THREADS.equals(key)){
		  setNumberOfThreads(Integer.parseInt(value));
		} else if (TRAFFIC_DYNAMICS.equals(key)) {
			setTrafficDynamics(value) ;
		}	else {
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
		} else if (SNAPSHOT_FORMAT.equals(key)) {
			return getSnapshotFormat();
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
		} else if (NUMBER_OF_THREADS.equals(key)) {
		  return String.valueOf(this.getNumberOfThreads());
		} else if (TRAFFIC_DYNAMICS.equals(key)) {
			return getTrafficDynamics() ;
		}	else {
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
		map.put(SNAPSHOT_FORMAT, getValue(SNAPSHOT_FORMAT));
		map.put(SNAPSHOT_STYLE, getValue(SNAPSHOT_STYLE));
		map.put(FLOW_CAPACITY_FACTOR, getValue(FLOW_CAPACITY_FACTOR));
		map.put(STORAGE_CAPACITY_FACTOR, getValue(STORAGE_CAPACITY_FACTOR));
		map.put(STUCK_TIME, getValue(STUCK_TIME));
		map.put(REMOVE_STUCK_VEHICLES, getValue(REMOVE_STUCK_VEHICLES));
		map.put(NUMBER_OF_THREADS, getValue(NUMBER_OF_THREADS));
		map.put(TRAFFIC_DYNAMICS, getValue(TRAFFIC_DYNAMICS)) ;
		return map;
	}
	
	// measure so that comments remain consistent between Simulation and QSim.  kai, aug'10
	/* package */ static String REMOVE_STUCK_VEHICLES_STRING=
		"Boolean. `true': stuck vehicles are removed, aborting the plan; `false': stuck vehicles are forced into the next link. `false' is probably the better choice.";
	/* package */ static String STUCK_TIME_STRING=
		"time in seconds.  Time after which the frontmost vehicle on a link is called `stuck' if it does not move." ;

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(SNAPSHOT_STYLE,"snapshotStyle: `equiDist' (vehicles equidistant on link) or `queue' (vehicles queued at end of link) or `withHolesExperimental' (experimental!!)");
		map.put(NUMBER_OF_THREADS, "Use number of threads > 1 for parallel version using the specified number of threads");
		map.put(SNAPSHOT_FORMAT, "Comma-separated list of visualizer output file formats.  'plansfile', `transims', `googleearth', and `otfvis'.  'netvis' is, I think, no longer possible.") ;
		map.put(REMOVE_STUCK_VEHICLES, REMOVE_STUCK_VEHICLES_STRING ) ;
		map.put(STUCK_TIME, STUCK_TIME_STRING ) ;
		map.put(TRAFFIC_DYNAMICS, "`queue' for the standard queue model, `withHolesExperimental' (experimental!!) for the queue model with holes") ;
		return map ;
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

	/**
	 * Sets the number of seconds the simulation should advance from one simulated time step to the next.
	 *
	 * @param seconds
	 */
	public void setTimeStepSize(final double seconds) {
		this.timeStepSize = seconds;
	}

	public double getTimeStepSize() {
		return this.timeStepSize;
	}

	public void setSnapshotPeriod(final double snapshotPeriod) {
		this.snapshotPeriod = snapshotPeriod;
	}

	public double getSnapshotPeriod() {
		return this.snapshotPeriod;
	}

	/** See "getComments()" for options.
	 */
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

	public void setRemoveStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	public boolean isRemoveStuckVehicles() {
		return this.removeStuckVehicles;
	}

	/** See {@link #getComments()} for options. */
	public void setSnapshotStyle(final String style) {
		this.snapshotStyle = style.intern();
		if (!"equiDist".equals(this.snapshotStyle) && !"queue".equals(this.snapshotStyle) 
				&& !"withHolesExperimental".equals(this.snapshotStyle) ) {
			Logger.getLogger(this.getClass()).warn("The snapshotStyle \"" + style + "\" is not one of the known ones. "
					+ "See comment in config dump of log file for allowed styles.");
		}
	}

	public String getSnapshotStyle() {
		return this.snapshotStyle;
	}
	
	public void setTrafficDynamics(final String str) {
		this.trafficDynamics = str ;
		if ( !"queue".equals(this.trafficDynamics) && !"withHolesExperimental".equals(this.trafficDynamics) ) {
			Logger.getLogger(this.getClass()).warn("The trafficDynamics \"" + str + "\" is ot one of the known ones. "
					+ "See comment in config dump of log file for allowed styles." ) ;
		}
	}
	
	public String getTrafficDynamics() {
		return this.trafficDynamics ;
	}

  public int getNumberOfThreads() {
    return this.numberOfThreads ;
  }


  public void setNumberOfThreads(final int numberOfThreads) {
    this.numberOfThreads = numberOfThreads;
  }

}
