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

package org.matsim.core.scoring.functions;

import org.matsim.core.api.internal.MatsimParameters;

public class ActivityUtilityParameters implements MatsimParameters {

	private final String type;
	private final double priority;
	private final double typicalDuration_s;

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
	private boolean scoreAtAll=true;

	public ActivityUtilityParameters(final String type, final double priority, final double typicalDuration_s) {
		//if typical duration is <=48 seconds (and priority=1) then zeroUtilityDuration becomes 0.0 because of the double precision. This means it is not possible
		// to have activities with a typical duration <=48 seconds (GL/June2011)
		super();
		this.type = type;
		this.priority = priority;
		this.typicalDuration_s = typicalDuration_s;

		this.zeroUtilityDuration = (typicalDuration_s / 3600.0)
		* Math.exp( -10.0 / (typicalDuration_s / 3600.0) / priority );
		// ( the 3600s are in there because the original formulation was in "hours".  So the values in seconds are first
		// translated into hours.  kai, sep'12 )

		// example: pt interaction activity with typical duration = 120sec.
		// 120/3600 * exp( -10 / (120 / 3600) ) =  1.7 x 10^(-132)  (!!!!!!!!!!)
		// In consequence, even a pt interaction of one seconds causes a fairly large utility.

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
		return this.typicalDuration_s;
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

	public boolean isScoreAtAll() {
		return scoreAtAll;
	}

	public void setScoreAtAll(boolean scoreAtAll) {
		this.scoreAtAll = scoreAtAll;
	}

}
