/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.vsp.demandde.pendlermatrix;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.GeoFileReader;


public class Verschmierer {

	private String filename;

	private Map<Integer, Geometry> zones = new HashMap<Integer, Geometry>();

	private Random random = new Random();

	public Verschmierer(String filename) {
		this.filename = filename;
		readShape();
	}

	private void readShape() {
		for (SimpleFeature landkreis : GeoFileReader.getAllFeatures(filename)) {
			Integer gemeindeschluessel = Integer.parseInt((String) landkreis.getAttribute("gemeindesc"));
			zones.put(gemeindeschluessel, (Geometry) landkreis.getDefaultGeometry());
		}
	}

	private Geometry findZone(Coord coord) {
		GeometryFactory gf = new GeometryFactory();
		Point point = gf.createPoint(new Coordinate(coord.getX(), coord.getY()));
		for (Geometry zone : zones.values()) {
			if (zone.contains(point)) {
				return zone;
			}
		}
		return null;
	}

	public Coord shootIntoSameZoneOrLeaveInPlace(Coord coord) {
		Geometry zone = findZone(coord);
		if (zone != null) {
			return doShoot(zone);
		} else {
			return coord;
		}
	}

	private static Point getRandomPointInFeature(Random rnd, Geometry g) {
		Point p = null;
		double x, y;
		do {
			x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
			y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble() * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
			p = MGC.xy2Point(x, y);
		} while (!g.contains(p));
		return p;
	}

	private Coord doShoot(Geometry zone) {
		Coord coord;
		Point point = getRandomPointInFeature(this.random , zone);
		coord = new Coord(point.getX(), point.getY());
		return coord;
	}


}
