/* *********************************************************************** *
 * project: org.matsim.*
 * ThresholdFunction.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

/**
 * @author thibautd
 */
public class ThresholdFunction {
	private double primaryTieThreshold;
	private double secondaryReduction;

	public ThresholdFunction(
			final double initialPrimaryTieThreshold,
			final double initialSecondaryReduction) {
		this.primaryTieThreshold = initialPrimaryTieThreshold;
		this.secondaryReduction = initialSecondaryReduction;
	}

	public double getPrimaryTieThreshold() {
		return this.primaryTieThreshold;
	}

	public void setPrimaryTieThreshold(final double primaryTieThreshold) {
		this.primaryTieThreshold = primaryTieThreshold;
	}

	public double getSecondaryTieThreshold() {
		return this.primaryTieThreshold - this.secondaryReduction;
	}

	public double getSecondaryReduction() {
		return secondaryReduction;
	}

	public void setSecondaryReduction(final double secondaryReduction) {
		this.secondaryReduction = secondaryReduction;
	}
}

