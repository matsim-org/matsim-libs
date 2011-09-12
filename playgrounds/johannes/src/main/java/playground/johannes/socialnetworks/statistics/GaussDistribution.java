/* *********************************************************************** *
 * project: org.matsim.*
 * GaussDistribution.java
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
public class GaussDistribution implements UnivariateRealFunction {

	private final double sigma;
	
	private final double mu;
	
	private final double scale;
	
	public GaussDistribution(double sigma, double mu, double scale) {
		this.sigma = sigma;
		this.mu = mu;
		this.scale = scale;
	}
	
	@Override
	public double value(double x) throws FunctionEvaluationException {
		return scale/(sigma * Math.sqrt(2 * Math.PI)) * Math.exp(-0.5 * Math.pow((x - mu)/sigma, 2.0));
	}

}
