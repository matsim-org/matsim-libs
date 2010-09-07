/* *********************************************************************** *
 * project: org.matsim.*
 * PolygonWHoles.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.gregor.gis.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.geotools.feature.IllegalAttributeException;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

public class PolygonWHoles {
	private static final double minX = 647500;
	private static final double maxX = 657500;
	private static final double minY = 9890000;
	private static final double maxY = 9905000;
	
	public static void main(String [] args) {
		String in = "/home/laemmel/arbeit/diss/qgis/run789_lostAgents.shp";
		String out = "/home/laemmel/arbeit/diss/qgis/run789_lostAgents_inv.shp";
		FeatureSource fs = null;
		try {
			fs = ShapeFileReader.readDataFile(in);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Feature ft  = null;
		try {
			ft = (Feature) fs.getFeatures().iterator().next();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Polygon oldP = (Polygon)((MultiPolygon) ft.getDefaultGeometry()).getGeometryN(0);
		
		GeometryFactory geofac = new GeometryFactory();
		Coordinate c1 = new Coordinate(minX, minY);
		Coordinate c2 = new Coordinate(minX, maxY);
		Coordinate c3 = new Coordinate(maxX, maxY);
		Coordinate c4 = new Coordinate(maxX, minY);
		Coordinate [] hull = new Coordinate[] {c1,c2,c3,c4,c1};
		LinearRing lr = geofac.createLinearRing(hull);
		Polygon p = geofac.createPolygon(lr, new LinearRing[] {(LinearRing)oldP.getExteriorRing()});
		MultiPolygon mp = geofac.createMultiPolygon(new Polygon[]{p});
		Collection<Feature> fts = new ArrayList<Feature>();
		try {
			fts.add(fs.getSchema().create(new Object[]{mp,"run789_lostAgents"}));
		} catch (IllegalAttributeException e) {
			e.printStackTrace();
		}
		try {
			ShapeFileWriter.writeGeometries(fts, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
}
