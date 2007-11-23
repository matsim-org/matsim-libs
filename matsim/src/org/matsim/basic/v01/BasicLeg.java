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

public class BasicLeg {

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
		this.mode = mode.intern();
	}

	public final void setRoute(BasicRoute route) {
		this.route = route;
	}

}
