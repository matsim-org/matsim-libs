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

package org.matsim.contrib.zone.util;

import java.util.function.Predicate;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.contrib.common.util.random.RandomUtils;
import org.matsim.contrib.common.util.random.UniformRandom;
import org.matsim.core.utils.geometry.geotools.MGC;

public class RandomPointUtils {
	public static Point getRandomPointInGeometry(final Geometry geometry) {
		return getRandomPointInEnvelope(geometry::contains, geometry.getEnvelopeInternal());
	}

	public static Point getRandomPointInGeometry(final PreparedGeometry geometry) {
		return getRandomPointInEnvelope(geometry::contains, geometry.getGeometry().getEnvelopeInternal());
	}

	private static Point getRandomPointInEnvelope(Predicate<Point> contains, Envelope envelope) {
		UniformRandom uniform = RandomUtils.getGlobalUniform();
		double minX = envelope.getMinX();
		double maxX = envelope.getMaxX();
		double minY = envelope.getMinY();
		double maxY = envelope.getMaxY();

		Point p;
		do {
			double x = uniform.nextDouble(minX, maxX);
			double y = uniform.nextDouble(minY, maxY);
			p = MGC.xy2Point(x, y);
		} while (!contains.test(p));

		return p;
	}
}
