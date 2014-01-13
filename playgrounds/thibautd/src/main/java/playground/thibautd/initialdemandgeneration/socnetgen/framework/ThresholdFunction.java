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
	private double secondaryTieThreshold;

	public ThresholdFunction(
			final double initialPrimaryTieThreshold,
			final double initialSecondaryTieThreshold) {
		this.primaryTieThreshold = initialPrimaryTieThreshold;
		this.secondaryTieThreshold = initialSecondaryTieThreshold;
	}

	public double getPrimaryTieThreshold() {
		return this.primaryTieThreshold;
	}

	public void setPrimaryTieThreshold(final double primaryTieThreshold) {
		this.primaryTieThreshold = primaryTieThreshold;
	}

	public double getSecondaryTieThreshold() {
		return this.secondaryTieThreshold;
	}

	public void setSecondaryTieThreshold(final double secondaryTieThreshold) {
		this.secondaryTieThreshold = secondaryTieThreshold;
	}
}

