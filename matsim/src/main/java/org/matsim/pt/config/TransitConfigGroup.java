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
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup.StringGetter;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author mrieser
 */
public class TransitConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "transit";

	/*package*/ static final String TRANSIT_SCHEDULE_FILE = "transitScheduleFile";
	private static final String TRANSIT_LINES_ATTRIBUTES = "transitLinesAttributesFile";
	private static final String TRANSIT_STOPS_ATTRIBUTES = "transitStopsAttributesFile";
	/*package*/ static final String VEHICLES_FILE = "vehiclesFile";
	/*package*/ static final String TRANSIT_MODES = "transitModes";
	private static final String SCHEDULE_CRS = "inputScheduleCRS";

	private String transitScheduleFile = null;
	private String vehiclesFile = null;
	private String transitLinesAttributesFile = null;
	private String transitStopsAttributesFile = null;
	private String inputScheduleCRS = null;

	private Set<String> transitModes;
	
	// ---
	private static final String USE_TRANSIT = "useTransit";
	private boolean useTransit = false;

	// ---

	public TransitConfigGroup() {
		super(GROUP_NAME);
		Set<String> modes = new LinkedHashSet<String>();
		modes.add(TransportMode.pt);
		this.transitModes = Collections.unmodifiableSet(modes);
	}

	@StringSetter( TRANSIT_MODES )
	private void setTransitModes( final String value ) {
		this.transitModes = Collections.unmodifiableSet(CollectionUtils.stringToSet(value));
	}

	@StringGetter( TRANSIT_MODES )
	private String getTransitModeString() {
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

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(TRANSIT_SCHEDULE_FILE, "Input file containing the transit schedule to be simulated.");
		comments.put(VEHICLES_FILE, "Input file containing the vehicles used by the departures in the transit schedule.");
		comments.put(TRANSIT_MODES, "Comma-separated list of transportation modes that are handled as transit. Defaults to 'pt'.");
		comments.put(TRANSIT_LINES_ATTRIBUTES, "Optional input file containing additional attributes for transit lines, stored as ObjectAttributes.");
		comments.put(TRANSIT_STOPS_ATTRIBUTES, "Optional input file containing additional attributes for transit stop facilities, stored as ObjectAttributes.");
		comments.put(USE_TRANSIT, "Set this parameter to true if transit should be simulated, false if not.");

		comments.put( SCHEDULE_CRS , "The Coordinates Reference System in which the coordinates are expressed in the input file." +
				" At import, the coordinates will be converted to the coordinate system defined in \"global\", and will" +
				"be converted back at export. If not specified, no conversion happens." );
		return comments;
	}

	@StringSetter( TRANSIT_SCHEDULE_FILE )
	public void setTransitScheduleFile(final String filename) {
		this.testForLocked();
		this.transitScheduleFile = filename;
	}

	@StringGetter( TRANSIT_SCHEDULE_FILE )
	public String getTransitScheduleFile() {
		return this.transitScheduleFile;
	}

	@StringSetter( VEHICLES_FILE )
	public void setVehiclesFile(final String filename) {
		this.vehiclesFile = filename;
	}

	@StringGetter( VEHICLES_FILE )
	public String getVehiclesFile() {
		return this.vehiclesFile;
	}

	public void setTransitModes(final Set<String> modes) {
		this.transitModes = Collections.unmodifiableSet(new HashSet<String>(modes));
	}

	public Set<String> getTransitModes() {
		return this.transitModes;
	}

	@StringGetter( TRANSIT_LINES_ATTRIBUTES )
	public String getTransitLinesAttributesFile() {
		return transitLinesAttributesFile;
	}
	
	@StringSetter( TRANSIT_LINES_ATTRIBUTES )
	public void setTransitLinesAttributesFile(final String transitLinesAttributesFile) {
		this.transitLinesAttributesFile = transitLinesAttributesFile;
	}

	@StringGetter( TRANSIT_STOPS_ATTRIBUTES )
	public String getTransitStopsAttributesFile() {
		return this.transitStopsAttributesFile;
	}
	
	@StringSetter( TRANSIT_STOPS_ATTRIBUTES )
	public void setTransitStopsAttributesFile(final String transitStopsAttributesFile) {
		this.transitStopsAttributesFile = transitStopsAttributesFile;
	}
	
	@StringGetter( USE_TRANSIT )
	public boolean isUseTransit() {
		return this.useTransit;
	}
	@StringSetter( USE_TRANSIT )
	public void setUseTransit( boolean val ) {
		this.testForLocked();
		this.useTransit = val ;
	}


	@StringGetter( SCHEDULE_CRS )
	public String getInputScheduleCRS() {
		return inputScheduleCRS;
	}

	@StringSetter( SCHEDULE_CRS )
	public void setInputScheduleCRS(String inputScheduleCRS) {
		this.inputScheduleCRS = inputScheduleCRS;
	}
}
