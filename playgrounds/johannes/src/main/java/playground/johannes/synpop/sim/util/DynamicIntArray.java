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
public class DynamicIntArray {

    public final int naValue;

    private int[] array;

    public DynamicIntArray() {
        naValue = -Integer.MAX_VALUE;
        array = new int[12];
        Arrays.fill(array, naValue);
    }


    public DynamicIntArray(int size, int naValue) {
        this.naValue = naValue;
        array = new int[size];
        Arrays.fill(array, naValue);
    }

    public void set(int index, int value) {
        if(checkBounds(index)) {
            array[index] = value;
        } else {
            int newLength = index + 1;
            int[] copy = new int[newLength];
            Arrays.fill(copy, naValue);
            System.arraycopy(array, 0, copy, 0, Math.min(array.length, newLength));
            array = copy;
            array[index] = value;
        }
    }

    public int get(int index) {
        if(checkBounds(index)) return array[index];
        else return naValue;
    }

    private boolean checkBounds(int index) {
        return array.length > index;
    }
}
