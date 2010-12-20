/* *********************************************************************** *
 * project: org.matsim.*
 * MathLib.java
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

package playground.wrashid.lib;

public class MathLib {

	/**
	 *  If the difference is less than epsilon, treat as equal.
	 * @param firstDouble
	 * @param secondDouble
	 * @param epsilon
	 * @return
	 */
	public static boolean equals(double firstDouble, double secondDouble, double epsilon) {
	    if (firstDouble==secondDouble) return true;
	    return Math.abs(firstDouble - secondDouble) < epsilon;
	}
	
}
