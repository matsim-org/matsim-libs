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

package playground.johannes.synpop.sim.util;

import java.util.Arrays;

/**
 * @author johannes
 */
public class DynamicDoubleArray {

    public final double naValue;

    private double[] array;

    public DynamicDoubleArray() {
        naValue = Double.NaN;
        array = new double[12];
        Arrays.fill(array, naValue);
    }


    public DynamicDoubleArray(int size, double naValue) {
        this.naValue = naValue;
        array = new double[size];
        Arrays.fill(array, naValue);
    }

    public void set(int index, double value) {
        if(checkBounds(index)) {
            array[index] = value;
        } else {
            int newLength = index + 1;
            double[] copy = new double[newLength];
            Arrays.fill(copy, naValue);
            System.arraycopy(array, 0, copy, 0, Math.min(array.length, newLength));
            array = copy;
            array[index] = value;
        }
    }

    public double get(int index) {
        if(checkBounds(index)) return array[index];
        else return naValue;
    }

    private boolean checkBounds(int index) {
        return array.length > index;
    }

    public int size() {
        return array.length;
    }
}
