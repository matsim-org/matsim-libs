/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayTools.java
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

/**
 * 
 */
package playground.yu.utils;

/**
 * @author yu
 * 
 */
public class ArrayTools {
	/**
	 * @param array
	 *            an int array with only 2 elements,array[0]<=array[1]
	 * @param number
	 * @return
	 */
	public static boolean inRange(int[] array, int number) {
		int linkBoundary = array[0], rightBoundary = array[1];
		return number >= linkBoundary && number <= rightBoundary;
	}
}
