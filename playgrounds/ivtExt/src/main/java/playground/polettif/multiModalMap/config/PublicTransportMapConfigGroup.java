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

package playground.polettif.multiModalMap.config;

import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 *
 */
public class PublicTransportMapConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "PublicTransportMap";

	public static final String ARTIFICIAL_LINK_MODE = "artificial";

	public PublicTransportMapConfigGroup() {
		super( GROUP_NAME );
	}

	private String networkFile = null;
	private String scheduleFile = null;
	private String outputNetworkFile = null;
	private String outputScheduleFile = null;

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

	@StringGetter( "modeRoutingAssignment" )
	private String getModeRoutingAssignmentString() {
		String ret = "";
		for(Map.Entry<String, Set<String>> entry : modeRoutingAssignment.entrySet()) {
			ret += "|"+entry.getKey().toUpperCase()+":";
			String value = "";
			for(String mode : entry.getValue()) {
				value = ","+mode;
			}
			ret += value.substring(1);
		}
		return this.modesToKeepOnCleanUp == null ? null : ret.substring(1);
	}

	@StringSetter( "modeRoutingAssignment" )
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
			this.modeRoutingAssignment.put(tuple[0].toUpperCase(), set);
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

	@StringGetter( "modesToKeepOnCleanUp" )
	private String getModesToKeepOnCleanUpString() {
		String ret = "";
		if(modesToKeepOnCleanUp != null) {
			for(String mode : modesToKeepOnCleanUp) {
				ret += ","+mode;
			}
		}
		return this.modesToKeepOnCleanUp == null ? null : ret.substring(1);
	}

	@StringSetter( "modesToKeepOnCleanUp" )
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
	 * Defines the radius [meter] from a stop facility within nodes are searched.
	 * Mainly a maximum value for performance.
	 */
	private double nodeSearchRadius = 300;
	@StringGetter( "nodeSearchRadius" )
	public double getNodeSearchRadius() {
		return nodeSearchRadius;
	}
	@StringSetter( "nodeSearchRadius" )
	public void setNodeSearchRadius(double nodeSearchRadius) {
		this.nodeSearchRadius = nodeSearchRadius;
	}

	/**
	 * Number of link candidates considered for all stops, depends on accuracy of
	 * stops and desired performance. Somewhere between 4 and 10 seems reasonable,
	 * depending on the accuracy of the stop facility coordinates. Default: 8
	 */
	private int maxNClosestLinks = 8;
	@StringGetter( "maxNClosestLinks" )
	public int getMaxNClosestLinks() {
		return maxNClosestLinks;
	}
	@StringSetter( "maxNClosestLinks" )
	public void setMaxNClosestLinks(int maxNClosestLinks) {
		this.maxNClosestLinks = maxNClosestLinks;
	}

	/**
	 * The maximal distance [meter] a link candidate is allowed to have from
	 * the stop facility.
	 */
	private double maxStopFacilityDistance = 80;
	@StringGetter( "maxStopFacilityDistance" )
	public double getMaxStopFacilityDistance() {
		return maxStopFacilityDistance;
	}
	@StringSetter( "maxStopFacilityDistance" )
	public void setMaxStopFacilityDistance(double maxStopFacilityDistance) {
		this.maxStopFacilityDistance = maxStopFacilityDistance;
	}


	/**
	 * if two link candidates are the same travel time is multiplied by this
	 * factor. Otherwise travel time would just be the link traveltime
	 * since routing works with nodes
	 */
	private double sameLinkPunishment = 3;
	@StringGetter( "sameLinkPunishment" )
	public double getSameLinkPunishment() {
		return sameLinkPunishment;
	}
	@StringSetter( "sameLinkPunishment" )
	public void setSameLinkPunishment(double sameLinkPunishment) {
		this.sameLinkPunishment = sameLinkPunishment;
	}

	/**
	 * ID prefix used for artificial links and nodes created if no nodes
	 * are found within nodeSearchRadius
	 */
	private String prefixArtificial = "pt_";
	@StringGetter( "prefixArtificial" )
	public String getPrefixArtificial() {
		return prefixArtificial;
	}
	@StringSetter( "prefixArtificial" )
	public void setPrefixArtificial(String prefixArtificial) {
		this.prefixArtificial = prefixArtificial;
	}

	/**
	 * Suffix used for child stop facilities. A number for each child of a
	 * parent stop facility is appended (i.e. stop0123.fac:2).
	 */
	private String suffixChildStopFacilities = ".fac:";
	@StringGetter( "suffixChildStopFacilities" )
	public String getSuffixChildStopFacilities() {
		return suffixChildStopFacilities;
	}
	@StringSetter( "suffixChildStopFacilities" )
	public void setSuffixChildStopFacilities(String suffixChildStopFacilities) {
		this.suffixChildStopFacilities = suffixChildStopFacilities;
	}

	/**
	 * If all paths between two stops have a length > beelineDistanceMaxFactor * beelineDistance,
	 * an artificial link is created.
	 */
	private double beelineDistanceMaxFactor = 3;
	@StringGetter( "beelineDistanceMaxFactor" )
	public double getBeelineDistanceMaxFactor() {
		return beelineDistanceMaxFactor;
	}
	@StringSetter( "beelineDistanceMaxFactor" )
	public void setBeelineDistanceMaxFactor(double beelineDistanceMaxFactor) {
		this.beelineDistanceMaxFactor = beelineDistanceMaxFactor;
	}

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

	@StringGetter( "networkFile" )
	public String getNetworkFile() { return this.networkFile; }
	@StringSetter( "networkFile" )
	public String setNetworkFile(String networkFile) {
		final String old = this.networkFile;
		this.networkFile = networkFile;
		return old;
	}

	@StringGetter( "scheduleFile" )
	public String getScheduleFile() { return this.scheduleFile; }
	@StringSetter( "scheduleFile" )
	public String setScheduleFile(String scheduleFile) {
		final String old = this.scheduleFile;
		this.scheduleFile = scheduleFile;
		return old;
	}

	@StringGetter( "outputNetworkFile" )
	public String getOutputNetworkFile() { return this.outputNetworkFile; }
	@StringSetter( "outputNetworkFile" )
	public String setOutputNetwork(String outputNetwork) {
		final String old = this.outputNetworkFile;
		this.outputNetworkFile = outputNetwork;
		return old;
	}

	@StringGetter( "outputScheduleFile" )
	public String getOutputScheduleFile() { return this.outputScheduleFile; }
	@StringSetter( "outputScheduleFile" )
	public String setOutputSchedule(String outputSchedule) {
		final String old = this.outputScheduleFile;
		this.outputScheduleFile = outputSchedule;
		return old;
	}

	// /////////////////////////////////////////////////////////////////////
	// Default
	public static PublicTransportMapConfigGroup createDefaultConfig() {

		PublicTransportMapConfigGroup defaultConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), PublicTransportMapConfigGroup.GROUP_NAME, PublicTransportMapConfigGroup.class);

		defaultConfig.modesToKeepOnCleanUp.add("car");


		Set<String> busSet = new HashSet<>();
		busSet.add("bus");
		busSet.add("car");
		defaultConfig.modeRoutingAssignment.put("BUS", busSet);


		Set<String> tramSet = new HashSet<>();
		tramSet.add(ARTIFICIAL_LINK_MODE);
//		tramSet.add("tram");
		defaultConfig.modeRoutingAssignment.put("TRAM", tramSet);

		Set<String> railSet = new HashSet<>();
		railSet.add("rail");
		railSet.add("light_rail");
		defaultConfig.modeRoutingAssignment.put("RAIL", railSet);

		// subway, gondola, funicular, ferry and cablecar are not mapped

		return defaultConfig;
	}


	// todo change config to use different values for different modes
	/**
	 * Number of link candidates considered for all stops, different for scheduleModes.
	 * Depends on accuracy of stops and desired performance. Somewhere between 4 and 10 seems reasonable,
	 * depending on the accuracy of the stop facility coordinates. Default: 8
	 */
	public Map<String,Integer> getMaxNClosestLinksByMode() {
		return null;
	}
}
