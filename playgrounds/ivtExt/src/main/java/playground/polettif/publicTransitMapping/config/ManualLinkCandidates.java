/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.api.internal.MatsimParameters;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashSet;
import java.util.Set;

public class ManualLinkCandidates extends ReflectiveConfigGroup implements MatsimParameters {

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