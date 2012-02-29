/* *********************************************************************** *
 * project: org.matsim.*
 * ParkAndRideConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.parknride;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Module;

/**
 * @author thibautd
 */
public class ParkAndRideConfigGroup extends Module {
	public static final String GROUP_NAME = "parkAndRide";

	public static final String FACILITIES_FILE = "facilities";
	public static final String ALL_MODES = "availableModes";
	public static final String CHAIN_MODES = "chainBasedModes";

	private String facilitiesFile = null;
	private String[] modes = new String[]{
		TransportMode.car, TransportMode.pt, TransportMode.bike,
			TransportMode.walk, ParkAndRideConstants.PARK_N_RIDE_LINK_MODE};
	private String[] chainModes = new String[]{ TransportMode.car , TransportMode.bike };

	public ParkAndRideConfigGroup() {
		super( GROUP_NAME );
	}

	@Override
	public void addParam(
			final String param_name,
			final String value) {
		if (FACILITIES_FILE.equals( param_name )) {
			setFacilities( value );
		}
	}

	@Override
	public String getValue(final String param_name) {
		if (FACILITIES_FILE.equals( param_name )) {
			return getFacilities();
		}
		return null;
	}

	@Override
	public Map<String, String> getParams() {
		// linked hash map: the order in the file will correspond
		// to the order defined here
		Map<String, String> map = new LinkedHashMap<String, String>();

		addParameterToMap( map , FACILITIES_FILE );

		return map;
	}

	// /////////////////////////////////////////////////////////////////////////
	// getters/setters
	// /////////////////////////////////////////////////////////////////////////
	public void setFacilities(final String fileName) {
		facilitiesFile = fileName;
	}

	public String getFacilities() {
		return facilitiesFile;
	}

	public void setAvailableModes(final String value) {
		modes = value.split(",");
		
		for (int i=0; i<modes.length; i++) {
			modes[ i ] = modes[ i ].trim();
		}
	}

	public String[] getAvailableModes() {
		return modes;
	}

	public void setChainBasedModes(final String value) {
		chainModes = value.split(",");
		
		for (int i=0; i<modes.length; i++) {
			chainModes[ i ] = chainModes[ i ].trim();
		}
	}

	public String[] getChainBasedModes() {
		return chainModes;
	}
}

