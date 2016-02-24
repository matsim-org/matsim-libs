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
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

public final class QSimConfigGroup extends ReflectiveConfigGroup implements MobsimConfigGroupI {


	@SuppressWarnings("unused")
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
	private static final String USE_PERSON_ID_FOR_MISSING_VEHICLE_ID = "usePersonIdForMissingVehicleId";
	private static final String USE_DEFAULT_VEHICLES = "useDefaultVehicles";

	public static enum TrafficDynamics { queue, withHoles } ;
	
	public static enum StarttimeInterpretation { maxOfStarttimeAndEarliestActivityEnd, onlyUseStarttime } ;

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
	private int numberOfThreads = 1;
	private TrafficDynamics trafficDynamics = TrafficDynamics.queue ;
	private StarttimeInterpretation simStarttimeInterpretation = StarttimeInterpretation.maxOfStarttimeAndEarliestActivityEnd;

	// ---
	private static final String VEHICLE_BEHAVIOR = "vehicleBehavior";
	public static enum VehicleBehavior { teleport, wait, exception } ;
	private VehicleBehavior vehicleBehavior = VehicleBehavior.teleport ;
	// ---
	private static final String SNAPSHOT_STYLE = "snapshotStyle";
	public static enum SnapshotStyle { equiDist, queue, withHoles } ;
	private SnapshotStyle snapshotStyle = SnapshotStyle.equiDist ;

	// ---
	private static final String MAIN_MODE = "mainMode";
	private Collection<String> mainModes = Arrays.asList(TransportMode.car);

	// ---
	private static final String INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES = "insertingWaitingVehiclesBeforeDrivingVehicles";
	private boolean insertingWaitingVehiclesBeforeDrivingVehicles = false;

	// ---
	public static enum LinkDynamics { FIFO, PassingQ, SeepageQ }
	private LinkDynamics linkDynamics = LinkDynamics.FIFO ;
	private static final String LINK_DYNAMICS = "linkDynamics" ;

	// ---
	private double nodeOffset = 0;
	private float linkWidth = 30;
	private boolean usingThreadpool = true;

	public static final String LINK_WIDTH = "linkWidth";

	// ---
	private final static String FAST_CAPACITY_UPDATE = "usingFastCapacityUpdate";
	private boolean usingFastCapacityUpdate = false ;
	// ---
	private static final String VEHICLES_SOURCE = "vehiclesSource";
	public static enum VehiclesSource { defaultVehicle, fromVehiclesData } ;
	private VehiclesSource vehiclesSource = VehiclesSource.defaultVehicle ;
	// ---
	private static final String SEEP_MODE = "seepMode";
	private static final String IS_SEEP_MODE_STORAGE_FREE = "isSeepModeStorageFree";
	private static final String IS_RESTRICTING_SEEPAGE = "isRestrictingSeepage";
	private String seepMode = "bike";
	private boolean isSeepModeStorageFree = false;
	private boolean isRestrictingSeepage = true;
	// ---
	
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
		{
			String options = "" ;
			for ( SnapshotStyle style : SnapshotStyle.values() ) {
				options += style.toString() + " " ;
			}
			map.put(SNAPSHOT_STYLE,"snapshotStyle. One of: " + options ) ; 
		}
		map.put(NUMBER_OF_THREADS, "Number of threads used for the QSim.  "
				+ "Note that this setting is independent from the \"global\" threads setting.  "
				+ "In contrast to earlier versions, the non-parallel special version is no longer there." ) ;
		map.put(REMOVE_STUCK_VEHICLES, REMOVE_STUCK_VEHICLES_STRING );
		map.put(STUCK_TIME, STUCK_TIME_STRING );

