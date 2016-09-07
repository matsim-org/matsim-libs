/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.utils;

import java.util.Arrays;

/**
* @author amit
*/

public class NumberUtils {
	
	/**
	 * Well, this is the fastest (I think) method with little bit compromise on accuracy in some cases.
	 * There exists some other methods using BigDecimal, deciamlFormats, which I think, for me not so necessary. 
	 */
	public static double round(double number, int decimalPlace){
		double multiplier = Math.pow(10, decimalPlace);
		return Math.round(number * multiplier) / multiplier;
	}
	
	/**
	 * Taken from http://www.java2s.com/Code/Java/Collections-Data-Structure/Retrivethequartilevaluefromanarray.htm
	 * @param values the array of data
	 * @param lowerPercent lowerPercent The percent cut off. For the lower quartile use 25,
     *      for the upper-quartile use 75
	 * @return the quartile value from an array
	 */
	public static double quartile(double[] values, double lowerPercent) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("The data array either is null or does not contain any data.");
        }

        // Rank order the values
        double[] v = new double[values.length];
        System.arraycopy(values, 0, v, 0, values.length);
        Arrays.sort(v);
        int n = (int) Math.round(v.length * lowerPercent / 100);
        return v[n];
    }
}


	