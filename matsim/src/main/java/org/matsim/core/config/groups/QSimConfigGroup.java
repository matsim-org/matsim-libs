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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.vis.snapshotwriters.SnapshotWritersModule;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author nagel
 *
 */
public final class QSimConfigGroup extends ReflectiveConfigGroup {


	@SuppressWarnings("unused")
	private final static Logger log = LogManager.getLogger(QSimConfigGroup.class);

	public static final String GROUP_NAME = "qsim";

	private static final String START_TIME = "startTime";
	private static final String END_TIME = "endTime";
	private static final String TIME_STEP_SIZE = "timeStepSize";
	private static final String SNAPSHOT_PERIOD = "snapshotperiod";
	// measure so that comments remain consistent between Simulation and QSim.  kai, aug'10
	/* package */ final static String REMOVE_STUCK_VEHICLES_STRING =
			"Boolean. `true': stuck vehicles are removed, aborting the plan; `false': stuck vehicles are forced into the next link. `false' is probably the better choice.";
	private static final String FLOW_CAPACITY_FACTOR = "flowCapacityFactor";
	private static final String STORAGE_CAPACITY_FACTOR = "storageCapacityFactor";
	private static final String STUCK_TIME = "stuckTime";
	private static final String REMOVE_STUCK_VEHICLES = "removeStuckVehicles";
	private static final String NUMBER_OF_THREADS = "numberOfThreads";
	private static final String TRAFFIC_DYNAMICS = "trafficDynamics";
	private static final String SIM_STARTTIME_INTERPRETATION = "simStarttimeInterpretation";
	private static final String USE_PERSON_ID_FOR_MISSING_VEHICLE_ID = "usePersonIdForMissingVehicleId";
	private static final String SIM_ENDTIME_INTERPRETATION = "simEndtimeInterpretation";
	/* package */ final static String STUCK_TIME_STRING =
			"time in seconds.  Time after which the frontmost vehicle on a link is called `stuck' if it does not move.";
	private static final String FILTER_SNAPSHOTS = "filterSnapshots";
	private static final String LINK_DYNAMICS = "linkDynamics";
	private InflowCapacitySetting inflowCapacitySetting = InflowCapacitySetting.INFLOW_FROM_FDIAG;

	public enum StarttimeInterpretation {maxOfStarttimeAndEarliestActivityEnd, onlyUseStarttime}

	public enum EndtimeInterpretation {minOfEndtimeAndMobsimFinished, onlyUseEndtime}

	private static final String NODE_OFFSET = "nodeOffset";


	private OptionalTime startTime = OptionalTime.undefined();
	private OptionalTime endTime = OptionalTime.undefined();
	@Positive
	private double timeStepSize = 1.0;
	@PositiveOrZero
	private double snapshotPeriod = 0; // off, no snapshots
	@Positive
	private double flowCapFactor = 1.0;
	@Positive
	private double storageCapFactor = 1.0;
	@Positive
	private double stuckTime = 10;
	private boolean removeStuckVehicles = false;
	private boolean usePersonIdForMissingVehicleId = true;
	@Positive
	private int numberOfThreads = 1;
	//	private static final String CREATING_VEHICLES_FOR_ALL_NETWORK_MODES = "creatingVehiclesForAllNetworkModes";
//	private boolean creatingVehiclesForAllNetworkModes = true;
	// ---
	private static final String IS_SEEP_MODE_STORAGE_FREE = "isSeepModeStorageFree";
	private FilterSnapshots filterSnapshots = FilterSnapshots.no; // include all vehicles by default

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

	public enum VehicleBehavior {teleport, wait, exception}

	private VehicleBehavior vehicleBehavior = VehicleBehavior.teleport;
	// ---
	private static final String SNAPSHOT_STYLE = "snapshotStyle";

	public enum SnapshotStyle {
		equiDist, queue, withHoles, withHolesAndShowHoles,
		kinematicWaves /*kinematicWaves and withHoles produce same snapshots Amit Mar'17*/
	}

	private SnapshotStyle snapshotStyle = SnapshotStyle.queue;

	// ---
	private static final String MAIN_MODE = "mainMode";
	private TrafficDynamics trafficDynamics = TrafficDynamics.queue;


	// ---
	public enum LinkDynamics {FIFO, PassingQ, SeepageQ}

