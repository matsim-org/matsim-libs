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
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Config Group that is used by {@link playground.polettif.publicTransitMapping.mapping.PTMapper}
 * Defines parameters for mapping public transit to a network.
 *
 * @author polettif
 */
public class PublicTransitMappingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "PublicTransitMapping";

	/**
	 * Suffix used for child stop facilities. The id of the referenced link is appended (i.e. stop0123.link:7852).
	 */
	public static final String SUFFIX_CHILD_STOP_FACILITIES = ".link:";
	public static final String SUFFIX_CHILD_STOP_FACILITIES_REGEX = "[.]link:";

	public static final String ARTIFICIAL_LINK_MODE = "artificial";
	public static final String STOP_FACILITY_LOOP_LINK = "stopFacilityLink";
	public static final Set<String> ARTIFICIAL_LINK_MODE_AS_SET = Collections.singleton(ARTIFICIAL_LINK_MODE);

	private static final String MODE_ROUTING_ASSIGNMENT ="modeRoutingAssignment";
	private static final String MODES_TO_KEEP_ON_CLEAN_UP = "modesToKeepOnCleanUp";
	private static final String NODE_SEARCH_RADIUS = "nodeSearchRadius";
	private static final String TRAVEL_COST_TYPE = "travelCostType";
	private static final String MAX_NCLOSEST_LINKS = "maxNClosestLinks";
	private static final String MAX_LINK_CANDIDATE_DISTANCE = "maxLinkCandidateDistance";
	private static final String PREFIX_ARTIFICIAL = "prefixArtificial";
	private static final String MAX_TRAVEL_COST_FACTOR = "maxTravelCostFactor";
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
	private static final String NUM_OF_THREADS = "numOfThreads";
	private static final String REMOVE_TRANSIT_ROUTES_WITHOUT_LINK_SEQUENCES = "removeTransitRoutesWithoutLinkSequences";
	private static final String MANUAL_LINK_CANDIDATE_CSV_FILE = "manualLinkCandidateCsvFile";
	private static final String SUFFIX_CHILD_STOP_FACILITIES_TAG = "suffixChildStopFacilities";
	private static final String REMOVE_NOT_USED_STOP_FACILITIES = "removeNotUsedStopFacilities";

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
		map.put(TRAVEL_COST_TYPE,
				"Defines which link attribute should be used for routing. Possible values \""+ TravelCostType.linkLength+"\" (default) \n" +
				"\t\tand \""+ TravelCostType.travelTime+"\".");
		map.put(MAX_NCLOSEST_LINKS,
				"(concerns Link Candidates) Number of link candidates considered for all stops, depends on accuracy of stops and desired \n" +
				"\t\tperformance. Somewhere between 4 and 10 seems reasonable, depending on the accuracy of the stop \n" +
				"\t\tfacility coordinates and performance desires. Default: " + maxNClosestLinks);
		map.put(NODE_SEARCH_RADIUS,
				"(concerns Link Candidates) Defines the radius [meter] from a stop facility within nodes are searched. Values up to 2000 do \n" +
				"\t\tdon't have a significant impact on performance.");
		map.put(MAX_LINK_CANDIDATE_DISTANCE,
				"(concerns Link Candidates) The maximal distance [meter] a link candidate is allowed to have from the stop facility.");
		map.put(PREFIX_ARTIFICIAL,
				"ID prefix used for all artificial links and nodes created during mapping.");
		map.put(SCHEDULE_FREESPEED_MODES,
				"After the schedule has been mapped, the free speed of links can be set according to the necessary travel \n" +
				"\t\ttimes given by the transit schedule. The freespeed of a link is set to the minimal value needed by all \n" +
				"\t\ttransit routes passing using it. This is performed for \""+ARTIFICIAL_LINK_MODE + "\" automatically, additional \n" +
				"\t\tmodes (rail is recommended) can be added, separated by commas.");
		map.put(MAX_TRAVEL_COST_FACTOR,
				"If all paths between two stops have a [travelCost] > ["+MAX_TRAVEL_COST_FACTOR+"] * [minTravelCost], \n" +
				"\t\tan artificial link is created. If "+ TRAVEL_COST_TYPE +" is " + TravelCostType.travelTime +
				"\t\tminTravelCost is the travelTime between stops from schedule. If "+ TRAVEL_COST_TYPE +" is \n" +
				"\t\t"+ TravelCostType.linkLength + " minTravel cost is the beeline distance.");
		map.put(NUM_OF_THREADS,
				"Defines the number of numOfThreads that should be used for pseudoRouting. Default: 2.");
		map.put(NETWORK_FILE, "Path to the input network file. Not needed if PTMapper is called within another class.");
		map.put(SCHEDULE_FILE, "Path to the input schedule file. Not needed if PTMapper is called within another class.");
		map.put(OUTPUT_NETWORK_FILE, "Path to the output network file. Not needed if PTMapper is used within another class.");
		map.put(OUTPUT_STREET_NETWORK_FILE, "Path to the output car only network file. The input multimodal map is filtered. \n" +
				"\t\tNot needed if PTMapper is used within another class.");
		map.put(OUTPUT_SCHEDULE_FILE, "Path to the output schedule file. Not needed if PTMapper is used within another class.");
		map.put(REMOVE_NOT_USED_STOP_FACILITIES,
				"If true, stop facilities that are not used by any transit route are removed from the schedule. Default: true");
		return map;
	}


	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch(type) {
			case ManualLinkCandidates.SET_NAME :
				return new ManualLinkCandidates();
			case ModeRoutingAssignment.SET_NAME :
				return new ModeRoutingAssignment();
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
	private Map<String, Set<String>> modeRoutingAssignment = null;
	private Map<String, Boolean> mapArtificial = null;

	public Map<String, Set<String>> getModeRoutingAssignment() {
		if(modeRoutingAssignment == null) initiateParamSet();
		return modeRoutingAssignment;
	}

	public void setModeRoutingAssignment(Map<String, Set<String>> modeRoutingAssignment) {
		this.modeRoutingAssignment = modeRoutingAssignment;
	}

	public Map<String, Boolean> getMapArtificial() {
		if(modeRoutingAssignment == null) initiateParamSet();
		return mapArtificial;
	}

	public boolean mapArtificial(String scheduleTransportMode) {
		return mapArtificial.get(scheduleTransportMode);
	}

	private void initiateParamSet() {
		mapArtificial = new HashMap<>();
		modeRoutingAssignment = new HashMap<>();
		for(ConfigGroup e : this.getParameterSets(PublicTransitMappingConfigGroup.ModeRoutingAssignment.SET_NAME)) {
			ModeRoutingAssignment mra = (ModeRoutingAssignment) e;
			modeRoutingAssignment.put(mra.getScheduleMode(), mra.getNetworkModes());
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
 	 *
 	 */
	private boolean removeNotUsedStopFacilities = true;

	@StringGetter(REMOVE_NOT_USED_STOP_FACILITIES)
	public boolean getRemoveNotUsedStopFacilities() {
		return removeNotUsedStopFacilities;
	}

	@StringSetter(REMOVE_NOT_USED_STOP_FACILITIES)
	public void setRemoveNotUsedStopFacilities(boolean v) {
		this.removeNotUsedStopFacilities = v;
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
	private int numOfThreads = 2;

	@StringGetter(NUM_OF_THREADS)
	public int getNumOfThreads() {
		return numOfThreads;
	}

	@StringSetter(NUM_OF_THREADS)
	public void setNumOfThreads(int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}


	/**
	 * Defines which link attribute should be used for pseudo route
	 * calculations. Default is link length (linkLength). If high quality
	 * information on link travel times is available, travelTime
	 * can be used.
	 */
	public enum TravelCostType {
		travelTime, linkLength
	}
	private TravelCostType travelCostType = TravelCostType.linkLength;

	@StringGetter(TRAVEL_COST_TYPE)
	public TravelCostType getTravelCostType() {
		return travelCostType;
	}

	@StringSetter(TRAVEL_COST_TYPE)
	public void setTravelCostType(TravelCostType type) {
		this.travelCostType = type;
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
	 * If all paths between two stops have a length > maxTravelCostFactor * beelineDistance,
	 * an artificial link is created.
	 */
	private double maxTravelCostFactor = 5.0;

	@StringGetter(MAX_TRAVEL_COST_FACTOR)
	public double getMaxTravelCostFactor() {
		return maxTravelCostFactor;
	}

	@StringSetter(MAX_TRAVEL_COST_FACTOR)
	public void setMaxTravelCostFactor(double maxTravelCostFactor) {
		if(maxTravelCostFactor < 1) {
			throw new RuntimeException("maxTravelCostFactor cannnot be less than 1!");
		}
		this.maxTravelCostFactor = maxTravelCostFactor;
	}


	/**
	 *
	 */
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
		this.manualLinkCandidateCsvFile = file.equals("") ? null :file;
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
	public void setNetworkFile(String networkFile) {
		this.networkFile = networkFile.equals("") ? null : networkFile;
	}

	@StringGetter(SCHEDULE_FILE)
	public String getScheduleFileStr() {
		return this.scheduleFile == null ? "" : this.scheduleFile;
	}
	public String getScheduleFile() {
		return this.scheduleFile;
	}

	@StringSetter(SCHEDULE_FILE)
	public void setScheduleFile(String scheduleFile) {
		this.scheduleFile = scheduleFile.equals("") ? null : scheduleFile;
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
	public void setOutputStreetNetworkFile(String outputStreetNetworkFile) {
		this.outputStreetNetworkFile = outputStreetNetworkFile.equals("") ? null : outputStreetNetworkFile;
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

	public Set<ManualLinkCandidates> getManualLinkCandidates() {
		Set<ManualLinkCandidates> manualCandidates = new HashSet<>();
		for(ConfigGroup e : this.getParameterSets(PublicTransitMappingConfigGroup.ManualLinkCandidates.SET_NAME)) {
			manualCandidates.add((PublicTransitMappingConfigGroup.ManualLinkCandidates) e);
		}
		return manualCandidates;
	}

	/**
	 * Parameterset that define which network transport modes the router
	 * can use for each schedule transport mode.<p/>
	 *
	 * Network transport modes are the ones in {@link Link#getAllowedModes()}, schedule
	 * transport modes are from {@link TransitRoute#getTransportMode()}.
	 */
	public static class ModeRoutingAssignment extends ReflectiveConfigGroup implements MatsimParameters {

		public final static String SET_NAME = "modeRoutingAssignment";

		private static final String SCHEDULE_MODE = "scheduleMode";
		private static final String NETWORK_MODES = "networkModes";

		private String scheduleMode;
		private Set<String> networkModes;

		public ModeRoutingAssignment() {
			super(SET_NAME);
		}

		public ModeRoutingAssignment(String scheduleMode, Set<String> networkModes, boolean mapArtificial) {
			super(SET_NAME);
			this.scheduleMode = scheduleMode;
			this.networkModes = networkModes;
		}

		@StringGetter(SCHEDULE_MODE)
		public String getScheduleMode() {
			return scheduleMode;
		}
		@StringSetter(SCHEDULE_MODE)
		public void setScheduleMode(String scheduleMode) {
			this.scheduleMode = scheduleMode;
		}

		@StringGetter(NETWORK_MODES)
		public Set<String> getNetworkModes() {
			return networkModes;
		}
		@StringSetter(NETWORK_MODES)
		public void setNetworkModesStr(String networkModesStr) {
			this.networkModes = CollectionUtils.stringToSet(networkModesStr);
		}
		public void setNetworkModes(Set<String> networkModes) {
			this.networkModes = networkModes;
		}
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
