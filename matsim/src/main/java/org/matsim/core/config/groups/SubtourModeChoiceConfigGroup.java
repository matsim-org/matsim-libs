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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.utils.misc.StringUtils;

/**
 * @author thibautd
 */
public final class SubtourModeChoiceConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "subtourModeChoice";

	public final static String MODES = "modes";
	public final static String CHAINBASEDMODES = "chainBasedModes";
	public final static String CARAVAIL = "considerCarAvailability";
	public final static String SINGLE_PROBA = "probaForRandomSingleTripMode";
	public final static String COORD_DISTANCE = "coordDistance";

	private static final String BEHAVIOR = "behavior";

	private String[] chainBasedModes = new String[] { TransportMode.car, TransportMode.bike };
	private String[] allModes = new String[] { TransportMode.car, TransportMode.pt, TransportMode.bike, TransportMode.walk };
	// default is false for backward compatibility
	private boolean considerCarAvailability = false;
	private SubtourModeChoice.Behavior behavior = SubtourModeChoice.Behavior.fromSpecifiedModesToSpecifiedModes ;

	private double probaForRandomSingleTripMode = 0. ; // yyyyyy backwards compatibility setting; should be change. kai, may'18

	private double coordDistance = 0;

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

	private static String toString( final String[] modes ) {
		// (not same as toString() because of argument!)

		StringBuilder b = new StringBuilder();

		if (modes.length > 0) b.append( modes[ 0 ] );
		for (int i=1; i < modes.length; i++) {
			b.append( ',' );
			b.append( modes[ i ] );
		}

		return b.toString();
	}

	private static String[] toArray( final String modes ) {
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
		comments.put(SINGLE_PROBA, "Defines the probability of changing a single trip for a unchained mode instead of subtour.");
		comments.put(COORD_DISTANCE, "If greater than 0, activities that are closer than coordDistance, to each other, will be considered part of the same subtour." +
			"i.e. if two activities are close to each other, the agent is allowed to use the same 'chain-based' vehicle for both subtours.");

		{
			StringBuilder msg = new StringBuilder("Only for backwards compatibility.  Defines if only trips from modes list should change mode, or all trips.  Options: ");
			for ( SubtourModeChoice.Behavior behavior : SubtourModeChoice.Behavior.values() ) {
				msg.append(behavior.name());
				msg.append(' ');
			}
			comments.put(BEHAVIOR, msg.toString());
		}
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
	@StringSetter( BEHAVIOR )
	public final void setBehavior( SubtourModeChoice.Behavior behavior ) {
		this.behavior = behavior ;
	}
	@StringGetter( BEHAVIOR )
	public final SubtourModeChoice.Behavior getBehavior() {
		return this.behavior ;
	}

	@StringGetter(SINGLE_PROBA)
	public double getProbaForRandomSingleTripMode() {
		return this.probaForRandomSingleTripMode;
	}

	@StringSetter(SINGLE_PROBA)
	public void setProbaForRandomSingleTripMode(double probaForRandomSingleTripMode) {
		this.probaForRandomSingleTripMode = probaForRandomSingleTripMode;
	}

	@StringGetter(COORD_DISTANCE)
	public double getCoordDistance() {
		return coordDistance;
	}

	@StringSetter(COORD_DISTANCE)
	public void setCoordDistance(double coordDistance) {
		this.coordDistance = coordDistance;
	}
}
