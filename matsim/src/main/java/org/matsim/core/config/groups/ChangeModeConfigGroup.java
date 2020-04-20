
/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeModeConfigGroup.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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


import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.utils.misc.StringUtils;

import java.util.Arrays;
import java.util.Map;

public class ChangeModeConfigGroup extends ReflectiveConfigGroup {

	public final static String CONFIG_MODULE = "changeMode";
	public final static String CONFIG_PARAM_MODES = "modes";
	public final static String CONFIG_PARAM_IGNORECARAVAILABILITY = "ignoreCarAvailability";
	public final static String MODE_SWITCH_BEHAVIOR = "modeSwitchBehavior";


	public enum Behavior { fromAllModesToSpecifiedModes, fromSpecifiedModesToSpecifiedModes }
	private Behavior behavior = Behavior.fromSpecifiedModesToSpecifiedModes ;

	private String[] modes = new String[] { TransportMode.car, TransportMode.pt };
	private boolean ignoreCarAvailability = true;
	public ChangeModeConfigGroup() {
		super(CONFIG_MODULE);
	}

	public String[] getModes() {
		return modes;
	}

	@StringGetter( CONFIG_PARAM_MODES )
	private String getModesString() {
		return toString( modes );
	}

	@StringSetter( CONFIG_PARAM_MODES )
	private void setModes( final String value ) {
		setModes( toArray( value ) );
	}

	public void setModes( final String[] modes ) {
		this.modes = modes;
	}

	@StringSetter( CONFIG_PARAM_IGNORECARAVAILABILITY )
	public void setIgnoreCarAvailability(final boolean value) {
		this.ignoreCarAvailability = value;
	}

	@StringGetter( CONFIG_PARAM_IGNORECARAVAILABILITY )
	public boolean getIgnoreCarAvailability() {
		return ignoreCarAvailability;
	}

	@StringGetter(MODE_SWITCH_BEHAVIOR)
	public Behavior getBehavior() {
		return behavior;
	}

	@StringSetter(MODE_SWITCH_BEHAVIOR)
	public void setBehavior(Behavior behavior) {
		this.behavior = behavior;
	}

	@Override
	public Map<String, String> getComments() {
		Map<String, String> comments = super.getComments();
		comments.put(CONFIG_PARAM_MODES, "Defines all the modes available, including chain-based modes, seperated by commas" );
		comments.put(CONFIG_PARAM_IGNORECARAVAILABILITY, "Defines whether car availability is considered be considered or not. An agent has no car only if it has no license, or never access to a car. Default: true" );
		comments.put(MODE_SWITCH_BEHAVIOR,"Defines the mode switch behavior. Possible values "+ Arrays.toString(Behavior.values()) +" Default: fromSpecifiedModesToSpecifiedModes.");
		return comments;
	}

	private static String toString(final String[] modes ) {
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


}
