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

import com.opencsv.CSVReader;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.core.utils.collections.MapUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 *
 */
public class PublicTransitMappingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "PublicTransitMapping";

	public static final String ARTIFICIAL_LINK_MODE = "artificial";
	public static final String STOP_FACILITY_LOOP_LINK = "stopFacilityLink";
	public static final Set<String> ARTIFICIAL_LINK_MODE_AS_SET = Collections.singleton(ARTIFICIAL_LINK_MODE);

	/**
	 * Suffix used for child stop facilities. The id of the referenced link is appended (i.e. stop0123.link:7852).
	 */
	public static final String SUFFIX_CHILD_STOP_FACILITIES = ".link:";
	public static final String SUFFIX_CHILD_STOP_FACILITIES_REGEX = "[.]link:";

	private static final String MODE_ROUTING_ASSIGNMENT ="modeRoutingAssignment";
	private static final String MODES_TO_KEEP_ON_CLEAN_UP = "modesToKeepOnCleanUp";
	private static final String NODE_SEARCH_RADIUS = "nodeSearchRadius";
	private static final String PSEUDO_ROUTE_WEIGHT_TYPE = "pseudoRouteWeightType";
	private static final String MAX_NCLOSEST_LINKS = "maxNClosestLinks";
	private static final String MAX_LINK_CANDIDATE_DISTANCE = "maxLinkCandidateDistance";
	private static final String PREFIX_ARTIFICIAL = "prefixArtificial";
	private static final String BEELINE_DISTANCE_MAX_FACTOR = "beelineDistanceMaxFactor";
	private static final String NETWORK_FILE = "networkFile";
	private static final String SCHEDULE_FILE = "scheduleFile";
	private static final String OUTPUT_NETWORK_FILE = "outputNetworkFile";
	private static final String OUTPUT_SCHEDULE_FILE = "outputScheduleFile";
	private static final String OUTPUT_STREET_NETWORK_FILE = "outputStreetNetworkFile";
	private static final String LINK_DISTANCE_TOLERANCE = "linkDistanceTolerance";
	private static final String FREESPEED_ARTIFICIAL = "freespeedArtificialLinks";
	private static final String SCHEDULE_FREESPEED_MODES = "scheduleFreespeedModes";
	private static final String COMBINE_PT_MODES = "combinePtModes";
	private static final String ADD_PT_MODE = "addPtMode";
	private static final String MULTI_THREAD = "threads";
	private static final String REMOVE_TRANSIT_ROUTES_WITHOUT_LINK_SEQUENCES = "removeTransitRoutesWithoutLinkSequences";
	private static final String MANUAL_LINK_CANDIDATE_CSV_FILE = "manualLinkCandidateCsvFile";
	private static final String SUFFIX_CHILD_STOP_FACILITIES_TAG = "suffixChildStopFacilities";

	public PublicTransitMappingConfigGroup() {
		super(GROUP_NAME);

		modesToKeepOnCleanUp.add("car");
	}

	private String networkFile = null;
	private String scheduleFile = null;
	private String outputNetworkFile = null;
	private String outputStreetNetworkFile = null;
	private String outputScheduleFile = null;

	public static PublicTransitMappingConfigGroup createDefaultConfig() {
		return new PublicTransitMappingConfigGroup();
	}

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
		map.put(ADD_PT_MODE,
				"The mode \"pt\" is added to all links used by public transit after mapping if true. \n" +
						"\t\tIs not executed if "+COMBINE_PT_MODES+" is true. Default: true");
		map.put(REMOVE_TRANSIT_ROUTES_WITHOUT_LINK_SEQUENCES,
				"If true, transit routes without link sequences after mapping are removed from the schedule. Default: true");
		map.put(LINK_DISTANCE_TOLERANCE,
				"(concerns Link Candidates) After " +MAX_NCLOSEST_LINKS +" link candidates have been found, additional link \n" +
						"\t\tcandidates within ["+LINK_DISTANCE_TOLERANCE+"] * [distance to the Nth link] are added to the set.\n" +
						"\t\tMust be >= 1.");
		map.put(PSEUDO_ROUTE_WEIGHT_TYPE,
				"Defines which link attribute should be used for pseudo route calculations. Default is minimization \n" +
						"\t\tof travel distance. If high quality information on link travel times is available, travelTime can be \n" +
						"\t\tused. (Possible values \""+PseudoRouteWeightType.linkLength+"\" and \""+PseudoRouteWeightType.travelTime+"\")");
		map.put(MAX_NCLOSEST_LINKS,
				"(concerns Link Candidates) Number of link candidates considered for all stops, depends on accuracy of stops and desired \n" +
						"\t\tperformance. Somewhere between 4 and 10 seems reasonable, depending on the accuracy of the stop \n" +
						"\t\tfacility coordinates and performance desires. Default: " + maxNClosestLinks);
		map.put(NODE_SEARCH_RADIUS,
				"(concerns Link Candidates) Defines the radius [meter] from a stop facility within nodes are searched. Values up to 2000 do" +
				"\t\tdon't have a significant impact on performance.");
		map.put(MAX_LINK_CANDIDATE_DISTANCE,
				"(concerns Link Candidates) The maximal distance [meter] a link candidate is allowed to have from the stop facility.");
		map.put(PREFIX_ARTIFICIAL,
				"ID prefix used for all artificial links and nodes created during mapping.");
