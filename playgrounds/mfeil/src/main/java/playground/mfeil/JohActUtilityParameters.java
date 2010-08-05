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

package playground.mfeil;

/**
 * 	Joh's utility function parameters
 *  @author Matthias Feil
 */

public class JohActUtilityParameters {

	private final String type;
	private final double uMin;
	private final double uMax;
	private final double alpha;
	private final double beta;
	private final double gamma;

	private double openingTime = -1;
	private double closingTime = -1;
	private double latestStartTime = -1;
	private double earliestEndTime = -1;

	public JohActUtilityParameters(final String type, final double uMin,
			final double uMax, final double alpha, final double beta, final double gamma) {
		this.type = type;
		this.uMin = uMin;
		this.uMax = uMax;
		this.alpha = alpha;
		this.beta = beta;
		this.gamma = gamma;
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

	public final double getUMin() {
		return this.uMin;
	}

	public final double getUMax() {
		return this.uMax;
	}

	public final double getAlpha() {
		return this.alpha;
	}

	public final double getBeta() {
		return this.beta;
	}
	
	public final double getGamma() {
		return this.gamma;
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
