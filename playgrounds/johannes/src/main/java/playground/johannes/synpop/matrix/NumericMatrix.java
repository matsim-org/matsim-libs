/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.synpop.matrix;


import java.util.Collection;
import java.util.Set;

/**
 * @author johannes
 * 
 */
public class NumericMatrix implements Matrix<String, Double> {

	private final Matrix<String, Double> matrix;

	public NumericMatrix() {
		matrix = new HashMatrix<>();
	}

	public Double add(String key1, String key2, double value) {
		Double val = matrix.get(key1, key2);
		if(val == null) {
			return matrix.set(key1, key2, value);
		} else {
			return matrix.set(key1, key2, val + value);
		}
	}

	public void multiply(String i, String j, double factor) {
		Double val = matrix.get(i, j);
		if(val != null) {
			matrix.set(i, j, val * factor);
		}
	}

    @Override
    public Double set(String row, String column, Double value) {
        return matrix.set(row, column, value);
    }

    @Override
    public Double get(String row, String column) {
        return matrix.get(row, column);
    }

    @Override
    public Set<String> keys() {
        return matrix.keys();
    }

    @Override
    public Collection<Double> values() {
        return matrix.values();
    }
}
