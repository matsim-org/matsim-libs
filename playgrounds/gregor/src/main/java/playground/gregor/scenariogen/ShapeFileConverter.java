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

package playground.gregor.scenariogen;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import playground.gregor.multidestpeds.helper.WGS86UTM33N2MathBuildingTransformation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class ShapeFileConverter {

	public static void main(String [] args) throws IOException {
		String input = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed.shp";
		String output = "/Users/laemmel/devel/gr90/input/floorplan.shp";
		List<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(input)) {
			Geometry g = (Geometry) ft.getDefaultGeometry();
			Coordinate[] coords = g.getCoordinates();
			for (Coordinate coord : coords) {
				Coordinate c = coord;
				Coordinate c2 = WGS86UTM33N2MathBuildingTransformation.transform(c);
				c.x = c2.x;
				c.y = c2.y;
				c.z = 0;
			}
			fts.add(ft);
		}

		ShapeFileWriter.writeGeometries(fts, output);

	}


}
