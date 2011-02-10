/* *********************************************************************** *
 * project: org.matsim.*
 * SimpleTrigonometric.java
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
package playground.yu.utils.math;

import org.matsim.api.core.v01.Coord;

/**
 * @author yu
 * 
 */
public class SimpleTrigonometric {
	public static double getCosineCFrom3Sides(double a, double b, double c) {
		return (a * a + b * b - c * c) / (2 * a * b);
	}

	/**
	 * @param A
	 *            angle A
	 * @param B
	 *            angle B
	 * @param C
	 *            angle C
	 * @return if side a or b with length 0, returns 1 (cos(0)).
	 */
	public static double getCosineCFrom3Coords(Coord A, Coord B, Coord C) {
		double xA = A.getX(), yA = A.getY()//
		, xB = B.getX(), yB = B.getY()//
		, xC = C.getX(), yC = C.getY();

		double aLengthSquare = (xC - xB) * (xC - xB) + (yC - yB) * (yC - yB)//
		, bLengthSquare = (xC - xA) * (xC - xA) + (yC - yA) * (yC - yA)//
		, cLengthSquare = (xA - xB) * (xA - xB) + (yA - yB) * (yA - yB);

		if (aLengthSquare == 0d || bLengthSquare == 0d) {
			return 1d;
		}
		return (aLengthSquare + bLengthSquare - cLengthSquare)
				/ (2d * Math.sqrt(aLengthSquare * bLengthSquare));
	}
}