//		map.put(FREESPEED_ARTIFICIAL,
//				"The freespeed [m/s] of artificially created links. This value is the same for all schedule modes. Is ignored if" +
//				"\t\t"+SCHEDULE_FREESPEED_MODES + "is set.");
		map.put(SCHEDULE_FREESPEED_MODES,
				"After the schedule has been mapped, the free speed of links can be set according to the necessary travel" +
				"\t\ttimes given by the transit schedule. The freespeed of a link is set to the minimal value needed by all" +
				"\t\ttransit routes passing using it. This is performed for \""+ARTIFICIAL_LINK_MODE + "\" automatically, additional" +
				"\t\tmodes (rail is recommended) can be added, separated by commas.");
		map.put(BEELINE_DISTANCE_MAX_FACTOR,
				"If all paths between two stops have a [length] > [beelineDistanceMaxFactor] * [beelineDistance], \n" +
				"\t\tan artificial link is created. If "+PSEUDO_ROUTE_WEIGHT_TYPE+" is " + PseudoRouteWeightType.travelTime +
				"\t\tthe check is [travelTime] > [beelineDistanceMaxFactor] * [travelTime between stops from schedule]");
		map.put(MULTI_THREAD,
				"Defines the number of threads that should be used for pseudoRouting. Default: 2.");
		map.put(NETWORK_FILE, "Path to the input network file. Not needed if PTMapper is called within another class.");
		map.put(SCHEDULE_FILE, "Path to the input schedule file. Not needed if PTMapper is called within another class.");
		map.put(OUTPUT_NETWORK_FILE, "Path to the output network file. Not needed if PTMapper is used within another class.");
		map.put(OUTPUT_STREET_NETWORK_FILE, "Path to the output car only network file. The input multimodal map is filtered. \n" +
				"\t\tNot needed if PTMapper is used within another class.");
		map.put(OUTPUT_SCHEDULE_FILE, "Path to the output schedule file. Not needed if PTMapper is used within another class.");
