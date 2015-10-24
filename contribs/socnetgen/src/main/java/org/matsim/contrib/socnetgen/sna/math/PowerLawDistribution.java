/* *********************************************************************** *
 * project: org.matsim.*
 * PowerLawDistribution.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetgen.sna.math;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * @author illenberger
 *
 */
public class PowerLawDistribution implements UnivariateRealFunction {

	private final double exponent;
	
	private final double factor;
	
	public PowerLawDistribution(double exponent, double factor) {
		this.exponent = exponent;
		this.factor = factor;
	}
	@Override
	public double value(double x) throws FunctionEvaluationException {
		return factor * Math.pow(x, exponent);
	}

}
