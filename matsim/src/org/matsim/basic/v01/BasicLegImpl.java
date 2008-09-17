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

import org.matsim.utils.misc.Time;

public class BasicLegImpl implements BasicLeg {

	protected int num = Integer.MIN_VALUE;
	protected BasicRoute route = null;

	private double depTime = Time.UNDEFINED_TIME;
	private double travTime = Time.UNDEFINED_TIME;
	private double arrTime = Time.UNDEFINED_TIME;
	private Mode mode;

	
	public BasicLegImpl(BasicLeg.Mode mode) {
		this.mode = mode;
	}
	
	
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

	public final Mode getMode() {
		return this.mode;
	}

	public final void setMode(Mode mode) {
		this.mode = mode;
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
