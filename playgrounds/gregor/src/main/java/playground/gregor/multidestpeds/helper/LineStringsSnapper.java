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

package playground.gregor.multidestpeds.helper;

import java.util.Collection;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

public class LineStringsSnapper {

	private static final double SNAP = 0.1;

	public static void main(String[] args) {
		String in = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries.shp";
		String out = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed.shp";

		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(in);
		QuadTree<Coordinate> quad = new QuadTree<Coordinate>(reader.getBounds().getMinX(),reader.getBounds().getMinY(),reader.getBounds().getMaxX(),reader.getBounds().getMaxY());

		for (SimpleFeature ft : reader.getFeatureSet()) {
			MultiLineString ml = (MultiLineString) ft.getDefaultGeometry();
			for (int i = 0; i < ml.getNumGeometries(); i++) {
				LineString ls = (LineString) ml.getGeometryN(i);
				Point start = ls.getStartPoint();
				checkIt(start,quad);


				Point end = ls.getEndPoint();
				checkIt(end,quad);
			}
		}
		ShapeFileWriter.writeGeometries(reader.getFeatureSet(), out);
	}

	private static void checkIt(Point start, QuadTree<Coordinate> quad) {
		Collection<Coordinate> col = quad.get(start.getX(), start.getY(), SNAP);
		if (col.size() == 0) {
			quad.put(start.getX(), start.getY(), start.getCoordinate());
		} else if (col.size() == 1) {
			Coordinate other = col.iterator().next();
			start.getCoordinate().setCoordinate(other);
			System.out.println("SNAP, SNAP, SNAP...");
		} else {
			throw new RuntimeException("can not happen!");
		}

	}

}
