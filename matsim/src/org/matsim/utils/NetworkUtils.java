/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkUtils.java
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

package org.matsim.utils;

import java.util.Collection;

import org.matsim.network.Node;

public class NetworkUtils {

	public static double[] getBoundingBox(Collection<Node> nodes) {
		double[] bBox = new double[4];
		bBox[0] = Double.MIN_VALUE;
		bBox[1] = Double.MAX_VALUE;
		bBox[2] = Double.MIN_VALUE;
		bBox[3] = Double.MAX_VALUE;

		for (Node n : nodes) {
			if (n.getCoord().getX() > bBox[0]) {
				bBox[0] = n.getCoord().getX();
			}
			if (n.getCoord().getX() < bBox[1]) {
				bBox[1] = n.getCoord().getX();
			}
			if (n.getCoord().getY() > bBox[2]) {
				bBox[2] = n.getCoord().getY();
			}
			if (n.getCoord().getY() < bBox[3]) {
				bBox[3] = n.getCoord().getY();
			}
		}

		return bBox;
	}

}
