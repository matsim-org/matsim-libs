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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.Time;

/**
 * @author nagel
 *
 */
public final class QSimConfigGroup extends ReflectiveConfigGroup {


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
	private static final String SIM_ENDTIME_INTERPRETATION = "simEndtimeInterpretation";
	
	public static enum TrafficDynamics { queue, withHoles,
		kinematicWaves //  MATSim-630; previously, the switch was InflowConstraint.maxflowFromFdiag. Amit Jan 2017.
	} ;
	
	public static enum StarttimeInterpretation { maxOfStarttimeAndEarliestActivityEnd, onlyUseStarttime } ;
	public static enum EndtimeInterpretation { minOfEndtimeAndMobsimFinished, onlyUseEndtime } ;

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
	
	//Vehicles of size (in PCU) smaller than or equal to this threshold will be allowed to enter the buffer even
	//if flowcap_accumulate <= 0.
	//The default value is 0.0 meaning that all vehicles of non-zero sizes will be let into buffer only
	//if flowcap_accumulate > 0.
	//
	//Flow capacity easing prevents buses from waiting long time for entering the buffer in sub-sampled scenarios
	//For instance, for 10% scenario, a car (representing 10 cars) has a size of 1.0 PCU, a bus (representing 1 bus)
	//has a size of 0.3 PCU, and link flow capacities are reduced to about 0.1 of the original capacity.
	//(1) If pcuThresholdForFlowCapacityEasing == 0, all buses moving just behind private cars wait long times before
	//entering the buffer (recovering the flow capacity accumulator in a 10% scenario takes approx. 10 times longer than
	//in the 100% scenario).
	//(2) If pcuThresholdForFlowCapacityEasing == 0.3, buses at the front of the queue immediately enter the buffer
	//(once they arrive at the end of a link). If they (one or more buses) have been queued behind a private car,
	//they all leave the link at the same time as the preceding private car.
	private double pcuThresholdForFlowCapacityEasing = 0.0;

	// ---
	private static final String VEHICLE_BEHAVIOR = "vehicleBehavior";
	public static enum VehicleBehavior { teleport, wait, exception } ;
	private VehicleBehavior vehicleBehavior = VehicleBehavior.teleport ;
	// ---
	private static final String SNAPSHOT_STYLE = "snapshotStyle";
	public static enum SnapshotStyle { equiDist, queue, withHoles, withHolesAndShowHoles,
		kinematicWaves /*kinematicWaves and withHoles produce same snapshots Amit Mar'17*/ } ;
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
	private boolean usingFastCapacityUpdate = true ;
	// ---
	private static final String VEHICLES_SOURCE = "vehiclesSource";
	public enum VehiclesSource { defaultVehicle, modeVehicleTypesFromVehiclesData, fromVehiclesData} ;
	private VehiclesSource vehiclesSource = VehiclesSource.defaultVehicle ;
	// ---

//	private static final String CREATING_VEHICLES_FOR_ALL_NETWORK_MODES = "creatingVehiclesForAllNetworkModes";
//	private boolean creatingVehiclesForAllNetworkModes = true;
	// ---
	private static final String IS_SEEP_MODE_STORAGE_FREE = "isSeepModeStorageFree";
	
	private boolean isSeepModeStorageFree = false;

