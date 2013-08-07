/* *********************************************************************** *
 * project: org.matsim.*
 * GisDebugger.java
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
package playground.gregor.sim2denvironment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PointFeatureFactory;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GisDebugger {

	private static PolygonFeatureFactory polygonFactory;
	private static PolylineFeatureFactory polylineFactory;
	private static PointFeatureFactory pointFactory;

	private static List<Geometry> geos = new ArrayList<Geometry>();

	private static List<String> strs = new ArrayList<String>();

	private static boolean init = false;

	private static String CRS = "EPSG: 3395";

	public static final GeometryFactory geofac = new GeometryFactory();

	public static void setCRSString(String crs) {
		CRS = crs;
	}
	
	public static void addGeometry(Geometry geo) {
		geos.add(geo);
	}

	public static void addGeometry(Geometry geo, String str) {
		addGeometry(geo);
		strs.add(str);

	}

	public static void dump(String file) {
		if (!init) {
			initFeatures();
			init = true;
		}
		Collection<SimpleFeature> fts = new  ArrayList<SimpleFeature>();
		double d = 0;
		Iterator<String> it = null;
		if (strs.size() == geos.size()) {
			it = strs.iterator();
		}
		for (Geometry geo : geos) {
//			Algorithms.translate(5, 20, geo.getCoordinates());          
			String str = "";
			if (it != null) {
				str = it.next();
			}
			Object[] attributes = new Object[] {d++, str};
			if (geo instanceof MultiPolygon) {
				fts.add(polygonFactory.createPolygon((MultiPolygon) geo, attributes, null));
			} else if (geo instanceof Polygon) {
				fts.add(polygonFactory.createPolygon((Polygon) geo, attributes, null));
			} else if (geo instanceof LineString) {
				fts.add(polylineFactory.createPolyline((LineString) geo, attributes, null));
			} else if (geo instanceof Point) {
				fts.add(pointFactory.createPoint((Point) geo, attributes, null));
			} else {
				throw new RuntimeException("type of Geometry is not supported" + geo);
			}

		}
		ShapeFileWriter.writeGeometries(fts, file);
		geos.clear();
		strs.clear();
	}


	private static void initFeatures() {
		CoordinateReferenceSystem targetCRS = MGC.getCRS(CRS);
		
		polygonFactory = new PolygonFeatureFactory.Builder().
				setCrs(targetCRS).
				addAttribute("dblAvgZ", Double.class).
				addAttribute("name", String.class).
				create();
		
		polylineFactory = new PolylineFeatureFactory.Builder().
				setCrs(targetCRS).
				addAttribute("dblAvgZ", Double.class).
				addAttribute("name", String.class).
				create();
		
		pointFactory = new PointFeatureFactory.Builder().
				setCrs(targetCRS).
				addAttribute("dblAvgZ", Double.class).
				addAttribute("name", String.class).
				create();
	}

	public static void addCircle(Coordinate position, double r, String string) {
		
		Coordinate[] circle = new Coordinate[64];
		for (int pos = 0; pos < 63; pos ++) {
			double alpha = pos * ((2*Math.PI) / 64);
			double x = Math.cos(alpha) * (r)+position.x;
			double y = Math.sin(alpha) * (r)+position.y;
			Coordinate c = new Coordinate(x,y);
			circle[pos] = c;
		}
		
		circle[63] = circle[0];
//		Algorithms.translate(position.x, position.y, circle);
		addGeometry(geofac.createLineString(circle), string);
		
	}

}
