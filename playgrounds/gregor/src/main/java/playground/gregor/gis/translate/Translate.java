/* *********************************************************************** *
 * project: org.matsim.*
 * Translate.java
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

package playground.gregor.gis.translate;

import java.util.HashSet;
import java.util.Set;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class Translate {
	
	private final static double targetY = 7075588;
	private final static double targetX = 1113551;
	
	//sx:-848508.6355312001 sy:-5920911.431172118
	
	public static void main(String [] args) {
		String input = "/Users/laemmel/devel/sim2dDemoIII/env_gen/floorplan.shp";
		String output ="/Users/laemmel/devel/sim2dDemoIII/env_gen/floorplan_offset.shp";
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(input);
		double offsetX = reader.getBounds().getMinX();
		double offsetY = reader.getBounds().getMinY();
		double sx = -offsetX + targetX;
		double sy = -offsetY + targetY;
		System.out.println("sx:" + sx + " sy:" + sy);
		
		Set<Coordinate> handled = new HashSet<Coordinate>();
		
		for (SimpleFeature ft : reader.getFeatureSet()) {
			for (Coordinate c : ((Geometry) ft.getDefaultGeometry()).getCoordinates()) {
				if (handled.contains(c)) {
					System.out.println("hab schon!");
					continue;
				}
				c.x += sx;
				c.y += sy;
				handled.add(c);
				
			}
		}
		ShapeFileWriter.writeGeometries(reader.getFeatureSet(), output);
		
	}

}
