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

public class QSimConfigGroup extends Module implements MobsimConfigGroupI {

	private final static Logger log = Logger.getLogger(QSimConfigGroup.class);
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
	private static final String SIM_STARTTIME_INTERPRETATION = "simStarttimeInterpretation" ;
	private static final String VEHICLE_BEHAVIOR = "vehicleBehavior";

	public static final String SNAPSHOT_EQUI_DIST = "equiDist" ;
	public static final String SNAPSHOT_AS_QUEUE = "queue" ;

	public static final String TRAFF_DYN_QUEUE = "queue" ;
	public static final String TRAFF_DYN_W_HOLES = "withHolesExperimental" ;

	public static final String MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END = "maxOfStarttimeAndEarliestActivityEnd" ;
	public static final String ONLY_USE_STARTTIME = "onlyUseStarttime" ;
	
	public static final String VEHICLE_BEHAVIOR_TELEPORT = "teleport";
	public static final String VEHICLE_BEHAVIOR_WAIT = "wait";
	public static final String VEHICLE_BEHAVIOR_EXCEPTION = "exception";

	private double startTime = Time.UNDEFINED_TIME;
	private double endTime = Time.UNDEFINED_TIME;
	private double timeStepSize = 1.0;
	private double snapshotPeriod = 0; // off, no snapshots
	private String snapshotFormat = "";
	private String snapshotStyle = SNAPSHOT_EQUI_DIST ;
	private double flowCapFactor = 1.0;
	private double storageCapFactor = 1.0;
	private double stuckTime = 100;
	private boolean removeStuckVehicles = true;
	private int numberOfThreads = 1;
	private String trafficDynamics = TRAFF_DYN_QUEUE ;
	private String simStarttimeInterpretation = MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END ;
	private String vehicleBehavior = VEHICLE_BEHAVIOR_TELEPORT;

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
		} else if (SIM_STARTTIME_INTERPRETATION.equals(key)) {
			setSimStarttimeInterpretation(value) ;
		} else if (VEHICLE_BEHAVIOR.equals(key)) {
			setVehicleBehavior(value);
		} else {
			throw new IllegalArgumentException(key);
		}
	}


	@Override
	public final String getValue(final String key) {
		throw new RuntimeException("Please don't use `getValue' for QSimConfigGroup; use direct getters instead.  kai, dec'10") ;
	}

	@Override
	public final TreeMap<String, String> getParams() {
		TreeMap<String, String> map = new TreeMap<String, String>();
		map.put(START_TIME, Time.writeTime(getStartTime()));
		map.put(END_TIME, Time.writeTime(getEndTime()));
		map.put(TIME_STEP_SIZE, Time.writeTime(getTimeStepSize()));
		map.put(SNAPSHOT_PERIOD, Time.writeTime(getSnapshotPeriod()));
		map.put(SNAPSHOT_FORMAT, getSnapshotFormat());
		map.put(SNAPSHOT_STYLE, getSnapshotStyle());
		map.put(FLOW_CAPACITY_FACTOR, String.valueOf(getFlowCapFactor()));
		map.put(STORAGE_CAPACITY_FACTOR, String.valueOf(getStorageCapFactor()));
		map.put(STUCK_TIME, String.valueOf(getStuckTime()));
		map.put(REMOVE_STUCK_VEHICLES, String.valueOf(isRemoveStuckVehicles()));
		map.put(NUMBER_OF_THREADS, String.valueOf(getNumberOfThreads()));
		map.put(TRAFFIC_DYNAMICS, getTrafficDynamics());
		map.put(SIM_STARTTIME_INTERPRETATION, getSimStarttimeInterpretation());
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
		map.put(TRAFFIC_DYNAMICS, "`"
				+ TRAFF_DYN_QUEUE + "' for the standard queue model, `"
				+ TRAFF_DYN_W_HOLES + "' (experimental!!) for the queue model with holes") ;
		map.put(SIM_STARTTIME_INTERPRETATION, "`"
				+ MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END + "' (default behavior) or `"
				+ ONLY_USE_STARTTIME + "'" ) ;
		return map ;
	}
	/* direct access */

	public void setStartTime(final double startTime) {
		this.startTime = startTime;
	}

	@Override
	public double getStartTime() {
		return this.startTime;
	}

	public void setEndTime(final double endTime) {
		this.endTime = endTime;
	}

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

	@Override
	public double getTimeStepSize() {
		return this.timeStepSize;
	}

	public void setSnapshotPeriod(final double snapshotPeriod) {
		this.snapshotPeriod = snapshotPeriod;
	}

	@Override
	public double getSnapshotPeriod() {
		return this.snapshotPeriod;
	}

	/** See "getComments()" for options.
	 */
	public void setSnapshotFormat(final String snapshotFormat) {
		this.snapshotFormat = snapshotFormat;
	}

	@Override
	public String getSnapshotFormat() {
		return this.snapshotFormat;
	}

	public void setFlowCapFactor(final double flowCapFactor) {
		this.flowCapFactor = flowCapFactor;
	}

	@Override
	public double getFlowCapFactor() {
		return this.flowCapFactor;
	}

	public void setStorageCapFactor(final double val) {
		this.storageCapFactor = val;
	}

	@Override
	public double getStorageCapFactor() {
		return this.storageCapFactor;
	}

	public void setStuckTime(final double stuckTime) {
		this.stuckTime = stuckTime;
	}

	@Override
	public double getStuckTime() {
		return this.stuckTime;
	}

	public void setRemoveStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	@Override
	public boolean isRemoveStuckVehicles() {
		return this.removeStuckVehicles;
	}

	/** See {@link #getComments()} for options. */
	public void setSnapshotStyle(final String style) {
		this.snapshotStyle = style.intern();
		if (!SNAPSHOT_EQUI_DIST.equals(this.snapshotStyle) && !SNAPSHOT_AS_QUEUE.equals(this.snapshotStyle)
				&& !"withHolesExperimental".equals(this.snapshotStyle) ) {
			log.warn("The snapshotStyle \"" + style + "\" is not one of the known ones. "
					+ "See comment in config dump of log file for allowed styles.");
		}
	}

	@Override
	public String getSnapshotStyle() {
		return this.snapshotStyle;
	}

	public void setTrafficDynamics(final String str) {
		this.trafficDynamics = str ;
		if ( !TRAFF_DYN_QUEUE.equals(this.trafficDynamics) && !TRAFF_DYN_W_HOLES.equals(this.trafficDynamics) ) {
			log.warn("The trafficDynamics \"" + str + "\" is ot one of the known ones. "
					+ "See comment in config dump in log file for allowed styles." ) ;
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

	public String getSimStarttimeInterpretation() {
		return simStarttimeInterpretation;
	}

	public void setSimStarttimeInterpretation(String str) {
		this.simStarttimeInterpretation = str;
		if ( !MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END.equals(str)
				&& !ONLY_USE_STARTTIME.equals(str) ) {
			log.warn("The simStarttimeInterpretation '" + str + "' is not one of the known ones. "
					+ "See comment in config dump in log file for allowed styles.") ;
		}
	}

	public void setVehicleBehavior(String value) {
		this.vehicleBehavior = value;
	}
	
	public String getVehicleBehavior() {
		return this.vehicleBehavior;
	}

}
