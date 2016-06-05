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
}


	