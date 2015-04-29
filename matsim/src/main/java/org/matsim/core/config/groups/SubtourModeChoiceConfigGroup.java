/* *********************************************************************** *
 * project: org.matsim.*
 * SubtourModeChoiceConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.core.config.groups;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.experimental.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author thibautd
 */
public class SubtourModeChoiceConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "subtourModeChoice";
	
	public final static String MODES = "modes";
	public final static String CHAINBASEDMODES = "chainBasedModes";
	public final static String CARAVAIL = "considerCarAvailability";
	
	private String[] chainBasedModes = new String[] { TransportMode.car, TransportMode.bike };
	private String[] allModes = new String[] { TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk };
	// default is false for backward compatibility
	private boolean considerCarAvailability = false;

	public SubtourModeChoiceConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter( MODES )
	private String getModesString() {
		return toString( allModes );
	}

	@StringGetter( CHAINBASEDMODES )
	private String getChainBaseModesString() {
		return toString( chainBasedModes );
	}

	private String toString( final String[] modes ) {
		StringBuilder b = new StringBuilder();

		if (modes.length > 0) b.append( modes[ 0 ] );
		for (int i=1; i < modes.length; i++) {
			b.append( ',' );
			b.append( modes[ i ] );
		}

		return b.toString();
	}

	private String[] toArray( final String modes ) {
		String[] parts = StringUtils.explode(modes, ',');

		for (int i = 0, n = parts.length; i < n; i++) {
			parts[i] = parts[i].trim().intern();
		}

		return parts;
	}

	@StringSetter( MODES )
	private void setModes( final String value ) {
		setModes( toArray( value ) );
	}

	@StringSetter( CHAINBASEDMODES )
	private void setChainBasedModes( final String value ) {
		setChainBasedModes( toArray( value ) );
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(MODES, "Defines all the modes available, including chain-based modes, seperated by commas" );
		comments.put(CHAINBASEDMODES, "Defines the chain-based modes, seperated by commas" );
		comments.put(CARAVAIL, "Defines whether car availability must be considered or not. A agent has no car only if it has no license, or never access to a car" );
		return comments;
	}

	/* direct access */

	public void setModes( final String[] modes ) {
		this.allModes = modes;
	}

	public String[] getModes() {
		return this.allModes;
	}

	public void setChainBasedModes( final String[] modes ) {
		this.chainBasedModes = modes;
	}

	public String[] getChainBasedModes() {
		return this.chainBasedModes;
	}

	@StringSetter( CARAVAIL )
	public void setConsiderCarAvailability(final boolean value) {
		this.considerCarAvailability = value;
	}

	@StringGetter( CARAVAIL )
	public boolean considerCarAvailability() {
		return considerCarAvailability;
	}
}
