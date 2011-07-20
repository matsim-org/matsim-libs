/* *********************************************************************** *
 * project: org.matsim.*
 * ExponentialDistribution.java
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
package playground.johannes.socialnetworks.statistics;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * @author illenberger
 *
 */
public class ExponentialDistribution implements UnivariateRealFunction {


	private final double lambda;
	
	private final double intercept;
	
	public ExponentialDistribution(double lambda, double intercept) {
		this.lambda = lambda;
		this.intercept = intercept;
	}
	
	@Override
	public double value(double arg0) throws FunctionEvaluationException {
		return intercept * Math.exp(lambda * arg0);
	}

}
