/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.polettif.publicTransitMapping.config;

import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;


/**
 *
 */
public class PublicTransitMappingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "PublicTransportMap";

	public static final String ARTIFICIAL_LINK_MODE = "artificial";

	private static final String MODE_ROUTING_ASSIGNMENT ="modeRoutingAssignment";
	private static final String MODES_TO_KEEP_ON_CLEAN_UP = "modesToKeepOnCleanUp";
	private static final String NODE_SEARCH_RADIUS = "nodeSearchRadius";
	private static final String PSEUDO_ROUTE_WEIGHT_TYPE = "pseudoRouteWeightType";
	private static final String MAX_NCLOSEST_LINKS = "maxNClosestLinks";
	private static final String MAX_STOP_FACILITY_DISTANCE = "maxStopFacilityDistance";
	private static final String PREFIX_ARTIFICIAL = "prefixArtificial";
	private static final String SUFFIX_CHILD_STOP_FACILITIES = "suffixChildStopFacilities";
	private static final String BEELINE_DISTANCE_MAX_FACTOR = "beelineDistanceMaxFactor";
	private static final String NETWORK_FILE = "networkFile";
	private static final String SCHEDULE_FILE = "scheduleFile";
	private static final String OUTPUT_NETWORK_FILE = "outputNetworkFile";
	private static final String OUTPUT_SCHEDULE_FILE = "outputScheduleFile";
	private static final String OUTPUT_STREET_NETWORK_FILE = "outputStreetNetworkFile";
	private static final String LINK_DISTANCE_TOLERANCE = "linkDistanceTolerance";
	private static final String FREESPEED_ARTIFICIAL = "freespeedArtificialLinks";
	private static final String COMBINE_PT_MODES = "combinePtModes";


	public PublicTransitMappingConfigGroup() {
		super(GROUP_NAME);

		modesToKeepOnCleanUp.add("car");
	}

	private String networkFile = null;
	private String scheduleFile = null;
	private String outputNetworkFile = null;
	private String outputStreetNetworkFile = null;
	private String outputScheduleFile = null;

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODE_ROUTING_ASSIGNMENT,
				"References transportModes from the schedule (key) and the allowed transportModes of a link from \n" +
				"\t\the network (values). Schedule transport modes not defined here are not mapped at all and routes \n" +
				"\t\tusing them are removed. One schedule transport mode can be mapped to multiple network transport \n" +
				"\t\tmodes, the latter have to be separated by \",\". To map a schedule transport mode independently \n" +
				"\t\tfrom the network use \"artificial\". Assignments are separated by \"|\" (case sensitive). \n" +
				"\t\tExample: \"bus:bus,car | rail:rail,light_rail\"");
		map.put(MODES_TO_KEEP_ON_CLEAN_UP,
				"All links that do not have a transit route on them are removed, except the ones \n" +
				"\t\tlisted in this set (typically only car). Separated by comma.");
		map.put(COMBINE_PT_MODES,
				"Defines whether at the end of mapping, all non-car link modes (bus, rail, etc) \n" +
				"\t\tshould be replaced with pt (true) or not. Default: false");
		map.put(LINK_DISTANCE_TOLERANCE,
				"[Link Candidates] After " +MAX_NCLOSEST_LINKS +" link candidates have been found, additional link \n" +
				"\t\tcandidates within "+LINK_DISTANCE_TOLERANCE+"*(distance to the Nth link) are added to the set.\n" +
				"\t\tMust be > 1.");
		map.put(PSEUDO_ROUTE_WEIGHT_TYPE,
				"Defines which link attribute should be used for pseudo route calculations. Default is minimization \n" +
				"\t\tof travel distance. If high quality information on link travel times is available, travelTime can be \n" +
				"\t\tused. (Possible values \""+PseudoRouteWeightType.linkLength+"\" and \""+PseudoRouteWeightType.travelTime+"\")");
		map.put(MAX_NCLOSEST_LINKS,
				"[Link Candidates] Number of link candidates considered for all stops, depends on accuracy of stops and desired \n" +
				"\t\tperformance. Somewhere between 4 and 10 seems reasonable, depending on the accuracy of the stop \n" +
				"\t\tfacility coordinates. Default: " + nodeSearchRadius);
		map.put(NODE_SEARCH_RADIUS,
				"[Link Candidates] Defines the radius [meter] from a stop facility within nodes are searched. Mainly a maximum \n" +
				"\t\tvalue for performance.");
		map.put(MAX_STOP_FACILITY_DISTANCE,
				"[Link Candidates] The maximal distance [meter] a link candidate is allowed to have from the stop facility.");
		map.put(PREFIX_ARTIFICIAL,
				"ID prefix used for all artificial links and nodes created during mapping.");
		map.put(FREESPEED_ARTIFICIAL,
				"The freespeed [m/s] of artificially created links.");
		map.put(SUFFIX_CHILD_STOP_FACILITIES,
				"Suffix used for child stop facilities. The id of the referenced link is appended\n" +
				"\t\t(i.e. stop0123.link:LINKID20123).");
		map.put(BEELINE_DISTANCE_MAX_FACTOR ,
				"If all paths between two stops have a length > beelineDistanceMaxFactor * beelineDistance, \n" +
				"\t\tan artificial link is created.");
		map.put(NETWORK_FILE, "Path to the input network file. Not needed if PTMapper is called within another class.");
		map.put(SCHEDULE_FILE, "Path to the input schedule file. Not needed if PTMapper is called within another class.");
		map.put(OUTPUT_NETWORK_FILE, "Path to the output network file. Not needed if PTMapper is used within another class.");
		map.put(OUTPUT_NETWORK_FILE, "Path to the output car only network file. The inpu multimodal map is filtered. \n" +
				"\t\tNot needed if PTMapper is used within another class.");
		map.put(OUTPUT_SCHEDULE_FILE, "Path to the output schedule file. Not needed if PTMapper is used within another class.");
		return map;
	}


	/**
	 * for each schedule transport the following needs to be specified:
	 * - should it be mapped independently?
	 * - to which network transport modeRouting it can be mapped
	 *
	 * for network transport modeRouting:
	 * - should it be cleaned up
	 */

	/**
	 * References transportModes from the schedule (key) and the
	 * allowed modeRouting of a link from the network (value). <p/>
	 * <p/>
	 * Schedule transport modeRouting should be in gtfs categories:
	 * <ul>
	 * <li>0 - Tram, Streetcar, Light rail. Any light rail or street level system within a metropolitan area.</li>
	 * <li>1 - Subway, Metro. Any underground rail system within a metropolitan area.</li>
	 * <li>2 - Rail. Used for intercity or long-distance travel.</li>
	 * <li>3 - Bus. Used for short- and long-distance bus routes.</li>
	 * <li>4 - Ferry. Used for short- and long-distance boat service.</li>
	 * <li>5 - Cable car. Used for street-level cable cars where the cable runs beneath the car.</li>
	 * <li>6 - Gondola, Suspended cable car. Typically used for aerial cable cars where the car is suspended from the cable.</li>
	 * <li>7 - Funicular. Any rail system designed for steep inclines.</li>
	 * </ul>
	 */
	private Map<String, Set<String>> modeRoutingAssignment = new HashMap<>();

	public Map<String, Set<String>> getModeRoutingAssignment() {
		return this.modeRoutingAssignment;
	}

	public void setModeRoutingAssignment(Map<String, Set<String>> modeRoutingAssignment) {
		this.modeRoutingAssignment = modeRoutingAssignment;
	}

	@StringGetter(MODE_ROUTING_ASSIGNMENT)
	private String getModeRoutingAssignmentString() {
		String ret = "";
		for(Map.Entry<String, Set<String>> entry : modeRoutingAssignment.entrySet()) {
			ret += "|" + entry.getKey() + ":";
			String value = "";
			for(String mode : entry.getValue()) {
				value += "," + mode;
			}
			ret += value.substring(1);
		}
		return this.modeRoutingAssignment.size() == 0 ? "" : ret.substring(1);
	}

	@StringSetter(MODE_ROUTING_ASSIGNMENT)
	private void setModeRoutingAssignmentString(String modeRoutingAssignmentString) {
		if(modeRoutingAssignmentString == null) {
			this.modeRoutingAssignment = null;
			return;
		}

		for(String assignment : modeRoutingAssignmentString.split("\\|")) {
			String[] tuple = assignment.split(":");
			Set<String> set = new HashSet<>();
			for(String networkMode : tuple[1].trim().split(",")) {
				set.add(networkMode.trim());
			}
			this.modeRoutingAssignment.put(tuple[0].trim(), set);
		}
	}


	/**
	 * All links that do not have a transit route on them are removed, except
	 * the ones listed in this set (typically only car).
	 */
	private Set<String> modesToKeepOnCleanUp = new HashSet<>();

	public Set<String> getModesToKeepOnCleanUp() {
		return this.modesToKeepOnCleanUp;
	}

	public void setModesToKeepOnCleanUp(Set<String> modesToKeepOnCleanUp) {
		this.modesToKeepOnCleanUp = modesToKeepOnCleanUp;
	}

	@StringGetter(MODES_TO_KEEP_ON_CLEAN_UP)
	private String getModesToKeepOnCleanUpString() {
		String ret = "";
		if(modesToKeepOnCleanUp != null) {
			for(String mode : modesToKeepOnCleanUp) {
				ret += "," + mode;
			}
		}
		return this.modesToKeepOnCleanUp == null ? null : ret.substring(1);
	}

	@StringSetter(MODES_TO_KEEP_ON_CLEAN_UP)
	private void setModesToKeepOnCleanUp(String modesToKeepOnCleanUp) {
		if(modesToKeepOnCleanUp == null) {
			this.modesToKeepOnCleanUp = null;
			return;
		}
		for(String mode : modesToKeepOnCleanUp.split(",")) {
			this.modesToKeepOnCleanUp.add(mode.trim());
		}
	}

	/**
	 * Defines whether at the end of mapping, all non-car link modes (bus, rail, etc)
	 * should be replaced with pt (true) or not. Default: false.
	 */
	private boolean combinePtModes = false;

	@StringGetter(COMBINE_PT_MODES)
	public boolean getCombinePtModes() {
		return combinePtModes;
	}

	@StringSetter(COMBINE_PT_MODES)
	public void setCombinePtModes(boolean v) {
		this.combinePtModes = v;
	}

	/**
	 * Defines the radius [meter] from a stop facility within nodes are searched.
	 * Mainly a maximum value for performance.
	 */
	private double nodeSearchRadius = 300;

	@StringGetter(NODE_SEARCH_RADIUS)
	public double getNodeSearchRadius() {
		return nodeSearchRadius;
	}

	@StringSetter(NODE_SEARCH_RADIUS)
	public void setNodeSearchRadius(double nodeSearchRadius) {
		this.nodeSearchRadius = nodeSearchRadius;
	}

	/**
	 * Defines the radius [meter] from a stop facility within nodes are searched.
	 * Mainly a maximum value for performance.
	 */
	private double linkDistanceTolerance = 1.0;

	@StringGetter(LINK_DISTANCE_TOLERANCE)
	public double getLinkDistanceTolerance() {
		return linkDistanceTolerance;
	}

	@StringSetter(LINK_DISTANCE_TOLERANCE)
	public void setLinkDistanceTolerance(double linkDistanceTolerance) {
		this.linkDistanceTolerance = linkDistanceTolerance < 1 ? 1 : linkDistanceTolerance;
	}


	/**
	 * Defines which link attribute should be used for pseudo route
	 * calculations. Default is link length (linkLength). If high quality
	 * information on link travel times is available, travelTime
	 * can be used.
	 */
	public enum PseudoRouteWeightType {
		travelTime, linkLength
	}
	private PseudoRouteWeightType pseudoRouteWeightType = PseudoRouteWeightType.linkLength;

	@StringGetter(PSEUDO_ROUTE_WEIGHT_TYPE)
	public PseudoRouteWeightType getPseudoRouteWeightType() {
		return pseudoRouteWeightType;
	}

	@StringSetter(PSEUDO_ROUTE_WEIGHT_TYPE)
	public void setPseudoRouteWeightType(PseudoRouteWeightType type) {
		this.pseudoRouteWeightType = type;
	}


	/**
	 * Number of link candidates considered for all stops, depends on accuracy of
	 * stops and desired performance. Somewhere between 4 and 10 seems reasonable,
	 * depending on the accuracy of the stop facility coordinates. Default: 8
	 */
	private int maxNClosestLinks = 8;

	@StringGetter(MAX_NCLOSEST_LINKS)
	public int getMaxNClosestLinks() {
		return maxNClosestLinks;
	}

	@StringSetter(MAX_NCLOSEST_LINKS)
	public void setMaxNClosestLinks(int maxNClosestLinks) {
		this.maxNClosestLinks = maxNClosestLinks;
	}

	/**
	 * The maximal distance [meter] a link candidate is allowed to have from
	 * the stop facility.
	 */
	private double maxStopFacilityDistance = 80;

	@StringGetter(MAX_STOP_FACILITY_DISTANCE)
	public double getMaxStopFacilityDistance() {
		return maxStopFacilityDistance;
	}

	@StringSetter(MAX_STOP_FACILITY_DISTANCE)
	public void setMaxStopFacilityDistance(double maxStopFacilityDistance) {
		this.maxStopFacilityDistance = maxStopFacilityDistance;
	}

	/**
	 * ID prefix used for artificial links and nodes created if no nodes
	 * are found within nodeSearchRadius
	 */
	private String prefixArtificial = "pt_";

	@StringGetter(PREFIX_ARTIFICIAL)
	public String getPrefixArtificial() {
		return prefixArtificial;
	}

	@StringSetter(PREFIX_ARTIFICIAL)
	public void setPrefixArtificial(String prefixArtificial) {
		this.prefixArtificial = prefixArtificial;
	}

	/**
	 * Suffix used for child stop facilities. A number for each child of a
	 * parent stop facility is appended (i.e. stop0123.fac:2).
	 */
	// TODO remove suffix from config file and set as static to ensure other parts of the package work
	private String suffixChildStopFacilities = ".link:";
	private String suffixChildStopFacilitiesRegex = "[.]link:";

	@StringGetter(SUFFIX_CHILD_STOP_FACILITIES)
	public String getSuffixChildStopFacilities() {
		return suffixChildStopFacilities;
	}

	@StringSetter(SUFFIX_CHILD_STOP_FACILITIES)
	public void setSuffixChildStopFacilities(String suffixChildStopFacilities) {
		this.suffixChildStopFacilities = suffixChildStopFacilities;
		this.suffixChildStopFacilitiesRegex = suffixChildStopFacilities.replace(".", "[.]"); // todo regex escape
	}

	public String getSuffixChildStopFacilitiesRegex() {
		return suffixChildStopFacilitiesRegex;
	}

	/**
	 * If all paths between two stops have a length > beelineDistanceMaxFactor * beelineDistance,
	 * an artificial link is created.
	 */
	private double beelineDistanceMaxFactor = 5.0;
	private double beelineFreespeed = 50 / 3.6; // todo include in config

	@StringGetter(BEELINE_DISTANCE_MAX_FACTOR)
	public double getBeelineDistanceMaxFactor() {
		return beelineDistanceMaxFactor;
	}

	@StringSetter(BEELINE_DISTANCE_MAX_FACTOR)
	public void setBeelineDistanceMaxFactor(double beelineDistanceMaxFactor) {
		this.beelineDistanceMaxFactor = beelineDistanceMaxFactor;
	}

	public double getBeelineFreespeed() {
		return beelineFreespeed;
	}

	/**
	 * The freespeed of artificially created links.
	 */
	private double freespeedArtificialLinks = 50;

	@StringGetter(FREESPEED_ARTIFICIAL)
	public double getFreespeedArtificial() {
		return freespeedArtificialLinks;
	}

	@StringSetter(FREESPEED_ARTIFICIAL)
	public void setFreespeedArtificial(double freespeedArtificialLinks) {
		this.freespeedArtificialLinks = freespeedArtificialLinks;
	}

	/**
	 *
	 */
	public Set<String> getNetworkModes() {
		Set<String> networkModes = new HashSet<>();
		modeRoutingAssignment.values().forEach(networkModes::addAll);
		return networkModes;
	}

	public Set<String> getScheduleModes() {
		Set<String> scheduleModes = new HashSet<>();
		modeRoutingAssignment.keySet().forEach(scheduleModes::add);
		return scheduleModes;
	}

	/**
	 * Params for filepahts
	 */
	@StringGetter(NETWORK_FILE)
	public String getNetworkFile() {
		return this.networkFile == null ? "" : this.networkFile;
	}

	@StringSetter(NETWORK_FILE)
	public String setNetworkFile(String networkFile) {
		final String old = this.networkFile;
		this.networkFile = networkFile;
		return old;
	}

	@StringGetter(SCHEDULE_FILE)
	public String getScheduleFile() {
		return this.scheduleFile == null ? "" : this.scheduleFile;
	}

	@StringSetter(SCHEDULE_FILE)
	public String setScheduleFile(String scheduleFile) {
		final String old = this.scheduleFile;
		this.scheduleFile = scheduleFile;
		return old;
	}

	@StringGetter(OUTPUT_NETWORK_FILE)
	public String getOutputNetworkFile() {
		return this.outputNetworkFile == null ? "" : this.outputNetworkFile;
	}

	@StringSetter(OUTPUT_NETWORK_FILE)
	public String setOutputNetworkFile(String outputNetwork) {
		final String old = this.outputNetworkFile;
		this.outputNetworkFile = outputNetwork;
		return old;
	}

	@StringGetter(OUTPUT_STREET_NETWORK_FILE)
	public String getOutputStreetNetworkFile() {
		return this.outputStreetNetworkFile == null ? null : this.outputStreetNetworkFile;
	}

	@StringSetter(OUTPUT_STREET_NETWORK_FILE)
	public String setOutputStreetNetworkFile(String outputStreetNetwork) {
		final String old = this.outputStreetNetworkFile;
		this.outputStreetNetworkFile = outputStreetNetwork;
		return old;
	}

	@StringGetter(OUTPUT_SCHEDULE_FILE)
	public String getOutputScheduleFile() {
		return this.outputScheduleFile;
	}

	@StringSetter(OUTPUT_SCHEDULE_FILE)
	public String setOutputScheduleFile(String outputSchedule) {
		final String old = this.outputScheduleFile;
		this.outputScheduleFile = outputSchedule;
		return old;
	}

	/**
	 * Number of link candidates considered for all stops, different for scheduleModes.
	 * Depends on accuracy of stops and desired performance. Somewhere between 4 and 10 seems reasonable,
	 * depending on the accuracy of the stop facility coordinates. Default: 8
	 */
	public Map<String, Integer> getMaxNClosestLinksByMode() {
		return null;
	}

}
