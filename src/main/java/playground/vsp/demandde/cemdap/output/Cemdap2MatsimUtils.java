/* *********************************************************************** *
 * project: org.matsim.*                                                   *
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

package playground.vsp.demandde.cemdap.output;

import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author dziemke
 */
public class Cemdap2MatsimUtils {
	public final static Random random = MatsimRandom.getRandom();
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	public static final Coord getRandomCoordinate(SimpleFeature feature) {
		Geometry geometry = (Geometry) feature.getDefaultGeometry();
		Envelope envelope = geometry.getEnvelopeInternal();
		while (true) {
			Point point = getRandomCoordinate(envelope);
			if (point.within(geometry)) {
				return new Coord(point.getX(), point.getY());
			}
		}
	}
	
	public static final Point getRandomCoordinate(Envelope envelope) {
		double x = envelope.getMinX() + random.nextDouble() * envelope.getWidth();
		double y = envelope.getMinY() + random.nextDouble() * envelope.getHeight();
		return geometryFactory.createPoint(new Coordinate(x,y));
	}
	
	/**
	 * CEMDAP handles all IDs incl. zone IDs internally as integers. Casting IDs as integers removes leading zeroes.
	 * In order to be consistent and enable zones to be found, zone IDs need to have leading zeros removed.
	 */
	public static final String removeLeadingZeroFromString(String input) {
		Integer zoneIdAsInt = Integer.parseInt(input); 
		return zoneIdAsInt.toString();
	}
}