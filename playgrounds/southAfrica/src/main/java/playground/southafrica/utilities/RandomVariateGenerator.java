/* *********************************************************************** *
 * project: org.matsim.*
 * RandomVariateGenerator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.southafrica.utilities;

public class RandomVariateGenerator {

	/**
	 * Generates a triangular-distributed random variate. It works for special
	 * cases too where the triangle is right-angled, either left-tailed or 
	 * right-tailed.  
	 * @param low lowest possible value;
	 * @param mid most likely/probable value;
	 * @param high highest possible value;
	 * @return
	 * @see <a href="http://en.wikipedia.org/wiki/Triangular_distribution#Generating_Triangular-distributed_random_variates">Wikipedia</a>
	 */
	public static double getTriangular(double low, double mid, double high){
		double r = Math.random();
		double income = 0;
		if(r > 0 && r < (mid-low)/(high-low)){
			income = low + Math.sqrt(r*(high-low)*(mid-low));
		} else if(r >= (mid-low)/(high-low)){
			income = high - Math.sqrt((1-r)*(high-low)*(high-mid));
		}
		return income;
	}
	
	
	/**
	 * Generates an exponentially-distributed random variate.
	 * @param lambda the rate parameter
	 * @return
	 * @see <a href="http://en.wikipedia.org/wiki/Exponential_distribution#Generating_exponential_variates">Wikipedia</a>
	 */
	public static double getExponential(double lambda){
		return -Math.log(Math.random()) / lambda;
	}
	
	
	/**
	 * Generates a random variate that has a Weibull distribution with the
	 * given parameters. (Source: <a href="http://users.ecs.soton.ac.uk/jn2/simulation/random.html"> 
	 * Jason Noble, University of Southampton</a>)
	 * @param shape 
	 * @param scale
	 * @return
	 * @see <a href="http://en.wikipedia.org/wiki/Weibull_distribution">Wikipedia</a>
	 */
	public static double getWeibull(double shape, double scale){
		return scale*Math.pow( -Math.log(Math.random()) , 1/shape);
	}
}

