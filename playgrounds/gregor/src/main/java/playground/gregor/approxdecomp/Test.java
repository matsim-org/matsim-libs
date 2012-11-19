/* *********************************************************************** *
 * project: org.matsim.*
 * Test.java
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

package playground.gregor.approxdecomp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.geotools.feature.Feature;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;

public class Test {
	
	

	
	public static void main(String [] args) {
//		String ref = "6 6 6 6 4 6 4 5 5 5 5 4 5 7 4 5 4 7 5 6 5";
		String p = "/Users/laemmel/devel/convexdecomp/polygon03_.shp";
//		String p = "/Users/laemmel/tmp/dump.shp";
//		DecompGuiDebugger dbg = new DecompGuiDebugger();
//		dbg.setVisible(true);
		
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(p);
		
		ApproxConvexDecomposer decomp = new ApproxConvexDecomposer();
		
//		com.vividsolutions.jts.algorithm.CGAlgorithms.isCCW(null)
		
		
		Set<Feature> geos = reader.getFeatureSet();
		Collection<Polygon> decomposed = new ArrayList<Polygon>();
		for (Feature ft : geos) {
			Geometry geo = ft.getDefaultGeometry();
	
			decomposed.addAll(decomp.decompose(geo));
		}
int nr = 0;		
		for (Polygon dec : decomposed) {
			GisDebugger.addGeometry(dec);
			nr += dec.getNumPoints();
		}
		System.out.println(decomposed.size() + " == 105?");
		GisDebugger.dump("/Users/laemmel/devel/convexdecomp/resolved.shp");
		
	}

}