	private LinkDynamics linkDynamics = LinkDynamics.FIFO;
	private Collection<String> mainModes = Collections.singletonList(TransportMode.car);

	// ---
	private double nodeOffset = 0;
	private float linkWidth = 30;

	public static final String LINK_WIDTH = "linkWidth";

	// ---
	private final static String FAST_CAPACITY_UPDATE = "usingFastCapacityUpdate";
	private boolean usingFastCapacityUpdate = true;
	// ---
	private static final String VEHICLES_SOURCE = "vehiclesSource";
	private VehiclesSource vehiclesSource = VehiclesSource.defaultVehicle;
	private Collection<String> seepModes = Collections.singletonList(TransportMode.bike);
	// ---

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		{
			StringBuilder options = new StringBuilder();
			for (SnapshotStyle style : SnapshotStyle.values()) {
				options.append(style.toString());
				options.append(' ');
			}
			map.put(SNAPSHOT_STYLE, "snapshotStyle. One of: " + options);
		}
		map.put(NUMBER_OF_THREADS, "Number of threads used for the QSim.  "
				+ "Note that this setting is independent from the \"global\" threads setting.  "
				+ "In contrast to earlier versions, the non-parallel special version is no longer there.");
		map.put(REMOVE_STUCK_VEHICLES, REMOVE_STUCK_VEHICLES_STRING);
		map.put(STUCK_TIME, STUCK_TIME_STRING);

		{
			StringBuilder options = new StringBuilder(60);
			for (TrafficDynamics dyn : TrafficDynamics.values()) {
				options.append(dyn).append(' ');
			}
			map.put(TRAFFIC_DYNAMICS, "options: " + options);
		}
		{
			StringBuilder options = new StringBuilder(60);
			for (StarttimeInterpretation ii : StarttimeInterpretation.values()) {
				options.append(ii).append(' ');
			}
			map.put(SIM_STARTTIME_INTERPRETATION, "Options: " + options);
		}
		{
			StringBuilder options = new StringBuilder(60);
			for (VehicleBehavior behav : VehicleBehavior.values()) {
				options.append(behav).append(' ');
			}
			map.put(VEHICLE_BEHAVIOR, "Defines what happens if an agent wants to depart, but the specified vehicle is not available. " +
					"One of: " + options);
		}
		map.put(MAIN_MODE, "[comma-separated list] Defines which modes are congested modes. Technically, these are the modes that " +
				"the departure handler of the netsimengine handles.  Effective cell size, effective lane width, flow capacity " +
				"factor, and storage capacity factor need to be set with diligence.  Need to be vehicular modes to make sense.");
		map.put(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES,
				INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES_CMT);
		map.put(NODE_OFFSET, "Shortens a link in the visualization, i.e. its start and end point are moved into towards the center. Does not affect traffic flow. ");
		map.put(LINK_WIDTH, "The (initial) width of the links of the network. Use positive floating point values. This is used only for visualisation.");
		{
			StringBuilder stb = new StringBuilder();
			for (LinkDynamics ld : LinkDynamics.values()) {
				stb.append(" ").append(ld.toString());
			}
			map.put(LINK_DYNAMICS, "default: FIFO; options:" + stb);
		}
		map.put(USE_PERSON_ID_FOR_MISSING_VEHICLE_ID, "If a route does not reference a vehicle, agents will use the vehicle with the same id as their own.");
		map.put(FAST_CAPACITY_UPDATE, "If false, the qsim accumulates fractional flows up to one flow unit in every time step.  If true, "
				+ "flows are updated only if an agent wants to enter the link or an agent is added to buffer. "
				+ "Default is true.");
		map.put(USE_LANES, "Set this parameter to true if lanes should be used, false if not.");
		{
			StringBuilder stb = new StringBuilder();
			for (VehiclesSource src : VehiclesSource.values()) {
				stb.append(" ").append(src.toString());
			}
			map.put(VEHICLES_SOURCE, "If vehicles should all be the same default vehicle, or come from the vehicles file, "
					+ "or something else.  Possible values: " + stb);
		}
		map.put(SEEP_MODE, "If link dynamics is set as " + LinkDynamics.SeepageQ + ", set a seep mode. Default is bike.");
		map.put(IS_SEEP_MODE_STORAGE_FREE, "If link dynamics is set as " + LinkDynamics.SeepageQ + ", set to true if seep mode do not consumes any space on the link. Default is false.");
		map.put(IS_RESTRICTING_SEEPAGE, "If link dynamics is set as " + LinkDynamics.SeepageQ + ", set to false if all seep modes should perform seepage. Default is true (better option).");
		map.put(FILTER_SNAPSHOTS, "If set to " + FilterSnapshots.withLinkAttributes + " snapshots will only be generated for links which include " + SnapshotWritersModule.GENERATE_SNAPSHOT_FOR_LINK_KEY + " as attribute key. Default is no filtering.");
//		map.put(CREATING_VEHICLES_FOR_ALL_NETWORK_MODES, "If set to true, creates a vehicle for each person corresponding to every network mode. However, " +
//				"this will be overridden if vehicle source is "+ VehiclesSource.fromVehiclesData+".");

