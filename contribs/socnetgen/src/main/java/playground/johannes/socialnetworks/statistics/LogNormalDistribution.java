/* *********************************************************************** *
 * project: org.matsim.*
 * LogNormalDistribution.java
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
public class LogNormalDistribution implements UnivariateRealFunction {

	private final double sigma;
	
	private final double mu;
	
	private final double intercept;
	
	public LogNormalDistribution(double sigma, double mu, double intercept) {
		this.sigma = sigma;
		this.mu = mu;
		this.intercept = intercept;
	}
	
	@Override
	public double value(double x) throws FunctionEvaluationException {
		return intercept / (Math.sqrt(2 * Math.PI) * sigma * x) * Math.exp(- Math.pow(Math.log(x) - mu, 2)/(2 * Math.pow(sigma, 2)));
	}

}