	private EndtimeInterpretation simEndtimeInterpretation;
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
			StringBuilder options = new StringBuilder();
			for ( SnapshotStyle style : SnapshotStyle.values() ) {
				options.append(style.toString());
				options.append(' ');
			}
			map.put(SNAPSHOT_STYLE,"snapshotStyle. One of: " + options.toString()) ;
		}
		map.put(NUMBER_OF_THREADS, "Number of threads used for the QSim.  "
				+ "Note that this setting is independent from the \"global\" threads setting.  "
				+ "In contrast to earlier versions, the non-parallel special version is no longer there." ) ;
		map.put(REMOVE_STUCK_VEHICLES, REMOVE_STUCK_VEHICLES_STRING );
		map.put(STUCK_TIME, STUCK_TIME_STRING );

		{
			String options = "" ;
			for ( TrafficDynamics dyn : TrafficDynamics.values() ) {
				options += dyn + " " ;
			}
			map.put(TRAFFIC_DYNAMICS, "options: " + options ) ;
		}
		{ 
			String options = "" ;
			for ( StarttimeInterpretation ii : StarttimeInterpretation.values() ) {
				options += ii + " " ;
			}
			map.put(SIM_STARTTIME_INTERPRETATION, "Options: " + options ) ;
		}
		{
			String options = "" ;
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
		map.put(USING_THREADPOOL, "if the qsim should use as many runners as there are threads (Christoph's dissertation version)"
				+ " or more of them, together with a thread pool (seems to be faster in some situations, but is not tested).") ;
		map.put(FAST_CAPACITY_UPDATE, "If false, the qsim accumulates fractional flows up to one flow unit in every time step.  If true, "
				+ "flows are updated only if an agent wants to enter the link or an agent is added to buffer. "
				+ "Default is true.") ;
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
//		map.put(CREATING_VEHICLES_FOR_ALL_NETWORK_MODES, "If set to true, creates a vehicle for each person corresponding to every network mode. However, " +
//				"this will be overridden if vehicle source is "+ VehiclesSource.fromVehiclesData+".");
		
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
		if ( seconds != 1.0 ) {
			Logger.getLogger(this.getClass()).warn("there are nearly no tests for time step size != 1.0.  Please write such tests and remove "
					+ "this warning. ") ;
		}
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

	@StringSetter(FLOW_CAPACITY_FACTOR)
	public void setFlowCapFactor(final double flowCapFactor) {
		this.flowCapFactor = flowCapFactor;
	}

	@StringGetter(FLOW_CAPACITY_FACTOR)
	public double getFlowCapFactor() {
		return this.flowCapFactor;
	}

	@StringSetter(STORAGE_CAPACITY_FACTOR)
	public void setStorageCapFactor(final double val) {
		this.storageCapFactor = val;
	}

	@StringGetter(STORAGE_CAPACITY_FACTOR)
	public double getStorageCapFactor() {
		return this.storageCapFactor;
	}

	@StringSetter(STUCK_TIME)
	public void setStuckTime(final double stuckTime) {
		this.stuckTime = stuckTime;
	}

	@StringGetter(STUCK_TIME)
	public double getStuckTime() {
		return this.stuckTime;
	}

	@StringSetter(REMOVE_STUCK_VEHICLES)
	public void setRemoveStuckVehicles(final boolean removeStuckVehicles) {
		this.removeStuckVehicles = removeStuckVehicles;
	}

	@StringGetter(REMOVE_STUCK_VEHICLES)
	public boolean isRemoveStuckVehicles() {
		return this.removeStuckVehicles;
	}

	@StringSetter(SNAPSHOT_STYLE)
	public void setSnapshotStyle(final SnapshotStyle style) {
		this.snapshotStyle = style ;
	}

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

	@StringGetter(SIM_ENDTIME_INTERPRETATION)
	public EndtimeInterpretation getSimEndtimeInterpretation() {
		return simEndtimeInterpretation;
	}

	@StringSetter(SIM_ENDTIME_INTERPRETATION)
	public void setSimEndtimeInterpretation(EndtimeInterpretation str) {
		this.simEndtimeInterpretation = str;
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
	public void setLinkDynamics(LinkDynamics linkDynamics) {
		this.linkDynamics = linkDynamics ;
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
	private static final String SEEP_MODE = "seepMode";
	private Collection<String> seepModes = Arrays.asList(TransportMode.bike);
	@StringGetter(SEEP_MODE)
	private String getSeepModesAsString() {
		return CollectionUtils.setToString(new HashSet<>(getSeepModes()));
	}
	@StringSetter(SEEP_MODE)
	private void setSeepModes(String value) {
		setSeepModes(Arrays.asList(value.split(",")));
	}
	public Collection<String> getSeepModes() {
		return seepModes;
	}
	public void setSeepModes(Collection<String> seepModes) {
		this.seepModes = seepModes;
	}
	// ---
	@StringGetter(IS_SEEP_MODE_STORAGE_FREE)
	public boolean isSeepModeStorageFree() {
		return isSeepModeStorageFree;
	}
	@StringSetter(IS_SEEP_MODE_STORAGE_FREE)
	public void setSeepModeStorageFree(boolean isSeepModeStorageFree) {
		this.isSeepModeStorageFree = isSeepModeStorageFree;
	}
	// ---
	private static final String IS_RESTRICTING_SEEPAGE = "isRestrictingSeepage";
	private boolean isRestrictingSeepage = true;
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
		// yyyyyy this should better become a threshold number!  kai, aug'16
		return this.usingTravelTimeCheckInTeleportation ;
	}
	public boolean setUsingTravelTimeCheckInTeleportation( boolean val ) {
		// yyyyyy this should better become a threshold number!  kai, aug'16
		return this.usingTravelTimeCheckInTeleportation = val ;
	}
	
	static final String PCU_THRESHOLD_FOR_FLOW_CAPACITY_EASING = //
			"Flow capacity easing is activated for vehicles of size equal or smaller than the specified threshold. "
			+ "Introduced to minimise the chances of buses being severely delayed in downsampled scenarios";
	
	
	public double getPcuThresholdForFlowCapacityEasing() {
		return pcuThresholdForFlowCapacityEasing;
	}
	
	/**
	 * @param pcuThresholdForFlowCapacityEasing -- {@value #PCU_THRESHOLD_FOR_FLOW_CAPACITY_EASING}
	 */
	public void setPcuThresholdForFlowCapacityEasing(double pcuThresholdForFlowCapacityEasing) {
		this.pcuThresholdForFlowCapacityEasing = pcuThresholdForFlowCapacityEasing;
	}

////	@StringGetter(CREATING_VEHICLES_FOR_ALL_NETWORK_MODES)
//	public boolean isCreatingVehiclesForAllNetworkModes() {
//		// yyyy do we really need this switch?  Quite in general, please try to avoid boolean switches.  kai, may'18
//		return creatingVehiclesForAllNetworkModes;
//	}

////	@StringSetter(CREATING_VEHICLES_FOR_ALL_NETWORK_MODES)
//	public void setCreatingVehiclesForAllNetworkModes(boolean creatingVehiclesForAllNetworkModes) {
//		// yyyy do we really need this switch?  Quite in general, please try to avoid boolean switches.  kai, may'18
//		this.creatingVehiclesForAllNetworkModes = creatingVehiclesForAllNetworkModes;
//	}
	
}
