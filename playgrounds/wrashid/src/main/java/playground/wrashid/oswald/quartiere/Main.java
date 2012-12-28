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

package playground.wrashid.oswald.quartiere;

import java.awt.Polygon;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Main {

	public static void main(String[] args) {
		String file = "C:/ETHZEclipseData/static data/parking/zürich city/stadtquartiere/Zurich.shp";
		Polygon polygon = null;
		try {

			for (SimpleFeature feature : ShapeFileReader.getAllFeatures(file)) {
				polygon = new Polygon();

				Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();

				// System.out.println(feature.getFeatureType());
				// System.out.println(feature.toString());
				// System.out.println(feature.getID());
				// System.out.println(feature.getAttribute("NAME"));

				Coordinate[] coordinates = sourceGeometry.getCoordinates();
				for (int i = 0; i < coordinates.length; i++) {
					polygon.addPoint((int) Math.round(coordinates[i].x), (int) Math.round(coordinates[i].y));
				}

				// Coordinate[] coordinates =
				// sourceGeometry.getCoordinates();
				// System.out.println("f: " + coordinates[0]);
				// System.out.println("l: " +
				// coordinates[coordinates.length-1]);

				if (polygon.contains(685340, 245700)) {
					System.out.println(feature.getAttribute("NAME"));
				}

			}

		} catch (Throwable e) {
			e.printStackTrace();

		}

	}

}