//		map.put(SUFFIX_CHILD_STOP_FACILITIES_TAG,
//				"Suffix used for child stop facilities. The id of the referenced link is appended\n" +
//						"\t\t(i.e. stop0123.link:LINKID20123).");
		return map;
	}


	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch(type) {
			case ManualLinkCandidates.SET_NAME :
				return new ManualLinkCandidates();
			default:
				throw new IllegalArgumentException("Unknown parameterset name!");
		}
	}

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
	private String getModeRoutingAssignmentStr() {
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
	public void setModeRoutingAssignmentStr(String modeRoutingAssignmentString) {
		if(modeRoutingAssignmentString == null) {
			this.modeRoutingAssignment = null;
			return;
		}

		if(modeRoutingAssignmentString.equals("")) {
			throw new IllegalArgumentException("No modeRoutingAssignment defined in config!");
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

	public void addModeRoutingAssignment(String key, String value) {
		Set<String> set = MapUtils.getSet(key, this.modeRoutingAssignment);
		set.add(value);
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
	 *
	 */
	private boolean addPtMode = true;

	@StringGetter(ADD_PT_MODE)
	public boolean getAddPtMode() {
		return addPtMode;
	}

	@StringSetter(ADD_PT_MODE)
	public void setAddPtMode(boolean addPtMode) {
		this.addPtMode = addPtMode;
	}

	/**
	 *
	 */
	private boolean removeTransitRoutesWithoutLinkSequences = true;

	@StringGetter(REMOVE_TRANSIT_ROUTES_WITHOUT_LINK_SEQUENCES)
	public boolean getRemoveTransitRoutesWithoutLinkSequences() {
		return removeTransitRoutesWithoutLinkSequences;
	}

	@StringSetter(REMOVE_TRANSIT_ROUTES_WITHOUT_LINK_SEQUENCES)
	public void setRemoveTransitRoutesWithoutLinkSequences(boolean v) {
		this.removeTransitRoutesWithoutLinkSequences = v;
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
	 * Defines whehter multiple threads should be used (one for each
	 * schedule transport mode).
	 */
	private int threads = 2;

	@StringGetter(MULTI_THREAD)
	public int getThreads() {
		return threads;
	}

	@StringSetter(MULTI_THREAD)
	public void setThreads(int threads) {
		this.threads = threads;
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
	private double maxLinkCandidateDistance = 80;

	@StringGetter(MAX_LINK_CANDIDATE_DISTANCE)
	public double getMaxLinkCandidateDistance() {
		return maxLinkCandidateDistance;
	}

	@StringSetter(MAX_LINK_CANDIDATE_DISTANCE)
	public void setMaxLinkCandidateDistance(double maxLinkCandidateDistance) {
		this.maxLinkCandidateDistance = maxLinkCandidateDistance;
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
//	@StringGetter(SUFFIX_CHILD_STOP_FACILITIES_TAG)
	public String getSuffixChildStopFacilities() {
		return SUFFIX_CHILD_STOP_FACILITIES;
	}

//	@StringSetter(SUFFIX_CHILD_STOP_FACILITIES_TAG)
//	public void setSuffixChildStopFacilities(String suffixChildStopFacilities) {
//		this.suffixChildStopFacilities = suffixChildStopFacilities;
//		this.suffixRegexEscaped = Pattern.quote(suffixChildStopFacilities);
//	}

	public String getSuffixRegexEscaped() {
		return SUFFIX_CHILD_STOP_FACILITIES_REGEX;
	}

	/**
	 * If all paths between two stops have a length > beelineDistanceMaxFactor * beelineDistance,
	 * an artificial link is created.
	 */
	private double beelineDistanceMaxFactor = 5.0;

	@StringGetter(BEELINE_DISTANCE_MAX_FACTOR)
	public double getBeelineDistanceMaxFactor() {
		return beelineDistanceMaxFactor;
	}

	@StringSetter(BEELINE_DISTANCE_MAX_FACTOR)
	public void setBeelineDistanceMaxFactor(double beelineDistanceMaxFactor) {
		if(beelineDistanceMaxFactor < 1) {
			throw new RuntimeException("beelineDistanceMaxFactor cannnot be less than 1!");
		}
		this.beelineDistanceMaxFactor = beelineDistanceMaxFactor;
	}

	/**
	 * The freespeed of artificially created links.
	 */
//	private double freespeedArtificialLinks = 40;

//	@StringGetter(FREESPEED_ARTIFICIAL)
//	public double getFreespeedArtificial() {
//		return freespeedArtificialLinks;
//	}

//	@StringSetter(FREESPEED_ARTIFICIAL)
//	public void setFreespeedArtificial(double freespeedArtificialLinks) {
//		this.freespeedArtificialLinks = freespeedArtificialLinks;
//	}

	public Set<String> scheduleFreespeedModes = new HashSet<>(ARTIFICIAL_LINK_MODE_AS_SET);

	@StringGetter(SCHEDULE_FREESPEED_MODES)
	public String getScheduleFreespeedModesStr() {
		return "";
	}
	public Set<String> getScheduleFreespeedModes() {
		return scheduleFreespeedModes;
	}

	@StringSetter(SCHEDULE_FREESPEED_MODES)
	public void setScheduleFreespeedModesStr(String modes) {
		this.scheduleFreespeedModes.addAll(CollectionUtils.stringToSet(modes));
	}
	public void setScheduleFreespeedModes(Set<String> modes) {
		this.scheduleFreespeedModes.addAll(modes);
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
	 * Manual link candidate csv
	 */
	private String manualLinkCandidateCsvFile = null;

	@StringGetter(MANUAL_LINK_CANDIDATE_CSV_FILE)
	public String getManualLinkCandidateCsvFileStr() {
		return this.manualLinkCandidateCsvFile== null ? "" : this.manualLinkCandidateCsvFile;
	}
	public String getManualLinkCandidateCsvFile() {
		return this.manualLinkCandidateCsvFile;
	}

	@StringSetter(MANUAL_LINK_CANDIDATE_CSV_FILE)
	public void setManualLinkCandidateCsvFile(String file) {
		this.manualLinkCandidateCsvFile = file;
	}

	public void loadManualLinkCandidatesCsv() {
		try {
			CSVReader reader = new CSVReader(new FileReader(manualLinkCandidateCsvFile), ';');

			String[] line = new String[0];
			while(line != null) {
				this.addParameterSet(new ManualLinkCandidates(line[0], line[1], line[2]));
				line = reader.readNext();
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Params for filepahts
	 */
	@StringGetter(NETWORK_FILE)
	public String getNetworkFileStr() {
		return this.networkFile == null ? "" : this.networkFile;
	}
	public String getNetworkFile() {
		return this.networkFile;
	}

	@StringSetter(NETWORK_FILE)
	public String setNetworkFile(String networkFile) {
		final String old = this.networkFile;
		this.networkFile = networkFile;
		return old;
	}

	@StringGetter(SCHEDULE_FILE)
	public String getScheduleFileStr() {
		return this.scheduleFile == null ? "" : this.scheduleFile;
	}
	public String getScheduleFile() {
		return this.scheduleFile;
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
	public String getOutputStreetNetworkFileStr() {
		return this.outputStreetNetworkFile == null ? "" : this.outputStreetNetworkFile;
	}
	public String getOutputStreetNetworkFile() {
		return this.outputStreetNetworkFile;
	}

	@StringSetter(OUTPUT_STREET_NETWORK_FILE)
	public String setOutputStreetNetworkFile(String outputStreetNetwork) {
		final String old = this.outputStreetNetworkFile;
		this.outputStreetNetworkFile = outputStreetNetwork;
		return old;
	}

	public String getOutputScheduleFile() {
		return this.outputScheduleFile;
	}

	@StringGetter(OUTPUT_SCHEDULE_FILE)
	public String getOutputScheduleFileStr() {
		return this.outputScheduleFile == null ? "" : this.outputScheduleFile;
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

	/**
	 * Link candidates for complicated stops can be defined manually
	 * with this parameterset
	 */
	public static class ManualLinkCandidates extends ReflectiveConfigGroup implements MatsimParameters {

		public final static String SET_NAME = "manualLinkCandidates";

		private static final String LINK_IDS = "links";
		private static final String MODES = "modes";
		private static final String STOP_FACILITY = "stopFacility";
		private static final String REPLACE = "replace";

		private Id<TransitStopFacility> stopFacilityId;
		private Set<String> modes = new HashSet<>();
		private Set<Id<Link>> linkIds = new HashSet<>();
		private boolean replace = true;

		public ManualLinkCandidates() {
			super(SET_NAME);
		}

		public ManualLinkCandidates(String stopFacilityId, String modes, String linkIds) {
			super(SET_NAME);
			setStopFacilityIdStr(stopFacilityId);
			setModesStr(modes);
			setLinkIdsStr(linkIds);
			this.replace = true;
		}

		/**
		 * stop facility id
		 */
		@StringGetter(STOP_FACILITY)
		public String getStopFacilityIdStr() {
			return stopFacilityId.toString();
		}
		public Id<TransitStopFacility> getStopFacilityId() {
			return stopFacilityId;
		}
		@StringSetter(STOP_FACILITY)
		public void setStopFacilityIdStr(String stopFacilityIdStr) {
			this.stopFacilityId = Id.create(stopFacilityIdStr, TransitStopFacility.class);
		}
		public void setStopFacilityIdStr(Id<TransitStopFacility> stopFacilityId) {
			this.stopFacilityId = stopFacilityId;
		}

		/**
		 * modes
		 */
		@StringGetter(MODES)
		public String getModesStr() {
			return CollectionUtils.setToString(this.modes);
		}
		public Set<String> getModes() {
			return modes;
		}

		@StringSetter(MODES)
		public void setModesStr(String modes) {
			this.modes = CollectionUtils.stringToSet(modes);
		}
		public void setModes(Set<String> modes) {
			this.modes = modes;
		}

		/**
		 * link ids
		*/
		@StringGetter(LINK_IDS)
		public String getLinkIdsStr() {
			return CollectionUtils.idSetToString(linkIds);
		}
		public Set<Id<Link>> getLinkIds() {
			return linkIds;
		}

		@StringSetter(LINK_IDS)
		public void setLinkIdsStr(String linkIds) {
			for(String linkIdStr : CollectionUtils.stringToSet(linkIds)) {
				this.linkIds.add(Id.createLinkId(linkIdStr));
			}
		}
		public void setLinkIds(Set<Id<Link>> linkIds) {
			this.linkIds = linkIds;
		}

		@StringGetter(REPLACE)
		public boolean replaceCandidates() {
			return replace;
		}
		@StringSetter(REPLACE)
		public void setReplaceCandidates(boolean v) {
			this.replace = v;
		}
	}
}
