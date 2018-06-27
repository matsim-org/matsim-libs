/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * SimpleDisruptionConfigGroup.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2018 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.pt.withinday;

import org.matsim.core.config.ReflectiveConfigGroup;

public class WithinDayTransitConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "withinDayTransit";

	private String disruptionsFile;
	private String behavior;
	
	public WithinDayTransitConfigGroup() {
		super(GROUP_NAME);
	}

	@StringSetter("disruptionsFile")
	public void setDisruptionsFile(String disruptionFile) {
		this.disruptionsFile = disruptionFile;
	}
	
	@StringGetter("disruptionsFile")
	public String getDisruptionsFile() {
		return disruptionsFile;
	}
	
	@StringSetter("behavior")
	public void setBehavior(String behavior) {
		this.behavior = behavior;
	}
	
	@StringGetter("behavior")
	public String getBehavior() {
		return behavior;
	}

}
