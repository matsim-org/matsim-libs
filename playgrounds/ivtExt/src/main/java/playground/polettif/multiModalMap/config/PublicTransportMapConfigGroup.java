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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ReflectiveConfigGroup;

import java.util.*;


/**
 * Demonstrate how to use ReflectiveModule to easily create typed config groups.
 * Please do not modify this class: it is used from unit tests!
 */
public class PublicTransportMapConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "MultiModalMap";

	// TODO: test for ALL primitive types
	private double doubleField = Double.NaN;

	// Object fields:
	// Id: string representation is toString
	private Id<Link> idField = null;
	// Coord: some conversion needed
	private Coord coordField = null;
	// enum: handled especially
	private PublicTransportMapEnum enumField = null;
	// field without null conversion
	private String nonNull = "some arbitrary default value.";


	private Set<String> modesToCleanUp = new HashSet<>();
	private Set<String> mapBusTo = new HashSet<>();
	private double nodeSearchRadius = 300;
	private int maxNClosestLinks = 2;
	private double maxStopFacilityDistance = 80;
	private double sameLinkPunishment = 10;
	private String prefixArtificialLinks = "pt_";
	private String suffixChildStopFacilities = ".fac:";

	/**
	 * References transportModes from the schedule (key) and the
	 * allowed modes of a link from the network (value). <p/>
	 * <p/>
	 * Schedule transport modes should be in gtfs categories:
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
	private Map<String, Set<String>> modes = new HashMap<>();

	public PublicTransportMapConfigGroup() {
		super( GROUP_NAME );
	}

	// /////////////////////////////////////////////////////////////////////
	// primitive type field: standard getter and setter suffice
	@StringGetter( "doubleField" )
	public double getDoubleField() {
		return this.doubleField;
	}

	// there should be no restriction on return type of
	// setters
	@StringSetter( "doubleField" )
	public double setDoubleField(double doubleField) {
		final double old = this.doubleField;
		this.doubleField = doubleField;
		return old;
	}

	// /////////////////////////////////////////////////////////////////////
	// id field: need for a special setter, normal getter suffice
	/**
	 * string representation of Id is result of
	 * toString: just annotate getter
	 */
	@StringGetter( "idField" )
	public Id<Link> getIdField() {
		return this.idField;
	}

	public void setIdField(Id<Link> idField) {
		this.idField = idField;
	}

	/**
	 * We need to do the conversion from string to Id
	 * ourselves.
	 * the annotated setter can be private to avoid polluting the
	 * interface: the user just sees the "typed" setter.
	 */
	@StringSetter( "idField" )
	private void setIdField(String s) {
		// Null handling needs to be done manually if conversion "by hand"
		this.idField = s == null ? null : Id.create( s, Link.class );
	}

	// /////////////////////////////////////////////////////////////////////
	// coord field: need for special getter and setter
	public Coord getCoordField() {
		return this.coordField;
	}

	public void setCoordField(Coord coordField) {
		this.coordField = coordField;
	}

	// we have to convert both ways here.
	// the annotated getter and setter can be private to avoid polluting the
	// interface: the user just sees the "typed" getter and setter.
	@StringGetter( "coordField" )
	private String getCoordFieldString() {
		// Null handling needs to be done manually if conversion "by hand"
		// Note that one *needs" to return a null pointer, not the "null"
		// String, which is reserved word.
		return this.coordField == null ? null : this.coordField.getX()+","+this.coordField.getY();
	}

	@StringSetter( "coordField" )
	private void setCoordField(String coordField) {
		if ( coordField == null ) {
			// Null handling needs to be done manually if conversion "by hand"
			this.coordField = null;
			return;
		}

		final String[] coords = coordField.split( "," );
		if ( coords.length != 2 ) throw new IllegalArgumentException( coordField );

		this.coordField = new Coord(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]));
	}

	// /////////////////////////////////////////////////////////////////////////
	// Non-null string: standard setter and getter
	@StringGetter( "nonNullField" )
	@DoNotConvertNull
	public String getNonNull() {
		return nonNull;
	}

	@StringSetter( "nonNullField" )
	@DoNotConvertNull
	public void setNonNull( String nonNull ) {
		// in case the setter is called from user code, we need to check for nullity ourselves.
		if ( nonNull == null ) throw new IllegalArgumentException();
		this.nonNull = nonNull;
	}

	// /////////////////////////////////////////////////////////////////////
	// enum: normal getter and setter suffice
	@StringGetter( "enumField" )
	public PublicTransportMapEnum getTestEnumField() {
		return this.enumField;
	}

	@StringSetter( "enumField" )
	public void setTestEnumField(final PublicTransportMapEnum enumField) {
		// no need to test for null: the parent class does it for us
		this.enumField = enumField;
	}

	// /////////////////////////////////////////////////////////////////////
	// Default
	public static PublicTransportMapConfigGroup createDefaultConfig() {

		PublicTransportMapConfigGroup defaultConfig = ConfigUtils.addOrGetModule(ConfigUtils.createConfig(), PublicTransportMapConfigGroup.GROUP_NAME, PublicTransportMapConfigGroup.class);

		defaultConfig.modesToCleanUp.add("rail");
		defaultConfig.mapBusTo.add("bus");

		Set<String> busSet = new HashSet<>(); busSet.add("bus"); busSet.add("car");
		defaultConfig.modes.put("BUS", busSet);

//		defaultConfig.modes.put("BUS", Collections.singleton("bus"));
/*
		Set<String> tramSet = new HashSet<>(); tramSet.add("tram");
		defaultConfig.modes.put("TRAM", tramSet);

		Set<String> railSet = new HashSet<>(); railSet.add("rail");
		defaultConfig.modes.put("RAIL", railSet);
*/
		// subway, gondola, funicular, ferry and cablecar are not mapped

		return defaultConfig;
	}

	public double getNodeSearchRadius() {
		return nodeSearchRadius;
	}

	public int getMaxNClosestLinks() {
		return maxNClosestLinks;
	}


	public double getMaxStopFacilityDistance() {
		return maxStopFacilityDistance;
	}

	public double getSameLinkPunishment() {
		return sameLinkPunishment;
	}

	public String getPrefixArtificialLinks() {
		return prefixArtificialLinks;
	}

	public String getSuffixChildStopFacilities() {
		return suffixChildStopFacilities;
	}

	public Set<String> getModesToCleanUp() {
		return modesToCleanUp;
	}

	public Set<String> getMapBusTo() {
		return mapBusTo;
	}

	public Map<String, Set<String>> getModes() {
		return modes;
	}

	public Set<String> getNetworkModes() {
		Set<String> networkModes = new HashSet<>();
		modes.values().forEach(networkModes::addAll);
		return networkModes;
	}

	public Set<String> getScheduleModes() {
		Set<String> scheduleModes = new HashSet<>();
		modes.keySet().forEach(scheduleModes::add);
		return scheduleModes;
	}
}
