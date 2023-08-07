/* *********************************************************************** *
 * project: org.matsim.*
 * HbefaWarmEmissionFactor.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.emissions;


/**
 * @author benjamin
 */
class HbefaWarmEmissionFactor extends HbefaEmissionFactor {

	private final double speed;

//	public String getRoadGradient() {
//		return roadGradient;
//	}
//
//	private final String roadGradient;

//	/*package-private*/ HbefaWarmEmissionFactor(double factor, double speed, @Deprecated String roadGradient) {

//	 My strong intuition is that roadGradient does <i>not</i> belong into the emission factor.  Since the emissions factor is output,
//	 not input.  For this, one has to note that for HBEFA speed is output, for a certain traffic state on a certain road category.  This is
//	 nonwithstanding the fact that for some versions of the emission calculation it is then used as input.  kai, apr'23

	/*package-private*/ HbefaWarmEmissionFactor(double factor, double speed ) {
		super(factor);
		this.speed = speed;
//		this.roadGradient = roadGradient;
	}

	public double getSpeed() {
		return speed;
	}

	@Override
	public String toString() {
		return "HbefaWarmEmissionFactor{" +
				"speed=" + speed +
//				", roadGradient=" + roadGradient +
				", warmEmissionFactor=" + getFactor() +
				'}';
	}
}
