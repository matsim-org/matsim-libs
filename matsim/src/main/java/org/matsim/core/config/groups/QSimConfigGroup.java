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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public class QSimConfigGroup extends ReflectiveConfigGroup implements MobsimConfigGroupI {

    public static enum LinkDynamics { FIFO, PassingQ }
	private LinkDynamics linkDynamics = LinkDynamics.FIFO ;
	private static final String LINK_DYNAMICS = "linkDynamics" ;

	private final static Logger log = Logger.getLogger(QSimConfigGroup.class);

	public static final String GROUP_NAME = "qsim";

	private static final String START_TIME = "startTime";
	private static final String END_TIME = "endTime";
	private static final String TIME_STEP_SIZE = "timeStepSize";
	private static final String SNAPSHOT_PERIOD = "snapshotperiod";
	private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	private static final String STUCK_TIME = "stuckTime";
	private static final String REMOVE_STUCK_VEHICLES = "removeStuckVehicles";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String TRAFFIC_DYNAMICS = "trafficDynamics";
	private static final String SIM_STARTTIME_INTERPRETATION = "simStarttimeInterpretation";
	private static final String VEHICLE_BEHAVIOR = "vehicleBehavior";
    private static final String USE_PERSON_ID_FOR_MISSING_VEHICLE_ID = "usePersonIdForMissingVehicleId";
    private static final String USE_DEFAULT_VEHICLES = "useDefaultVehicles";

	public static final String TRAFF_DYN_QUEUE = "queue";
	public static final String TRAFF_DYN_W_HOLES = "withHoles";

	public static final String MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END = "maxOfStarttimeAndEarliestActivityEnd";
	public static final String ONLY_USE_STARTTIME = "onlyUseStarttime";

	public static final String VEHICLE_BEHAVIOR_TELEPORT = "teleport";
	public static final String VEHICLE_BEHAVIOR_WAIT = "wait";
	public static final String VEHICLE_BEHAVIOR_EXCEPTION = "exception";
	private static final String NODE_OFFSET = "nodeOffset";

	
	private double startTime = Time.UNDEFINED_TIME;
	private double endTime = Time.UNDEFINED_TIME;
	private double timeStepSize = 1.0;
	private double snapshotPeriod = 0; // off, no snapshots
	private double flowCapFactor = 1.0;
	private double storageCapFactor = 1.0;
	private double stuckTime = 10;
	private boolean removeStuckVehicles = false;
    private boolean usePersonIdForMissingVehicleId = true;
    private boolean useDefaultVehicles = true;
    private int numberOfThreads = 1;
	private String trafficDynamics = TRAFF_DYN_QUEUE;
	private String simStarttimeInterpretation = MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END;
	private String vehicleBehavior = VEHICLE_BEHAVIOR_TELEPORT;
	
	// ---

	private static final String SNAPSHOT_STYLE = "snapshotStyle";
	public static final String SNAPSHOT_EQUI_DIST = "equiDist";
	public static final String SNAPSHOT_AS_QUEUE = "queue";
	public static final String SNAPSHOT_WITH_HOLES = "withHoles" ;
	private String snapshotStyle = SNAPSHOT_EQUI_DIST;
	
	// ---
	
	private static final String MAIN_MODE = "mainMode";
	private Collection<String> mainModes = Arrays.asList(TransportMode.car);
	
	// ---
	private static final String INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES = "insertingWaitingVehiclesBeforeDrivingVehicles";
	private boolean insertingWaitingVehiclesBeforeDrivingVehicles = false;

	private double nodeOffset = 0;
	private float linkWidth = 30;
	private boolean usingThreadpool = false ;

	public static final String LINK_WIDTH = "linkWidth";

	public QSimConfigGroup() {
		super(GROUP_NAME);
	}

    @StringSetter(MAIN_MODE)
    private void setMainModes(String value) {
        setMainModes(Arrays.asList(value.split(",")));
    }

    @StringSetter(SNAPSHOT_PERIOD)
    private void setSnapshotPeriod(String value) {
        setSnapshotPeriod(Time.parseTime(value));
    }

    @StringSetter(TIME_STEP_SIZE)
    private void setTimeStepSize(String value) {
        setTimeStepSize(Time.parseTime(value));
    }

    @StringSetter(END_TIME)
    private void setEndTime(String value) {
        setEndTime(Time.parseTime(value));
    }

    @StringSetter(START_TIME)
    private void setStartTime(String value) {
        setStartTime(Time.parseTime(value));
    }

    @StringGetter(MAIN_MODE)
    private String getMainModesAsString() {
        return CollectionUtils.setToString(new HashSet<>(getMainModes()));
    }

    @StringGetter(SNAPSHOT_PERIOD)
    private String getSnapshotPeriodAsString() {
        return Time.writeTime(getSnapshotPeriod());
    }

    @StringGetter(TIME_STEP_SIZE)
    private String getTimeStepSizeAsString() {
        return Time.writeTime(getTimeStepSize());
    }


    @StringGetter(END_TIME)
    private String getEndTimeAsString() {
        return Time.writeTime(getEndTime());
    }


    @StringGetter(START_TIME)
    private String getStartTimeAsString() {
        return Time.writeTime(getStartTime());
    }

    // measure so that comments remain consistent between Simulation and QSim.  kai, aug'10
	/* package */ final static String REMOVE_STUCK_VEHICLES_STRING=
		"Boolean. `true': stuck vehicles are removed, aborting the plan; `false': stuck vehicles are forced into the next link. `false' is probably the better choice.";
	/* package */ final static String STUCK_TIME_STRING=
		"time in seconds.  Time after which the frontmost vehicle on a link is called `stuck' if it does not move.";

	@Override
	public final Map<String, String> getComments() {
		Map<String,String> map = super.getComments();
		map.put(SNAPSHOT_STYLE,"snapshotStyle. One of: " 
				+ SNAPSHOT_EQUI_DIST + " (vehicles equidistant on link) or " 
				+ SNAPSHOT_AS_QUEUE + " (vehicles queued at end of link) or "
				+ SNAPSHOT_WITH_HOLES + " (experimental!!)" );
		map.put(NUMBER_OF_THREADS, "Number of threads used for the QSim.  "
				+ "Note that this setting is independent from the \"global\" threads setting.  "
				+ "In contrast to earlier versions, the non-parallel special version is no longer there." ) ;
		map.put(REMOVE_STUCK_VEHICLES, REMOVE_STUCK_VEHICLES_STRING );
		map.put(STUCK_TIME, STUCK_TIME_STRING );
		map.put(TRAFFIC_DYNAMICS, "`"
				+ TRAFF_DYN_QUEUE + "' for the standard queue model, `"
				+ TRAFF_DYN_W_HOLES + "' (experimental!!) for the queue model with holes");
		map.put(SIM_STARTTIME_INTERPRETATION, "`"
				+ MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END + "' (default behavior) or `"
				+ ONLY_USE_STARTTIME + "'" );
		map.put(VEHICLE_BEHAVIOR, "Defines what happens if an agent wants to depart, but the specified vehicle is not available. " +
				"One of: " + VEHICLE_BEHAVIOR_TELEPORT + ", " + VEHICLE_BEHAVIOR_WAIT + ", " + VEHICLE_BEHAVIOR_EXCEPTION);
		map.put(MAIN_MODE, "[comma-separated list] Defines which modes are congested modes. Technically, these are the modes that " +
				"the departure handler of the netsimengine handles.  Effective cell size, effective lane width, flow capacity " +
				"factor, and storage capacity factor need to be set with diligence.  Need to be vehicular modes to make sense.");
		map.put(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES, 
				"decides if waiting vehicles enter the network after or before the already driving vehicles were moved. Default: false"); 
		map.put(NODE_OFFSET, "Shortens a link in the visualization, i.e. its start and end point are moved into towards the center. Does not affect traffic flow. ");
		map.put(LINK_WIDTH, "The (initial) width of the links of the network. Use positive floating point values.");
		{
			StringBuilder stb = new StringBuilder() ;
			for ( LinkDynamics ld : LinkDynamics.values() ) {
				stb.append(" ").append(ld.toString());
			}
			map.put(LINK_DYNAMICS, "default: FIFO; options:" + stb ) ;
		}
        map.put(USE_PERSON_ID_FOR_MISSING_VEHICLE_ID, "If a route does not reference a vehicle, agents will use the vehicle with the same id as their own.");
		map.put(USE_DEFAULT_VEHICLES, "If this is true, we do not expect (or use) vehicles from the vehicles database, but create vehicles on the fly with default properties.");
		map.put(USING_THREADPOOL, "if the qsim should use as many runners as there are threads (Christoph's dissertation version)"
				+ " or more of them, together with a thread pool (seems to be faster in some situations, but is not tested).") ;
        return map;
	}

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
		if ( seconds != 1.0 ) {
			Logger.getLogger(this.getClass()).warn("there are nearly no tests for time step size != 1.0.  Please write such tests and remove "
				+ "this warning. ") ;
		}
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

    @StringSetter(FLOW_CAPACITY_FACTOR)
	public void setFlowCapFactor(final double flowCapFactor) {
		this.flowCapFactor = flowCapFactor;
	}

	@Override
    @StringGetter(FLOW_CAPACITY_FACTOR)
	public double getFlowCapFactor() {
		return this.flowCapFactor;
	}

    @StringSetter(STORAGE_CAPACITY_FACTOR)
	public void setStorageCapFactor(final double val) {
		this.storageCapFactor = val;
	}

	@Override
    @StringGetter(STORAGE_CAPACITY_FACTOR)
	public double getStorageCapFactor() {
		return this.storageCapFactor;
	}

    @StringSetter(STUCK_TIME)
	public void setStuckTime(final double stuckTime) {
		this.stuckTime = stuckTime;
	}

	@Override
    @StringGetter(STUCK_TIME)
	public double getStuckTime() {
		return this.stuckTime;
	}

    @StringSetter(REMOVE_STUCK_VEHICLES)
	public void setRemoveStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	@Override
    @StringGetter(REMOVE_STUCK_VEHICLES)
	public boolean isRemoveStuckVehicles() {
		return this.removeStuckVehicles;
	}

	@StringSetter(SNAPSHOT_STYLE)
	public void setSnapshotStyle(final String style) {
		this.snapshotStyle = style.intern();
		if (!SNAPSHOT_EQUI_DIST.equals(this.snapshotStyle) && !SNAPSHOT_AS_QUEUE.equals(this.snapshotStyle)
				&& !"withHolesExperimental".equals(this.snapshotStyle) ) {
			log.warn("The snapshotStyle \"" + style + "\" is not one of the known ones. "
					+ "See comment in config dump of log file for allowed styles.");
		}
	}

	@Override
    @StringGetter(SNAPSHOT_STYLE)
	public String getSnapshotStyle() {
		return this.snapshotStyle;
	}

    @StringSetter(TRAFFIC_DYNAMICS)
	public void setTrafficDynamics(final String str) {
		this.trafficDynamics = str;
		if ( !TRAFF_DYN_QUEUE.equals(this.trafficDynamics) && !TRAFF_DYN_W_HOLES.equals(this.trafficDynamics) 
				&& !"withHolesExperimental".equals(this.snapshotStyle) ) {
			log.warn("The trafficDynamics \"" + str + "\" is ot one of the known ones. "
					+ "See comment in config dump in log file for allowed styles." );
		}
	}

    @StringGetter(TRAFFIC_DYNAMICS)
	public String getTrafficDynamics() {
		return this.trafficDynamics;
	}

    @StringGetter(NUMBER_OF_THREADS)
	public int getNumberOfThreads() {
		return this.numberOfThreads;
	}

    @StringSetter(NUMBER_OF_THREADS)
	public void setNumberOfThreads(final int numberOfThreads) {
		if ( numberOfThreads < 1 ) {
			throw new IllegalArgumentException( "Number of threads must be strictly positive, got "+numberOfThreads );
		}
		this.numberOfThreads = numberOfThreads;
	}

    @StringGetter(SIM_STARTTIME_INTERPRETATION)
	public String getSimStarttimeInterpretation() {
		return simStarttimeInterpretation;
	}

    @StringSetter(SIM_STARTTIME_INTERPRETATION)
	public void setSimStarttimeInterpretation(String str) {
		this.simStarttimeInterpretation = str;
		if ( !MAX_OF_STARTTIME_AND_EARLIEST_ACTIVITY_END.equals(str)
				&& !ONLY_USE_STARTTIME.equals(str) ) {
			log.warn("The simStarttimeInterpretation '" + str + "' is not one of the known ones. "
					+ "See comment in config dump in log file for allowed styles.");
		}
	}

    @StringSetter(VEHICLE_BEHAVIOR)
	public void setVehicleBehavior(String value) {
		this.vehicleBehavior = value;
	}

    @StringGetter(VEHICLE_BEHAVIOR)
	public String getVehicleBehavior() {
		return this.vehicleBehavior;
	}

	public void setMainModes(Collection<String> mainModes) {
		this.mainModes = mainModes;
	}

	public Collection<String> getMainModes() {
		return mainModes;
	}

    @StringGetter(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES)
	public boolean isInsertingWaitingVehiclesBeforeDrivingVehicles() {
		return this.insertingWaitingVehiclesBeforeDrivingVehicles;
	}

    @StringSetter(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES)
	public void setInsertingWaitingVehiclesBeforeDrivingVehicles(boolean val) {
		this.insertingWaitingVehiclesBeforeDrivingVehicles = val;
	}

    @StringGetter(NODE_OFFSET)
	public double getNodeOffset() {
		return nodeOffset;
	}
	
	@StringSetter(NODE_OFFSET)
    public void setNodeOffset(double nodeOffset) {
		this.nodeOffset = nodeOffset;
	}

    @StringGetter(LINK_WIDTH)
	public float getLinkWidth() {
		return this.linkWidth;
	}

    @StringSetter(LINK_WIDTH)
	public void setLinkWidth(final float linkWidth) {
		this.linkWidth = linkWidth;
	}

    @StringGetter(LINK_DYNAMICS)
	public String getLinkDynamics() {
		return this.linkDynamics.toString() ;
	}

    @StringSetter(LINK_DYNAMICS)
	public void setLinkDynamics( String str ) {
		this.linkDynamics = LinkDynamics.valueOf( str ) ;
	}

    @StringGetter(USE_PERSON_ID_FOR_MISSING_VEHICLE_ID)
    public boolean getUsePersonIdForMissingVehicleId() {
        return usePersonIdForMissingVehicleId;
    }

    @StringSetter(USE_PERSON_ID_FOR_MISSING_VEHICLE_ID)
    public void setUsePersonIdForMissingVehicleId(boolean value) {
        this.usePersonIdForMissingVehicleId = value;
    }

    @StringGetter(USE_DEFAULT_VEHICLES)
    public boolean getUseDefaultVehicles() {
        return useDefaultVehicles;
    }

    @StringSetter(USE_DEFAULT_VEHICLES)
    public void setUseDefaultVehicles(boolean useDefaultVehicles) {
        this.useDefaultVehicles = useDefaultVehicles;
    }
    
    private static final String USING_THREADPOOL = "usingThreadpool" ;
    @StringGetter(USING_THREADPOOL)
    public boolean isUsingThreadpool() {
    	return this.usingThreadpool ;
    }
    @StringSetter(USING_THREADPOOL)
    public void setUsingThreadpool( boolean val ) {
    	this.usingThreadpool = val ;
    }

}
