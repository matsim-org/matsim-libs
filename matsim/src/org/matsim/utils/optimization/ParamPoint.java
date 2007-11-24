/* *********************************************************************** *
 * project: org.matsim.*
 * ParamPoint.java
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

package org.matsim.utils.optimization;

import org.matsim.gbl.Gbl;

/**
 * An implementation of a multidimensional point. The dimension can be freely 
 * chosen when instantiating an object, but cannot be altered afterwards.
 * Divison is not supplied, use multiply with factor `1 / divisor' instead ;-)
 */
public class ParamPoint {
	final private int dimension_;
	final private double[] values_;
	
	public ParamPoint(int dimension) {
		dimension_ = dimension;
		values_ = new double[dimension_];
	}
	
	public int getDimension() {
		return dimension_;
	}
	
	public void setValue(int idx, double value) {
		values_[idx] = value;
	}

	public double getValue(int idx) {
		return values_[idx];
	}

	public static ParamPoint add(ParamPoint p1, ParamPoint p2) {
		int dimension = p1.getDimension();
		if (p2.getDimension() != dimension) {
			Gbl.errorMsg("p1 and p2 have different dimensions.");
		}
		ParamPoint result = new ParamPoint(dimension);
		for (int i = 0; i < dimension; i++) {
			double value = p1.getValue(i) + p2.getValue(i);
			result.setValue(i, value);
		}
		return result;
	}
	
	public static ParamPoint subtract(ParamPoint p1, ParamPoint p2) {
		int dimension = p1.getDimension();
		if (p2.getDimension() != dimension) {
			Gbl.errorMsg("p1 and p2 have different dimensions.");
		}
		ParamPoint result = new ParamPoint(dimension);
		for (int i = 0; i < dimension; i++) {
			double value = p1.getValue(i) - p2.getValue(i);
			result.setValue(i, value);
		}
		return result;
	}
	
	public static ParamPoint multiply(ParamPoint p1, double factor) {
		int dimension = p1.getDimension();
		ParamPoint result = new ParamPoint(dimension);
		for (int i = 0; i < dimension; i++) {
			double value = p1.getValue(i) * factor;
			result.setValue(i, value);
		}
		return result;
	}
	
	@Override
	public String toString() {
		String result = "[dimension="+dimension_+"][values={"+values_[0];
		for (int i = 1; i < dimension_; i++) {
			result = result + "," + values_[i];
		}
		result = result + "}]";
		return result;
	}

}
