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
import org.matsim.core.utils.misc.Time;

public class SimpleDisruptionConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUP_NAME = "simpleDisruption";
	
	private double disruptionStart = 0;
	
	public SimpleDisruptionConfigGroup() {
		super(GROUP_NAME);
	}

	public double getDisruptionStart() {
		return disruptionStart;
	}
	
	public void setDisruptionStart(double disruptionStart) {
		this.disruptionStart = disruptionStart;
	}
	
	@StringGetter("disruptionStart")
	public String getDisruptionStartAsString() {
		return Time.writeTime(disruptionStart);
	}
	
	@StringSetter("disruptionStart")
	public void setDisruptionStartAsString(String disruptionStart) {
		this.disruptionStart = Time.parseTime(disruptionStart);
	}

	
}
