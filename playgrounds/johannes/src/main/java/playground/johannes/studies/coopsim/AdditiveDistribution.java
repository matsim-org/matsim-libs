/* *********************************************************************** *
 * project: org.matsim.*
 * AdditiveDistribution.java
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
package playground.johannes.studies.coopsim;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.matsim.contrib.sna.util.Composite;

/**
 * @author illenberger
 *
 */
public class AdditiveDistribution extends Composite<UnivariateRealFunction> implements UnivariateRealFunction {

	@Override
	public double value(double x) throws FunctionEvaluationException {
		double r = 0;
		for(UnivariateRealFunction func : components)
			r += func.value(x);
		
		return r;
	}

}
