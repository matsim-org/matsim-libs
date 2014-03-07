/* *********************************************************************** *
 * project: org.matsim.*
 * ScaleEndPointUtil
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.vis.vecmathutils;

import java.awt.geom.Point2D;

import org.matsim.core.utils.collections.Tuple;

/**
 * Util class containing stateless vecmath utility methods frequently needed for visualization code.
 * @author dgrether
 * @author tthunig
 * 
 */
public class VectorUtils {

	/**
	 * Scales a vector given by start and end point by a factor. Changes both - start and end coordinate of the vector.
	 * @param scaleFactor a scale factor in [0..1]
	 * @return a tuple with the scaled start point as first and the end point as
	 *         second entry
	 */
	public static Tuple<Point2D.Double, Point2D.Double> scaleVector(final Point2D.Double start, final Point2D.Double end,
			double scaleFactor) {
		
		return calculateScaledVector(start, end, scaleFactor);
	}

	/**
	 * Scales a vector given by start and end point by a factor. Changes only the start coordinate of the vector.
	 * @param scaleFactor a scale factor in [0..1]
	 * @return a tuple with the scaled start point as first and the old end point as
	 *         second entry
	 * @deprecated not used yet
	 */
	@Deprecated
	public static Tuple<Point2D.Double, Point2D.Double> scaleVectorStart(final Point2D.Double start, final Point2D.Double end,
			double scaleFactor) {
		
		Tuple<Point2D.Double, Point2D.Double> scaledVector = calculateScaledVector(start, end, scaleFactor);
		return new Tuple<Point2D.Double, Point2D.Double>(scaledVector.getFirst(), end);
	}
	
	/**
	 * Scales a vector given by start and end point by a factor. Changes only the end coordinate of the vector.
	 * @param scaleFactor a scale factor in [0..1]
	 * @return a tuple with the old start point as first and the scaled end point as
	 *         second entry
	 * @deprecated not used yet
	 */
	@Deprecated
	public static Tuple<Point2D.Double, Point2D.Double> scaleVectorEnd(final Point2D.Double start, final Point2D.Double end,
			double scaleFactor) {
		
		Tuple<Point2D.Double, Point2D.Double> scaledVector = calculateScaledVector(start, end, scaleFactor);
		return new Tuple<Point2D.Double, Point2D.Double>(start, scaledVector.getSecond());
	}
	
	
	private static Tuple<Point2D.Double, Point2D.Double> calculateScaledVector(
			final Point2D.Double start, final Point2D.Double end,
			double scaleFactor) {
		//norm
		Point2D.Double.Double delta = new Point2D.Double.Double(end.x - start.x, end.y - start.y);
		double length = Math.sqrt(Math.pow(delta.x, 2) + Math.pow(delta.y, 2));
		Point2D.Double.Double deltaNorm = new Point2D.Double.Double(delta.x / length, delta.y / length);
		//scale
		double scaledLength = length * scaleFactor;
		double offset = (length - scaledLength) / 2;
		Point2D.Double scaledEnd = new Point2D.Double(start.x + ((length - offset) * deltaNorm.x), start.y
				+ ((length - offset) * deltaNorm.y));
		Point2D.Double scaledStart = new Point2D.Double(start.x
				+ (offset * deltaNorm.x), start.y
				+ (offset * deltaNorm.y));
		Tuple<Point2D.Double, Point2D.Double> scaledVector = new Tuple<Point2D.Double, Point2D.Double>(scaledStart, scaledEnd);
		return scaledVector;
	}
}
