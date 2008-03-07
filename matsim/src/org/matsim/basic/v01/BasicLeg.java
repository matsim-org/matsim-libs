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

public class BasicLeg {

	public static final String MIVMODE = "miv";
	public static final String CARMODE = "car";
	public static final String RIDEMODE = "ride";
	public static final String MOTORBIKEMODE = "motorbike";
	public static final String PTMODE = "pt";
	public static final String TRAINMODE = "train";
	public static final String BUSMODE = "bus";
	public static final String TRAMMODE = "tram";
	public static final String BIKEMODE = "bike";
	public static final String WALKMODE = "walk";
	public static final String UNDEFINED = "undef";

	protected int num = Integer.MIN_VALUE;
	protected String mode;
	protected BasicRoute route = null;

	//////////////////////////////////////////////////////////////////////
	// getter methods
	//////////////////////////////////////////////////////////////////////

	public final int getNum() {
		return this.num;
	}

	public final String getMode() {
		return this.mode;
	}

	// could be overwritten in higher classes for providing  BasicRoute derived
	// return values
	public BasicRoute getRoute() {
		return this.route;
	}

	//////////////////////////////////////////////////////////////////////
	// setter methods
	//////////////////////////////////////////////////////////////////////

	public final void setNum(int num) {
		this.num = num;
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
			Logger.getLogger(BasicLeg.class).warn("Unknown Leg mode: " + mode);
			this.mode = mode.intern();
		}
	}

	public final void setRoute(BasicRoute route) {
		this.route = route;
	}

}
