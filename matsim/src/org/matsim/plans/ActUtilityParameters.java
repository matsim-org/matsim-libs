/* *********************************************************************** *
 * project: org.matsim.*
 * ActUtilityParameters.java
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

package org.matsim.plans;

public class ActUtilityParameters {

	private final String type;
	private final double priority;
	private final double typicalDuration;

	/**
	 * 	"duration at which the [performance] utility starts to be positive"
	 * (from Dave's paper, ga-acts-iatbr03.tex, though he called it t_0)
	 * (In decimal number of hours.)
	 */
	private final double zeroUtilityDuration; // in hours!
	private double minimalDuration = -1;
	private double openingTime = -1;
	private double closingTime = -1;
	private double latestStartTime = -1;
	private double earliestEndTime = -1;

	public ActUtilityParameters(final String type, final double priority, final double typicalDuration) {
		super();
		this.type = type;
		this.priority = priority;
		this.typicalDuration = typicalDuration;

		this.zeroUtilityDuration = (typicalDuration / 3600.0)
				* Math.exp( -10.0 / (typicalDuration / 3600.0) / priority );
		if (this.zeroUtilityDuration <= 0.0) {
			throw new RuntimeException("zeroUtilityDuration of type " + type + " must be greater than 0.0. Did you forget to specify the typicalDuration?");
		}
	}

	public final void setMinimalDuration(final double dur) {
		this.minimalDuration = dur;
	}

	public final void setOpeningTime(final double time) {
		this.openingTime = time;
	}

	public final void setClosingTime(final double time) {
		this.closingTime = time;
	}

	public final void setLatestStartTime(final double time) {
		this.latestStartTime = time;
	}

	public final void setEarliestEndTime(final double time) {
		this.earliestEndTime = time;
	}

	public final String getType() {
		return this.type;
	}

	public final double getPriority() {
		return this.priority;
	}

	public final double getTypicalDuration() {
		return this.typicalDuration;
	}

	public final double getZeroUtilityDuration() {
		return this.zeroUtilityDuration;
	}

	public final double getMinimalDuration() {
		return this.minimalDuration;
	}

	public final double getOpeningTime() {
		return this.openingTime;
	}

	public final double getClosingTime() {
		return this.closingTime;
	}

	public final double getLatestStartTime() {
		return this.latestStartTime;
	}

	public final double getEarliestEndTime() {
		return this.earliestEndTime;
	}

}
