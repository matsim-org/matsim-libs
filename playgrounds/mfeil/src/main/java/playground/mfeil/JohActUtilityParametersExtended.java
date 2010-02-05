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
 * 	For Joh's utility function
 *  @author Matthias Feil
 */

public class JohActUtilityParametersExtended extends JohActUtilityParameters{

	private final double beta_age;

	public JohActUtilityParametersExtended(final String type, final double uMin,
			final double uMax, final double alpha, final double beta, final double gamma, final double beta_age) {
		super(type, uMin, uMax, alpha, beta, gamma);
		this.beta_age = beta_age;
	}
	
	public final double getBetaAge() {
		return this.beta_age;
	}
}
