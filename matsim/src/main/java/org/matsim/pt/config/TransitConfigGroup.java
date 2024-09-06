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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.collections.CollectionUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
	private static final String ROUTINGALGORITHM_TYPE = "routingAlgorithmType";

	private static final String INSISTING_ON_USING_DEPRECATED_ATTRIBUTE_FILE = "insistingOnUsingDeprecatedAttributeFiles" ;
	private static final String USING_TRANSIT_IN_MOBSIM = "usingTransitInMobsim" ;

	public enum TransitRoutingAlgorithmType {@Deprecated DijkstraBased, SwissRailRaptor}

	public static final String TRANSIT_ATTRIBUTES_DEPRECATION_MESSAGE = "using the separate transit stops and lines attribute files is deprecated." +
			"  Add the information directly into each stop or line, using " +
			"the Attributable feature.  If you insist on continuing to use the separate attribute files, set " +
			"insistingOnUsingDeprecatedAttributeFiles to true.  The files will then be read, but the values " +
			"will be entered into each stop or line using Attributable, and written as such to output_transitSchedule.";

	private String transitScheduleFile = null;
	private String vehiclesFile = null;
	private String transitLinesAttributesFile = null;
	private String transitStopsAttributesFile = null;
	private String inputScheduleCRS = null;

	private Set<String> transitModes;
	private TransitRoutingAlgorithmType routingAlgorithmType = TransitRoutingAlgorithmType.SwissRailRaptor;

	// ---
	private static final String USE_TRANSIT = "useTransit";
	private boolean useTransit = false;
	private boolean insistingOnUsingDeprecatedAttributeFiles = false;

	// ---

	public TransitConfigGroup() {
		super(GROUP_NAME);
		Set<String> modes = new LinkedHashSet<>();
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
		comments.put(ROUTINGALGORITHM_TYPE, "The type of transit routing algorithm used, may have the values: " + Arrays.toString(TransitRoutingAlgorithmType.values()));

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

	public URL getTransitScheduleFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, getTransitScheduleFile() ) ;
	}

	@StringSetter( VEHICLES_FILE )
	public void setVehiclesFile(final String filename) {
		this.vehiclesFile = filename;
	}

	@StringGetter( VEHICLES_FILE )
	public String getVehiclesFile() {
		return this.vehiclesFile;
	}

	public URL getVehiclesFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, getVehiclesFile() ) ;
	}

	public void setTransitModes(final Set<String> modes) {
		this.transitModes = Collections.unmodifiableSet(new HashSet<>(modes));
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
	public URL getTransitStopsAttributesFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, getTransitStopsAttributesFile()) ;
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

	@StringGetter( ROUTINGALGORITHM_TYPE )
	public TransitRoutingAlgorithmType getRoutingAlgorithmType() {
		return this.routingAlgorithmType;
	}

	@StringSetter( ROUTINGALGORITHM_TYPE )
	public void setRoutingAlgorithmType(final TransitRoutingAlgorithmType type) {
		this.routingAlgorithmType = type;
	}

	@StringGetter( SCHEDULE_CRS )
	public String getInputScheduleCRS() {
		return inputScheduleCRS;
	}

	@StringSetter( SCHEDULE_CRS )
	public void setInputScheduleCRS(String inputScheduleCRS) {
		this.inputScheduleCRS = inputScheduleCRS;
	}

	public static final String BOARDING_ACCEPTANCE_CMT="under which conditions agent boards transit vehicle" ;
	public enum BoardingAcceptance { checkLineAndStop, checkStopOnly }
	private BoardingAcceptance boardingAcceptance = BoardingAcceptance.checkLineAndStop ;
	public BoardingAcceptance getBoardingAcceptance() {
		return this.boardingAcceptance;
	}
	public void setBoardingAcceptance(BoardingAcceptance boardingAcceptance) {
		this.boardingAcceptance = boardingAcceptance;
	}

	private boolean usingTransitInMobsim = true ;
	@StringSetter( USING_TRANSIT_IN_MOBSIM )
	public final void setUsingTransitInMobsim( boolean val ) {
		usingTransitInMobsim = val ;
	}
	@StringGetter( USING_TRANSIT_IN_MOBSIM )
	public final boolean isUsingTransitInMobsim(){
		return usingTransitInMobsim ;
	}

	@StringSetter(INSISTING_ON_USING_DEPRECATED_ATTRIBUTE_FILE)
	public final void setInsistingOnUsingDeprecatedAttributeFiles( boolean val ) {
		this.insistingOnUsingDeprecatedAttributeFiles = val ;
	}
	@StringGetter(INSISTING_ON_USING_DEPRECATED_ATTRIBUTE_FILE)
	public final boolean isInsistingOnUsingDeprecatedAttributeFiles() {
		return insistingOnUsingDeprecatedAttributeFiles;
	}

}
