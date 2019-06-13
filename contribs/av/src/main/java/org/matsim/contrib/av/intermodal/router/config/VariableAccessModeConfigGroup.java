/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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

/**
 * 
 */
package org.matsim.contrib.av.intermodal.router.config;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author jbischoff
 *
 */

public final class VariableAccessModeConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUPNAME = VariableAccessConfigGroup.MODEGROUPNAME;

	private static final String MODEDISTANCE = "accessDistance_m";
	private static final String MODE = "mode";
	private static final String TELEPORTED = "isTeleported";

	private double distance;
	private String mode;
	private boolean teleported;

	public VariableAccessModeConfigGroup() {
		super(GROUPNAME);
	}

	@StringGetter(MODEDISTANCE)
	public double getDistance() {
		return distance;
	}

	@StringSetter(MODEDISTANCE)
	public void setDistance(double distance) {
		this.distance = distance;
	}

	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
	}

	@StringGetter(TELEPORTED)
	public boolean isTeleported() {
		return teleported;
	}

	@StringSetter(TELEPORTED)
	public void setTeleported(boolean teleported) {
		this.teleported = teleported;
	}

}
