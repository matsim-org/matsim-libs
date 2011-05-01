/* *********************************************************************** *
 * project: org.matsim.*
 * CollectionMax.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.yu.utils.container;

import java.util.Collection;

/**
 * @author yu
 *
 */
public class CollectionMax {
	public static double getDoubleMax(Collection<Double> collection) {
		double max = Double.NEGATIVE_INFINITY;
		for (Double d : collection) {
			if (d > max) {
				max = d;
			}
		}
		return max;
	}

}
