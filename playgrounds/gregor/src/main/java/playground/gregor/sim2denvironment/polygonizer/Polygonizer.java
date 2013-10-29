/* *********************************************************************** *
 * project: org.matsim.*
 * Polygonizer.java
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

package playground.gregor.sim2denvironment.polygonizer;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.gregor.sim2denvironment.GisDebugger;

import com.vividsolutions.jts.geom.Geometry;

public class Polygonizer {
	
	public static void main(String args[]) {
		String input = "/Users/laemmel/devel/gct2/floorpl/42nd_north_east.shp";
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(input);
		Geometry res = null;
		for (SimpleFeature ft : reader.getFeatureSet()) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			if (res == null) {
				res = geo;
			} else {
				res = res.union(geo);
			}
		}
		Geometry res2 = res.buffer(.05,1);
		
		Geometry e = res.getEnvelope();
		
		Geometry e2 = e.difference(res2);
		GisDebugger.setCRSString("EPSG:3395");
		GisDebugger.addGeometry(e2);
		
		GisDebugger.dump("/Users/laemmel/devel/gct2/floorpl/42nd_north_east_p.shp");
		
		System.out.println(res);
		
	}

}
