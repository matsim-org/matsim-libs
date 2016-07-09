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
import playground.polettif.publicTransitMapping.mapping.RunPublicTransitMapper;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Config Group usedd by {@link RunPublicTransitMapper}. Defines parameters for
 * mapping public transit to a network.
 *
 * @author polettif
 */
public class PublicTransitMappingConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "PublicTransitMapping";

	// param names
	private static final String MODES_TO_KEEP_ON_CLEAN_UP = "modesToKeepOnCleanUp";
	private static final String NODE_SEARCH_RADIUS = "nodeSearchRadius";
	private static final String TRAVEL_COST_TYPE = "travelCostType";
	private static final String PREFIX_ARTIFICIAL = "prefixArtificial";
	private static final String MAX_TRAVEL_COST_FACTOR = "maxTravelCostFactor";
	private static final String NETWORK_FILE = "networkFile";
	private static final String SCHEDULE_FILE = "scheduleFile";
	private static final String OUTPUT_NETWORK_FILE = "outputNetworkFile";
	private static final String OUTPUT_SCHEDULE_FILE = "outputScheduleFile";
	private static final String OUTPUT_STREET_NETWORK_FILE = "outputStreetNetworkFile";
	private static final String SCHEDULE_FREESPEED_MODES = "scheduleFreespeedModes";
	private static final String COMBINE_PT_MODES = "combinePtModes";
	private static final String ADD_PT_MODE = "addPtMode";
	private static final String NUM_OF_THREADS = "numOfThreads";
	private static final String MANUAL_LINK_CANDIDATE_CSV_FILE = "manualLinkCandidateCsvFile";
	private static final String REMOVE_NOT_USED_STOP_FACILITIES = "removeNotUsedStopFacilities";

	// default values
	private Map<String, Set<String>> modeRoutingAssignment = null;
	private Map<String, LinkCandidateCreatorParams> linkCandidateParams = null;
	private Set<String> scheduleFreespeedModes = new HashSet<>(PublicTransitMappingStrings.ARTIFICIAL_LINK_MODE_AS_SET);
	private double maxTravelCostFactor = 5.0;	private Set<String> modesToKeepOnCleanUp = new HashSet<>();
	private String manualLinkCandidateCsvFile = null;
	private String prefixArtificial = "pt_";
	private int numOfThreads = 2;
	private double nodeSearchRadius = 500;
	private boolean removeNotUsedStopFacilities = true;
	private boolean combinePtModes = false;
	private boolean addPtMode = true;
	private String networkFile = null;
	private String scheduleFile = null;
	private String outputNetworkFile = null;
	private String outputStreetNetworkFile = null;
	private String outputScheduleFile = null;

	public enum TravelCostType {
		travelTime, linkLength
	}
	private TravelCostType travelCostType = TravelCostType.linkLength;


	public PublicTransitMappingConfigGroup() {	super(GROUP_NAME); }


	/**
	 * @return a new default public transit mapping config
	 */
	public static PublicTransitMappingConfigGroup createDefaultConfig() {
		PublicTransitMappingConfigGroup config = new PublicTransitMappingConfigGroup();
		config.getModesToKeepOnCleanUp().add("car");
		LinkCandidateCreatorParams lccParamsBus = new LinkCandidateCreatorParams("bus");
		lccParamsBus.setNetworkModesStr("car,bus");
		LinkCandidateCreatorParams lccParamsRail = new LinkCandidateCreatorParams("rail");
		lccParamsRail.setNetworkModesStr("rail,light_rail");
		lccParamsRail.setMaxNClosestLinks(20);
		lccParamsRail.setMaxLinkCandidateDistance(150);
		LinkCandidateCreatorParams lccParamsTram = new LinkCandidateCreatorParams("tram");
		lccParamsTram.setUseArtificialLoopLink(true);
		config.addParameterSet(lccParamsBus);
		config.addParameterSet(lccParamsRail);
		config.addParameterSet(lccParamsTram);

		ModeRoutingAssignment mraBus = new ModeRoutingAssignment("bus");
		mraBus.setNetworkModesStr("car,bus");
		ModeRoutingAssignment mraRail = new ModeRoutingAssignment("rail");
		mraRail.setNetworkModesStr("rail,light_rail");
		config.addParameterSet(mraBus);
		config.addParameterSet(mraRail);

		config.addParameterSet(new ManualLinkCandidates());

		return config;
	}

	@Override
	public final Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(MODES_TO_KEEP_ON_CLEAN_UP,
				"All links that do not have a transit route on them are removed, except the ones \n" +
						"\t\tlisted in this set (typically only car). Separated by comma.");
		map.put(COMBINE_PT_MODES,
				"Defines whether at the end of mapping, all non-car link modes (bus, rail, etc) \n" +
						"\t\tshould be replaced with pt (true) or not. Default: "+combinePtModes);
		map.put(ADD_PT_MODE,
				"The mode \"pt\" is added to all links used by public transit after mapping if true. \n" +
						"\t\tIs not executed if "+COMBINE_PT_MODES+" is true. Default: "+addPtMode);
		map.put(TRAVEL_COST_TYPE,
				"Defines which link attribute should be used for routing. Possible values \""+ TravelCostType.linkLength+"\" (default) \n" +
						"\t\tand \""+ TravelCostType.travelTime+"\".");
		map.put(NODE_SEARCH_RADIUS,
				"Defines the radius [meter] from a stop facility within nodes are searched. Values up to 2000 don't \n" +
						"\t\thave any significant impact on performance.");
		map.put(PREFIX_ARTIFICIAL,
				"ID prefix used for all artificial links and nodes created during mapping.");
		map.put(SCHEDULE_FREESPEED_MODES,
				"After the schedule has been mapped, the free speed of links can be set according to the necessary travel \n" +
						"\t\ttimes given by the transit schedule. The freespeed of a link is set to the minimal value needed by all \n" +
						"\t\ttransit routes passing using it. This is performed for \""+ PublicTransitMappingStrings.ARTIFICIAL_LINK_MODE + "\" automatically, additional \n" +
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
		map.put(MANUAL_LINK_CANDIDATE_CSV_FILE,
				"Manual link candidates can be defined in a csv file. Each line contains stopFacilityId, modes and linkIds. Separator is \";\"\n" +
				"\t\tExample line: 879843;bus,tram;565,566,5489,5488,321,45");
		return map;
	}


	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch(type) {
			case LinkCandidateCreatorParams.SET_NAME :
				return new LinkCandidateCreatorParams();
			case ModeRoutingAssignment.SET_NAME :
				return new ModeRoutingAssignment();
			case ManualLinkCandidates.SET_NAME :
				return new ManualLinkCandidates();
			default:
				throw new IllegalArgumentException("Unknown parameterset name!");
		}
	}

	public void loadParameterSets() {
		modeRoutingAssignment = new HashMap<>();
		for(ConfigGroup e : this.getParameterSets(PublicTransitMappingConfigGroup.ModeRoutingAssignment.SET_NAME)) {
			ModeRoutingAssignment mra = (ModeRoutingAssignment) e;
			modeRoutingAssignment.put(mra.getScheduleMode(), mra.getNetworkModes());
		}

		linkCandidateParams = new HashMap<>();
		for(ConfigGroup e : this.getParameterSets(LinkCandidateCreatorParams.SET_NAME)) {
			LinkCandidateCreatorParams lcp = (LinkCandidateCreatorParams) e;
			linkCandidateParams.put(lcp.getScheduleMode(), lcp);
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

	public Map<String, Set<String>> getModeRoutingAssignment() {
		return modeRoutingAssignment;
	}

	public void setModeRoutingAssignment(Map<String, Set<String>> modeRoutingAssignment) {
		this.modeRoutingAssignment = modeRoutingAssignment;
	}


	public Map<String, LinkCandidateCreatorParams> getLinkCandidateCreatorParams() {
		return linkCandidateParams;
	}

	@Deprecated
	public LinkCandidateCreatorParams getLinkCandidateCreatorParams(String scheduleMode, boolean createIfNotAvailable) {
		if(!linkCandidateParams.containsKey(scheduleMode) && !createIfNotAvailable) {
			throw new IllegalArgumentException("No LinkCandidateCreatorParams defined for schedule mode " + scheduleMode);
		} else {
			linkCandidateParams.put(scheduleMode, new LinkCandidateCreatorParams(scheduleMode));
		}
		return linkCandidateParams.get(scheduleMode);
	}

	/**
	 * All links that do not have a transit route on them are removed, except
	 * the ones listed in this set (typically only car).
	 */

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

	@StringGetter(COMBINE_PT_MODES)
	public boolean getCombinePtModes() { return combinePtModes; }

	@StringSetter(COMBINE_PT_MODES)
	public void setCombinePtModes(boolean v) { this.combinePtModes = v; }


	/**
	 *
	 */

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

	@StringGetter(NODE_SEARCH_RADIUS)
	public double getNodeSearchRadius() {
		return nodeSearchRadius;
	}

	@StringSetter(NODE_SEARCH_RADIUS)
	public void setNodeSearchRadius(double nodeSearchRadius) {
		this.nodeSearchRadius = nodeSearchRadius;
	}


	/**
	 * Threads
	 */
	@StringGetter(NUM_OF_THREADS)
	public int getNumOfThreads() {
		return numOfThreads;
	}

	@StringSetter(NUM_OF_THREADS)
	public void setNumOfThreads(int numOfThreads) {
		this.numOfThreads = numOfThreads;
	}


	@StringGetter(TRAVEL_COST_TYPE)
	public TravelCostType getTravelCostType() {
		return travelCostType;
	}

	@StringSetter(TRAVEL_COST_TYPE)
	public void setTravelCostType(TravelCostType type) {
		this.travelCostType = type;
	}




	/**
	 * ID prefix used for artificial links and nodes created if no nodes
	 * are found within nodeSearchRadius
	 */
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
	 * Manual link candidate csv
	 */
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


	/**
	 * loads manually defined link candidates from the csv file
	 */
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

	public Set<ManualLinkCandidates> getManualLinkCandidates() {
		Set<ManualLinkCandidates> manualCandidates = new HashSet<>();
		for(ConfigGroup e : this.getParameterSets(PublicTransitMappingConfigGroup.ManualLinkCandidates.SET_NAME)) {
			manualCandidates.add((PublicTransitMappingConfigGroup.ManualLinkCandidates) e);
		}
		return manualCandidates;
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



	public static class LinkCandidateCreatorParams extends ReflectiveConfigGroup implements MatsimParameters {

		public final static String SET_NAME = "linkCandidateCreator";

		private static final String SCHEDULE_MODE = "scheduleMode";
		private static final String NETWORK_MODES = "networkModes";
		private static final String MAX_NCLOSEST_LINKS = "maxNClosestLinks";
		private static final String MAX_LINK_CANDIDATE_DISTANCE = "maxLinkCandidateDistance";
		private static final String LINK_DISTANCE_TOLERANCE = "linkDistanceTolerance";
		private static final String USE_ARTIFICIAL_LOOP_LINK = "useArtificialLoopLink";

		private String scheduleMode;
		private Set<String> networkModes = new HashSet<>();

		private int maxNClosestLinks = 8;
		private double maxLinkCandidateDistance = 80;
		private double linkDistanceTolerance = 1.0;
		private boolean useLoopLink = false;

		@Override
		public final Map<String, String> getComments() {
			Map<String, String> map = super.getComments();
			map.put(SCHEDULE_MODE,
					"For which schedule mode these settings apply.");
			map.put(NETWORK_MODES,
					"Only links with at least one of these modes are considered as link candidate for this schedule mode.\n" +
					"\t\t\tSeparate more than one stop with comma.");
			map.put(LINK_DISTANCE_TOLERANCE,
					"After " +MAX_NCLOSEST_LINKS +" link candidates have been found, additional link \n" +
					"\t\t\tcandidates within ["+LINK_DISTANCE_TOLERANCE+"] * [distance to the Nth link] are added to the set.\n" +
					"\t\t\tMust be >= 1.");
			map.put(MAX_NCLOSEST_LINKS,
					"Number of link candidates considered for all stops, depends on accuracy of stops and desired \n" +
					"\t\t\tperformance. Somewhere between 4 and 10 seems reasonable for bus stops, depending on the accuracy of the stop \n" +
					"\t\t\tfacility coordinates and performance desires. Default: " + maxNClosestLinks);
			map.put(MAX_LINK_CANDIDATE_DISTANCE,
					"The maximal distance [meter] a link candidate is allowed to have from the stop facility. No link candidate\n" +
					"\t\t\tbeyond this distance are added.");
			map.put(USE_ARTIFICIAL_LOOP_LINK,
					"Define if a loop link for all stop facilities for the schedule mode should be created. All other parameters \n"+
					"\t\t\tare ignored if true. The node for the loop link is set on the coordinate of the stop facility.");
			return map;
		}

		public LinkCandidateCreatorParams() {
			super(SET_NAME);
		}

		public LinkCandidateCreatorParams(String scheduleMode) {
			super(SET_NAME);
			this.scheduleMode = scheduleMode;
		}

		@StringGetter(SCHEDULE_MODE)
		public String getScheduleMode() {
			return scheduleMode;
		}

		@StringSetter(SCHEDULE_MODE)
		public void setScheduleMode(String scheduleMode) {
			this.scheduleMode = scheduleMode;
		}


		/**
		 * modes
		 */
		@StringGetter(NETWORK_MODES)
		public String getModesStr() {
			return CollectionUtils.setToString(this.networkModes);
		}
		public Set<String> getNetworkModes() {
			return this.networkModes;
		}

		@StringSetter(NETWORK_MODES)
		public void setNetworkModesStr(String networkModes) {
			this.networkModes = CollectionUtils.stringToSet(networkModes);
		}
		public void setNetworkModes(Set<String> networkModes) {
			this.networkModes = networkModes;
		}

		/**
		 * max n closest links
		 */
		@StringGetter(MAX_NCLOSEST_LINKS)
		public int getMaxNClosestLinks() {
			return maxNClosestLinks;
		}

		@StringSetter(MAX_NCLOSEST_LINKS)
		public void setMaxNClosestLinks(int maxNClosestLinks) {
			this.maxNClosestLinks = maxNClosestLinks;
		}

		/**
		 * max distance
		 */
		@StringGetter(MAX_LINK_CANDIDATE_DISTANCE)
		public double getMaxLinkCandidateDistance() {
			return maxLinkCandidateDistance;
		}

		@StringSetter(MAX_LINK_CANDIDATE_DISTANCE)
		public void setMaxLinkCandidateDistance(double maxLinkCandidateDistance) {
			this.maxLinkCandidateDistance = maxLinkCandidateDistance;
		}

		/**
		 * Defines the radius [meter] from a stop facility within nodes are searched.
		 * Mainly a maximum value for performance.
		 */
		@StringGetter(LINK_DISTANCE_TOLERANCE)
		public double getLinkDistanceTolerance() {
			return linkDistanceTolerance;
		}

		@StringSetter(LINK_DISTANCE_TOLERANCE)
		public void setLinkDistanceTolerance(double linkDistanceTolerance) {
			this.linkDistanceTolerance = linkDistanceTolerance < 1 ? 1 : linkDistanceTolerance;
		}

		/**
		 * Define if a loop link for all stop facilities should be created. All other
		 * parameters are ignored if true.
		 */
		@StringGetter(USE_ARTIFICIAL_LOOP_LINK)
		public boolean useArtificialLoopLink() {
			return useLoopLink;
		}

		@StringSetter(USE_ARTIFICIAL_LOOP_LINK)
		public void setUseArtificialLoopLink(boolean v) {
			this.useLoopLink = v;
		}
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

		@Override
		public Map<String, String> getComments() {
			Map<String, String> map = super.getComments();
			map.put(NETWORK_MODES,
					"Transit Routes with the given scheduleMode can only use links with at least one of the network modes\n" +
					"\t\t\tdefined here. Separate multiple modes by comma. If no network modes are defined, the transit route will\n" +
					"\t\t\tuse artificial links.");
			return map;
		}

		public ModeRoutingAssignment() {
			super(SET_NAME);
		}

		public ModeRoutingAssignment(String scheduleMode) {
			super(SET_NAME);
			this.scheduleMode = scheduleMode;
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
		public String getNetworkModesStr() {
			return CollectionUtils.setToString(networkModes);
		}
		public Set<String> getNetworkModes() {
			return this.networkModes;
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
		private static final String SCHEDULE_MODES = "scheduleModes";
		private static final String STOP_FACILITY = "stopFacility";
		private static final String REPLACE = "replace";

		private Id<TransitStopFacility> stopFacilityId = null;
		private Set<String> scheduleModes = new HashSet<>();
		private Set<Id<Link>> linkIds = new HashSet<>();
		private boolean replace = true;

		public ManualLinkCandidates() {
			super(SET_NAME);
		}

		public ManualLinkCandidates(String stopFacilityId, String modes, String linkIds) {
			super(SET_NAME);
			setStopFacilityIdStr(stopFacilityId);
			setScheduleModesStr(modes);
			setLinkIdsStr(linkIds);
			this.replace = true;
		}

		@Override
		public Map<String, String> getComments() {
			Map<String, String> map = super.getComments();
			map.put(SCHEDULE_MODES,
					"The schedule transport modes for which these link apply. All possible links are considered if empty.");
			map.put(LINK_IDS,
					"The links, comma separated");
			map.put(REPLACE,
					"If true, the link candidates found by the the link candidate creator are replaced with the links\n" +
					"\t\t\tdefined here. If false, the manual links are added to the set.");
			return map;
		}


		/**
		 * stop facility id
		 */
		@StringGetter(STOP_FACILITY)
		public String getStopFacilityIdStr() {
			return stopFacilityId != null ? stopFacilityId.toString() : "";
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
		@StringGetter(SCHEDULE_MODES)
		public String getScheduleModesStr() {
			return CollectionUtils.setToString(this.scheduleModes);
		}
		public Set<String> getScheduleModes() {
			return scheduleModes;
		}

		@StringSetter(SCHEDULE_MODES)
		public void setScheduleModesStr(String modes) {
			this.scheduleModes = CollectionUtils.stringToSet(modes);
		}
		public void setScheduleModes(Set<String> modes) {
			this.scheduleModes = modes;
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