		{
			String options = null ;
			for ( TrafficDynamics dyn : TrafficDynamics.values() ) {
				options += dyn + " " ;
			}
			map.put(TRAFFIC_DYNAMICS, "options: " + options ) ;
		}
		{ 
			String options = null ;
			for ( StarttimeInterpretation ii : StarttimeInterpretation.values() ) {
				options += ii + " " ;
			}
			map.put(SIM_STARTTIME_INTERPRETATION, "Options: " + options ) ;
		}
		{
			String options = null ;
			for ( VehicleBehavior behav : VehicleBehavior.values() ) {
				options += behav + " " ;
			}
			map.put(VEHICLE_BEHAVIOR, "Defines what happens if an agent wants to depart, but the specified vehicle is not available. " +
					"One of: " + options ) ;
		}
		map.put(MAIN_MODE, "[comma-separated list] Defines which modes are congested modes. Technically, these are the modes that " +
				"the departure handler of the netsimengine handles.  Effective cell size, effective lane width, flow capacity " +
				"factor, and storage capacity factor need to be set with diligence.  Need to be vehicular modes to make sense.");
		map.put(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES, 
				"decides if waiting vehicles enter the network after or before the already driving vehicles were moved. Default: false"); 
		map.put(NODE_OFFSET, "Shortens a link in the visualization, i.e. its start and end point are moved into towards the center. Does not affect traffic flow. ");
		map.put(LINK_WIDTH, "The (initial) width of the links of the network. Use positive floating point values. This is used only for visualisation.");
		{
			StringBuilder stb = new StringBuilder() ;
			for ( LinkDynamics ld : LinkDynamics.values() ) {
				stb.append(" ").append(ld.toString());
			}
			map.put(LINK_DYNAMICS, "default: FIFO; options:" + stb ) ;
		}
		map.put(USE_PERSON_ID_FOR_MISSING_VEHICLE_ID, "If a route does not reference a vehicle, agents will use the vehicle with the same id as their own.");
		map.put(USE_DEFAULT_VEHICLES, "[DEPRECATED, use" + VEHICLES_SOURCE + " instead]  If this is true, we do not expect (or use) vehicles from the vehicles database, but create vehicles on the fly with default properties.");
		map.put(USING_THREADPOOL, "if the qsim should use as many runners as there are threads (Christoph's dissertation version)"
				+ " or more of them, together with a thread pool (seems to be faster in some situations, but is not tested).") ;
		map.put(FAST_CAPACITY_UPDATE, "normally, the qsim accumulates fractional flows up to one flow unit.  This is impractical with "
				+ " with smaller PCEs.  If this switch is set to true, cars can enter a link if the accumulated flow is >=0, and the accumulated flow can go "
				+ "into negative.  Will probably become the default eventually.") ;
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		{	
			StringBuilder stb = new StringBuilder() ;
			for ( VehiclesSource src : VehiclesSource.values() ) {
				stb.append(" ").append( src.toString() ) ;
			}
			map.put( VEHICLES_SOURCE, "If vehicles should all be the same default vehicle, or come from the vehicles file, "
					+ "or something else.  Possible values: " + stb );
		}
		map.put(SEEP_MODE, "If link dynamics is set as "+ LinkDynamics.SeepageQ+", set a seep mode. Default is bike.");
		map.put(IS_SEEP_MODE_STORAGE_FREE, "If link dynamics is set as "+ LinkDynamics.SeepageQ+", set to true if seep mode do not consumes any space on the link. Default is false.");
		map.put(IS_RESTRICTING_SEEPAGE, "If link dynamics is set as "+ LinkDynamics.SeepageQ+", set to false if all seep modes should perform seepage. Default is true (better option).");
		return map;
	}

	@StringSetter(FAST_CAPACITY_UPDATE)
	public final void setUsingFastCapacityUpdate( boolean val ) {
		this.usingFastCapacityUpdate = val ;
	}

	@StringGetter(FAST_CAPACITY_UPDATE)
	public final boolean isUsingFastCapacityUpdate() {
		return this.usingFastCapacityUpdate ;
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
	public void setSnapshotStyle(final SnapshotStyle style) {
		this.snapshotStyle = style ;
	}

	@Override
	@StringGetter(SNAPSHOT_STYLE)
	public SnapshotStyle getSnapshotStyle() {
		return this.snapshotStyle;
	}

	@StringSetter(TRAFFIC_DYNAMICS)
	public void setTrafficDynamics(final TrafficDynamics str) {
		this.trafficDynamics = str;
	}

	@StringGetter(TRAFFIC_DYNAMICS)
	public TrafficDynamics getTrafficDynamics() {
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
	public StarttimeInterpretation getSimStarttimeInterpretation() {
		return simStarttimeInterpretation;
	}

	@StringSetter(SIM_STARTTIME_INTERPRETATION)
	public void setSimStarttimeInterpretation(StarttimeInterpretation str) {
		this.simStarttimeInterpretation = str;
	}

	@StringSetter(VEHICLE_BEHAVIOR)
	public void setVehicleBehavior(VehicleBehavior value) {
		this.vehicleBehavior = value;
	}

	@StringGetter(VEHICLE_BEHAVIOR)
	public VehicleBehavior getVehicleBehavior() {
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
	public float getLinkWidthForVis() {
		return this.linkWidth;
	}

	@StringSetter(LINK_WIDTH)
	public void setLinkWidthForVis(final float linkWidth) {
		this.linkWidth = linkWidth;
	}

	@StringGetter(LINK_DYNAMICS)
	public LinkDynamics getLinkDynamics() {
		return this.linkDynamics ;
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

	@StringSetter( VEHICLES_SOURCE)
	public final void setVehiclesSource( VehiclesSource source ) {
		this.vehiclesSource = source ;
	}
	@StringGetter( VEHICLES_SOURCE )
	public final VehiclesSource getVehiclesSource() {
		return this.vehiclesSource ;
	}

	@StringGetter(USE_DEFAULT_VEHICLES)
	@Deprecated // use getVehiclesSource instead. kai, jun'15
	boolean getUseDefaultVehicles() {
		switch( this.vehiclesSource ) {
		case defaultVehicle:
			return true ;
		case fromVehiclesData:
			return false ;
		default:
			throw new RuntimeException( "not implemented") ;
		}
	}
	@StringSetter(USE_DEFAULT_VEHICLES)
	@Deprecated // use setVehiclesSource instead. kai, jun'15
	public void setUseDefaultVehicles(boolean useDefaultVehicles) {
		if ( useDefaultVehicles ) {
			this.vehiclesSource = VehiclesSource.defaultVehicle ;
		} else {
			this.vehiclesSource = VehiclesSource.fromVehiclesData ;
		}
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

	private static final String USE_LANES="useLanes" ;
	private boolean useLanes = false ;

	@StringGetter(USE_LANES)
	public boolean isUseLanes() {
		return this.useLanes;
	}
	@StringSetter(USE_LANES)
	public void setUseLanes(final boolean useLanes) {
		this.useLanes = useLanes;
	}

	// ---
	@StringGetter(SEEP_MODE)
	public String getSeepMode() {
		return seepMode;
	}
	@StringSetter(SEEP_MODE)
	public void setSeepMode(String seepMode) {
		this.seepMode = seepMode;
	}
	@StringGetter(IS_SEEP_MODE_STORAGE_FREE)
	public boolean isSeepModeStorageFree() {
		return isSeepModeStorageFree;
	}
	@StringSetter(IS_SEEP_MODE_STORAGE_FREE)
	public void setSeepModeStorageFree(boolean isSeepModeStorageFree) {
		this.isSeepModeStorageFree = isSeepModeStorageFree;
	}
	@StringGetter(IS_RESTRICTING_SEEPAGE)
	public boolean isRestrictingSeepage() {
		return isRestrictingSeepage;
	}
	@StringSetter(IS_RESTRICTING_SEEPAGE)
	public void setRestrictingSeepage(boolean isRestrictingSeepage) {
		this.isRestrictingSeepage = isRestrictingSeepage;
	}
	// ---
	private boolean usingTravelTimeCheckInTeleportation = false ;
	public boolean isUsingTravelTimeCheckInTeleportation() {
		return this.usingTravelTimeCheckInTeleportation ;
	}
	public boolean setUsingTravelTimeCheckInTeleportation( boolean val ) {
		return this.usingTravelTimeCheckInTeleportation = val ;
	}
}
