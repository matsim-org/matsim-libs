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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class AsciiFromShp {



	public static void main(String [] args) throws IOException {
		String out = "/Users/laemmel/svn/shared-svn/projects/120multiDestPeds/floor_plan/boundaries_closed_transformed.shp";
		//		String out = "/Users/laemmel/tmp/hjsk.shp";

		List<SimpleFeature> fts = new ArrayList<SimpleFeature>();
		System.out.println("#linenr,typ,x,y");
		int i = 0;
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(out)) {
			fts.add(ft);
			LineString ls = (LineString)((MultiLineString)ft.getDefaultGeometry()).getGeometryN(0);
			for (int j = 0; j < ls.getNumPoints(); j++) {
				System.out.println(i +"," + ft.getAttribute("lines") + "," + ls.getCoordinateN(j).x + "," + ls.getCoordinateN(j).y);

			}
			i++;

		}
		GeometryFactory gf = new GeometryFactory();
		LineString ls = gf.createLineString(new Coordinate[]{new Coordinate(1,21),new Coordinate(13,23)});
		MultiLineString ml = gf.createMultiLineString(new LineString[]{ls});
		
		PolylineFeatureFactory factory = initFeatures();
		
		
		SimpleFeature lsFeature = factory.createPolyline(ml, new Object[] {"wand"}, null);
		fts.add(lsFeature);
		
		ShapeFileWriter.writeGeometries(fts, "/Users/laemmel/tmp/!!!ftsTest.shp");
		
	}
	
	private static PolylineFeatureFactory initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS("EPSG: 32632");
		return new PolylineFeatureFactory.Builder().
				setCrs(targetCRS).
				setName("Boundary").
				addAttribute("name", String.class).
				create();
	}
}
