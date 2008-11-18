/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.dgrether.functions;


/**
 * @author dgrether
 *
 */
public class Function {


	public double[] computeXValues(double from, double to) {
		int size = 5;
		double[] ret = new double[size];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = i;
		}

		return ret;
	}


	public double[] computeYValues(double from, double to) {
		int size = 5;
		double[] ret = new double[size];

		for (int i = 0; i < ret.length; i++) {
			ret[i] = i;
		}

		return ret;
	}

	public String getTitle() {
		return "FunctionTitle";
	}


}
