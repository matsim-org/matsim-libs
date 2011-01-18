/* *********************************************************************** *
 * project: org.matsim.*
 * PointUtils.java
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
package playground.johannes.socialnetworks.gis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author illenberger
 * 
 */
public class PointUtils {

	private static final GeometryFactory geometryFactory = new GeometryFactory();

	public static Envelope envelope(Set<Point> points) {
		double xmin = Double.MAX_VALUE;
		double xmax = Double.MIN_VALUE;
		double ymin = Double.MAX_VALUE;
		double ymax = Double.MIN_VALUE;

		for (Point point : points) {
			xmin = Math.min(xmin, point.getX());
			xmax = Math.max(xmax, point.getX());
			ymin = Math.min(ymin, point.getY());
			ymax = Math.max(ymax, point.getY());
		}

		return new Envelope(xmin, xmax, ymin, ymax);
	}

	public static Point centerOfMass(Set<Point> points) {
		double xsum = 0;
		double ysum = 0;

		for (Point point : points) {
			xsum += point.getX();
			ysum += point.getY();
		}

		double n = points.size();
		return geometryFactory.createPoint(new Coordinate(xsum / n, ysum / n));
	}

	public static double avrDistance(Set<Point> points, DistanceCalculator calculator) {
		List<Point> pointList = new ArrayList<Point>(points);

		if (points.size() < 2)
			return Double.NaN;

		int n = pointList.size();
		double sum = 0;

		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				sum += calculator.distance(pointList.get(i), pointList.get(j));
			}
		}

		return 2 * sum / (double) n * (n - 1);
	}
}
