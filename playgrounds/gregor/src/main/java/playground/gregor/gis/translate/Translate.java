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

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;

public class Translate {
	
//	private final static double targetX = 848380;
//	private final static double targetY = 5921030;
	
	//sx:-848508.6355312001 sy:-5920911.431172118
	
	public static void main(String [] args) {
		String input = "/Users/laemmel/devel/burgdorf2d/raw_input/raw_env.shp";
		String output ="/Users/laemmel/devel/burgdorf2d/tmp/offset_raw_env.shp";
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(input);
		double offsetX = reader.getBounds().getMinX();
		double offsetY = reader.getBounds().getMinY();
		double sx = -offsetX;
		double sy = -offsetY;
		System.out.println("sx:" + sx + " sy:" + sy);
		
		Set<Coordinate> handled = new HashSet<Coordinate>();
		
		for (Feature ft : reader.getFeatureSet()) {
			for (Coordinate c : ft.getDefaultGeometry().getCoordinates()) {
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
