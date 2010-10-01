/* *********************************************************************** *
 * project: org.matsim.*
 * Polynomial.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.wrashid.lib.obj.math;

public class Polynomial {

	private double[] coefficients;
	
	/**
	 * first coefficient is for x^0, then x^1, etc.
	 * @param coefficients
	 */	
	public Polynomial(double[] coefficients) {
		this.coefficients = coefficients;
	}

	public double evaluate(double xValue){
		double result=0;
		
		for (int i=0;i<coefficients.length;i++){
			result+=coefficients[i]*Math.pow(xValue, i) ;
		}
		
		return result;
	}
	
}
