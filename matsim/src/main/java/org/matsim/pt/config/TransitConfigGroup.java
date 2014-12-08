/* *********************************************************************** *
 * project: org.matsim.*
 * TransitConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.config;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author mrieser
 */
public class TransitConfigGroup extends ConfigGroup {

	public static final String GROUP_NAME = "transit";

	/*package*/ static final String TRANSIT_SCHEDULE_FILE = "transitScheduleFile";
	private static final String TRANSIT_LINES_ATTRIBUTES = "transitLinesAttributesFile";
	private static final String TRANSIT_STOPS_ATTRIBUTES = "transitStopsAttributesFile";
	/*package*/ static final String VEHICLES_FILE = "vehiclesFile";
	/*package*/ static final String TRANSIT_MODES = "transitModes";

	private String transitScheduleFile = null;
	private String vehiclesFile = null;
	private String transitLinesAttributesFile = null;
	private String transitStopsAttributesFile = null;

	private Set<String> transitModes;

	public TransitConfigGroup() {
		super(GROUP_NAME);
		Set<String> modes = new LinkedHashSet<String>();
		modes.add(TransportMode.pt);
		this.transitModes = Collections.unmodifiableSet(modes);
	}

	@Override
	public void addParam(final String paramName, final String value) {
		if (TRANSIT_SCHEDULE_FILE.equals(paramName)) {
			setTransitScheduleFile(value);
		} else if (VEHICLES_FILE.equals(paramName)) {
				setVehiclesFile(value);
		} else if (TRANSIT_MODES.equals(paramName)) {
			this.transitModes = Collections.unmodifiableSet(CollectionUtils.stringToSet(value));
		} else if (TRANSIT_LINES_ATTRIBUTES.equals(paramName)) {
			this.transitLinesAttributesFile = value;
		} else if (TRANSIT_STOPS_ATTRIBUTES.equals(paramName)) {
			this.transitStopsAttributesFile = value;
		} else {
			throw new IllegalArgumentException(paramName);
		}
	}

	@Override
	public String getValue(final String paramName) {
		if (TRANSIT_SCHEDULE_FILE.equals(paramName)) {
			return getTransitScheduleFile();
		}
		if (VEHICLES_FILE.equals(paramName)) {
			return getVehiclesFile();
		}
		if (TRANSIT_MODES.equals(paramName)) {
			boolean isFirst = true;
			StringBuilder str = new StringBuilder();
			for (String mode : this.transitModes) {
				if (!isFirst) {
					str.append(',');
				}
				str.append(mode);
				isFirst = false;
			}
			return str.toString();
		}
		throw new IllegalArgumentException(paramName);
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> params = super.getParams();
		addParameterToMap(params, TRANSIT_SCHEDULE_FILE);
		addParameterToMap(params, VEHICLES_FILE);
		addParameterToMap(params, TRANSIT_MODES);
		if (this.transitLinesAttributesFile != null) {
			params.put(TRANSIT_LINES_ATTRIBUTES, this.transitLinesAttributesFile);
		}
		if (this.transitStopsAttributesFile != null) {
			params.put(TRANSIT_STOPS_ATTRIBUTES, this.transitStopsAttributesFile);
		}
		return params;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(TRANSIT_SCHEDULE_FILE, "Input file containing the transit schedule to be simulated.");
		comments.put(VEHICLES_FILE, "Input file containing the vehicles used by the departures in the transit schedule.");
		comments.put(TRANSIT_MODES, "Comma-separated list of transportation modes that are handled as transit. Defaults to 'pt'.");
		comments.put(TRANSIT_LINES_ATTRIBUTES, "Optional input file containing additional attributes for transit lines, stored as ObjectAttributes.");
		comments.put(TRANSIT_STOPS_ATTRIBUTES, "Optional input file containing additional attributes for transit stop facilities, stored as ObjectAttributes.");
		return comments;
	}

	public void setTransitScheduleFile(final String filename) {
		this.transitScheduleFile = filename;
	}

	public String getTransitScheduleFile() {
		return this.transitScheduleFile;
	}

	public void setVehiclesFile(final String filename) {
		this.vehiclesFile = filename;
	}

	public String getVehiclesFile() {
		return this.vehiclesFile;
	}

	public void setTransitModes(final Set<String> modes) {
		this.transitModes = Collections.unmodifiableSet(new HashSet<String>(modes));
	}

	public Set<String> getTransitModes() {
		return this.transitModes;
	}

	public String getTransitLinesAttributesFile() {
		return transitLinesAttributesFile;
	}
	
	public void setTransitLinesAttributesFile(final String transitLinesAttributesFile) {
		this.transitLinesAttributesFile = transitLinesAttributesFile;
	}

	public String getTransitStopsAttributesFile() {
		return this.transitStopsAttributesFile;
	}
	
	public void setTransitStopsAttributesFile(final String transitStopsAttributesFile) {
		this.transitStopsAttributesFile = transitStopsAttributesFile;
	}
}
