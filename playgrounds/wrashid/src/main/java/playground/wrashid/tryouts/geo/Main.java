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

package playground.wrashid.tryouts.geo;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class Main {

	public static void main(String[] args) {
		String file = "P:/Daten/GIS_Daten/ArcView/PLZ_Layers/PLZ_Reg_region.shp";

		// GeometryFactory.createPointFromInternalCoord(coord, exemplar)

		// WKTParser parser = new WKTParser( new Geome DefaultGeographicCRS.WGS84 );
		// Point point = (Point) parser.parse("POINT( 48.44 -123.37)");
		Point p = MGC.coord2Point(new Coord(744120.000135, 234919.999845));

		for (SimpleFeature feature : ShapeFileReader.getAllFeatures(file)) {
			Geometry sourceGeometry = (Geometry) feature.getDefaultGeometry();

			if (!sourceGeometry.disjoint(p)){
				System.out.println(sourceGeometry.getCoordinate());
			}

		}

	}

}
