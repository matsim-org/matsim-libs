/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.mid;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;

/**
 * @author johannes
 */
public class Route2GeoDistFunction implements UnivariateRealFunction {

    private double min = 0.5;

    private double A = 4;

    private double alpha = -0.5;

    public Route2GeoDistFunction(double A, double alpha, double min) {
        this.A = A;
        this.alpha = alpha;
        this.min = min;
    }

    @Override
    public double value(double x) throws FunctionEvaluationException {
        double routDist = x/1000.0;
        double factor = 1 - A * Math.pow(routDist, alpha);
        factor = Math.max(min, factor);
        return routDist * factor * 1000.0;
    }
}