		return map;
	}

	private boolean isSeepModeStorageFree = false;

	private EndtimeInterpretation simEndtimeInterpretation;

	// ---
	public enum NodeTransition {
		emptyBufferAfterBufferRandomDistribution_dontBlockNode,
		emptyBufferAfterBufferRandomDistribution_nodeBlockedWhenSingleOutlinkFull,
		moveVehByVehRandomDistribution_dontBlockNode,
		moveVehByVehRandomDistribution_nodeBlockedWhenSingleOutlinkFull,
		moveVehByVehDeterministicPriorities_nodeBlockedWhenSingleOutlinkFull
		/* note: moveVehByVehDeterministicPriorities is not implemented for the case when the node is not blocked
		 * as soon as a single outlink is full
		 * theresa, jun'20
		 */
	}
	private NodeTransition nodeTransitionLogic = NodeTransition.emptyBufferAfterBufferRandomDistribution_dontBlockNode;

	// ---

	public QSimConfigGroup() {
		super(GROUP_NAME);
	}

	@StringSetter(MAIN_MODE)
	private void setMainModes(String value) {
		Set<String> modes = Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());
		setMainModes(modes);
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
		this.endTime = Time.parseOptionalTime(value);
	}

	@StringSetter(START_TIME)
	private void setStartTime(String value) {
		this.startTime = Time.parseOptionalTime(value);
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

	@StringGetter(FILTER_SNAPSHOTS)
	public String getFilterSnapshotsAsString() {
		return filterSnapshots.toString();
	}

	public FilterSnapshots getFilterSnapshots() {
		return filterSnapshots;
	}

	@StringSetter(FILTER_SNAPSHOTS)
	public void setFilterSnapshots(String value) {
		this.filterSnapshots = FilterSnapshots.valueOf(value);
	}

	public void setFilterSnapshots(FilterSnapshots value) {
		this.filterSnapshots = value;
	}

	@StringGetter(FAST_CAPACITY_UPDATE)
	public final boolean isUsingFastCapacityUpdate() {
		return this.usingFastCapacityUpdate;
	}

	public void setStartTime(final double startTime) {
		this.startTime = OptionalTime.defined(startTime);
	}

	public OptionalTime getStartTime() {
		return this.startTime;
	}

	public void setEndTime(final double endTime) {
		this.endTime = OptionalTime.defined(endTime);
	}

	public OptionalTime getEndTime() {
		return this.endTime;
	}

	/**
	 * Sets the number of seconds the simulation should advance from one simulated time step to the next.
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

	@StringSetter(FAST_CAPACITY_UPDATE)
	public final void setUsingFastCapacityUpdate(boolean val) {
		this.usingFastCapacityUpdate = val;
	}

	@StringGetter(SNAPSHOT_STYLE)
	public SnapshotStyle getSnapshotStyle() {
		return this.snapshotStyle;
	}

	@StringSetter(SNAPSHOT_STYLE)
	public void setSnapshotStyle(final SnapshotStyle style) {
		this.snapshotStyle = style;
	}

	/**
	 * @param val {@link #INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES_CMT}
	 */
	@StringSetter(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES)
	public void setInsertingWaitingVehiclesBeforeDrivingVehicles(boolean val) {
		this.insertingWaitingVehiclesBeforeDrivingVehicles = val;
	}

	/**
	 * This determines the traffic dynamics of a link. The default is 'queue', but the recommended setting is kinematic waves.
	 *
	 * DEPRECATION NOTE: 'withHoles' is deprecated, use 'kinematicWaves' instead, as that uses 'withHoles' and adds an inflow capacity on top.
	 */
	public enum TrafficDynamics {
		queue,
		@Deprecated
		withHoles,
		kinematicWaves //  MATSim-630; previously, the switch was InflowConstraint.maxflowFromFdiag. Amit Jan 2017.
	}

	/**
	 * Defines how the qsim sets the inflow and/or how it reacts to link attributes which are inconsistent with regard to the fundamental diagram. <br>
	 *
	 * <li>Note that {@code MAX_CAP_FOR_ONE_LANE} is backwards-compatible but always sets the inflow capacity to the maximum according to the fundamental diagram for one lane,
	 * so it essentially sets the inflow capacity too low for multiple-lane-links. DEPRECATED: This is only for backwards compatibility. Use
	 * INFLOW_FROM_FDIAG instead.</li>
	 * <li>{@code INFLOW_FROM_FDIAG} sets the inflow capacity to maximum flow capacity according to the
	 * fundamental diagram, assuming the nr of lanes in the link attributes to be correct.</li>
	 * <li>{@code NR_OF_LANES_FROM_FDIAG} sets the number of lanes to minimum required according to the fundamental
	 * diagram, assuming the flow capacity in the link attributes to be correct. DEPRECATED: In practice the other setting is used most often! Use\
	 * INFLOW_FROM_FDIAG instead.</li>
	 */
	public enum InflowCapacitySetting {INFLOW_FROM_FDIAG,
		@Deprecated
		NR_OF_LANES_FROM_FDIAG,
		@Deprecated
		MAX_CAP_FOR_ONE_LANE}

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
	// ---
	private static final String INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES = "insertingWaitingVehiclesBeforeDrivingVehicles";
	private static final String INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES_CMT =
			"decides if waiting vehicles enter the network after or before the already driving vehicles were moved. Default: false";
	private boolean insertingWaitingVehiclesBeforeDrivingVehicles = true;
	// (yyyyyy switch this default to true; false has really weird consequences sometimes (vehicles waiting for hours in driveway;
	// and this is not included into decongestion approach. kai/ihab, aug'18)

	/**
	 * @return {@value #INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES_CMT}
	 */
	@StringGetter(INSERTING_WAITING_VEHICLES_BEFORE_DRIVING_VEHICLES)
	public boolean isInsertingWaitingVehiclesBeforeDrivingVehicles() {
		return this.insertingWaitingVehiclesBeforeDrivingVehicles;
	}

	public enum FilterSnapshots {no, withLinkAttributes}

	// ---
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
		// yyyy This setting triggers behavior in PrepareForSim, the result of which is also used by the router.  A better place for this switch might be in the vehicles config group. kai, may'21
		testForLocked();
		this.vehiclesSource = source ;
	}
	@StringGetter( VEHICLES_SOURCE )
	public final VehiclesSource getVehiclesSource() {
		// yyyy This setting triggers behavior in PrepareForSim, the result of which is also used by the router.  A better place for this switch might be in the vehicles config group. kai, may'21
		return this.vehiclesSource ;
	}

	private static final String USE_LANES="useLanes" ;
	private boolean useLanes = false;

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

	public enum VehiclesSource {defaultVehicle, modeVehicleTypesFromVehiclesData, fromVehiclesData}

	@StringGetter(SEEP_MODE)
	private String getSeepModesAsString() {
		return CollectionUtils.setToString(new HashSet<>(getSeepModes()));
	}

	@StringSetter(SEEP_MODE)
	private void setSeepModes(String value) {
		Set<String> modes = Arrays.stream(value.split(",")).map(String::trim).collect(Collectors.toSet());
		setSeepModes(modes);
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
		// yyyyyy replace boolean by something more expressive.  kai, aug'18
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
		// yyyyyy replace boolean by something more expressive.  kai, aug'18
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

	public NodeTransition getNodeTransitionLogic() {
		return nodeTransitionLogic;
	}

	public void setNodeTransitionLogic(NodeTransition nodeTransitionLogic) {
		this.nodeTransitionLogic = nodeTransitionLogic;
	}

	public InflowCapacitySetting getInflowCapacitySetting() {
		return this.inflowCapacitySetting;
	}

	public void setInflowCapacitySetting(InflowCapacitySetting inflowCapacitySetting) {
		this.inflowCapacitySetting = inflowCapacitySetting;
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
