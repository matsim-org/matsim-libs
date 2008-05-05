/* *********************************************************************** *
 * project: org.matsim.*
 * BasicLeg.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.basic.v01;

import org.apache.log4j.Logger;
import org.matsim.utils.misc.Time;

public class BasicLegImpl implements BasicLeg {

	protected int num = Integer.MIN_VALUE;
	protected String mode;
	protected BasicRoute route = null;

	private double depTime = Time.UNDEFINED_TIME;
	private double travTime = Time.UNDEFINED_TIME;
	private double arrTime = Time.UNDEFINED_TIME;

	
	public final int getNum() {
		return this.num;
	}

	public final void setNum(int num) {
		this.num = num;
	}

	public BasicRoute getRoute() {
		return this.route;
	}

	public final void setRoute(BasicRoute route) {
		this.route = route;
	}

	public final String getMode() {
		return this.mode;
	}

	public final void setMode(String mode) {
		if (MIVMODE.equalsIgnoreCase(mode))
			this.mode = MIVMODE;
		else if (CARMODE.equalsIgnoreCase(mode))
			this.mode = CARMODE;
		else if (RIDEMODE.equalsIgnoreCase(mode))
			this.mode = RIDEMODE;
		else if (MOTORBIKEMODE.equalsIgnoreCase(mode))
			this.mode = MOTORBIKEMODE;
		else if (PTMODE.equalsIgnoreCase(mode))
			this.mode = PTMODE;
		else if (TRAINMODE.equalsIgnoreCase(mode))
			this.mode = TRAINMODE;
		else if (BIKEMODE.equalsIgnoreCase(mode))
			this.mode = BIKEMODE;
		else if (WALKMODE.equalsIgnoreCase(mode))
			this.mode = WALKMODE;
		else {
			Logger.getLogger(BasicLegImpl.class).warn("Unknown Leg mode: " + mode);
			this.mode = mode.intern();
		}
	}

	public final double getDepTime() {
		return this.depTime;
	}

	public final void setDepTime(final double depTime) {
		this.depTime = depTime;
	}

	public final double getTravTime() {
		return this.travTime;
	}

	public final void setTravTime(final double travTime) {
		this.travTime = travTime;
	}

	public final double getArrTime() {
		return this.arrTime;
	}

	public final void setArrTime(final double arrTime) {
		this.arrTime = arrTime;
	}
}
