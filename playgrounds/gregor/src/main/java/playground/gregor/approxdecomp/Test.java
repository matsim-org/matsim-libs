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
import java.util.List;
import java.util.Set;

import org.geotools.feature.Feature;
import org.geotools.referencing.CRS;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.gregor.approxdecomp.ApproxConvexDecomposer.Opening;
import playground.gregor.approxdecomp.ApproxConvexDecomposer.PolygonInfo;
import playground.gregor.sim2d_v3.helper.gisdebug.GisDebugger;
import playground.gregor.sim2d_v4.scenario.Sim2DEnvironment;
import playground.gregor.sim2dio.Sim2DEnvironmentReader02;
import playground.gregor.sim2dio.Sim2DEnvironmentWriter02;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class Test {




	public static void main(String [] args) throws NoSuchAuthorityCodeException, FactoryException {
		//		String ref = "6 6 6 6 4 6 4 5 5 5 5 4 5 7 4 5 4 7 5 6 5";
		String p = "/Users/laemmel/devel/convexdecomp/polygon03_.shp";
		String initOpen = "/Users/laemmel/devel/convexdecomp/intialOpenings.shp";
		//		String p = "/Users/laemmel/tmp/dump.shp";
//				DecompGuiDebugger dbg = new DecompGuiDebugger();
//				dbg.setVisible(true);

		GeometryFactory geofac = new GeometryFactory();

		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(p);

		ShapeFileReader reader2 = new ShapeFileReader();
		reader2.readFileAndInitialize(initOpen);
		List<LineString> openings = new ArrayList<LineString>();
		for (Feature ft : reader2.getFeatureSet()) {
			Geometry geo = ft.getDefaultGeometry();
			if (geo instanceof LineString) {
				openings.add((LineString) geo);
			} else if (geo instanceof MultiLineString) {
				for (int i = 0; i < geo.getNumGeometries(); i++) {
					openings.add((LineString) geo.getGeometryN(i));

				}
			}
		}



		ApproxConvexDecomposer decomp = new ApproxConvexDecomposer(openings);

		//		com.vividsolutions.jts.algorithm.CGAlgorithms.isCCW(null)


		Set<Feature> geos = reader.getFeatureSet();
		Collection<PolygonInfo> decomposed = new ArrayList<PolygonInfo>();
		for (Feature ft : geos) {
			Geometry geo = ft.getDefaultGeometry();
			List<PolygonInfo> decs = decomp.decompose(geo);
			for (PolygonInfo  pi : decs) {
				decomposed.add(pi);
				Coordinate[] coords = pi.p.getExteriorRing().getCoordinates();
				for (Opening open : pi.openings) {
					Coordinate c0 = coords[open.edge];
					Coordinate c1 = coords[open.edge+1];
					LineString ls = geofac.createLineString(new Coordinate[]{c0,c1});
					GisDebugger.addGeometry(ls);
				}
			}

		}
		GisDebugger.dump("/Users/laemmel/devel/convexdecomp/openings.shp");
		int nr = 0;
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3395");
		Sim2DEnvironment env = new Sim2DEnvironment();
		env.setCRS(crs);
		env.setEnvelope(reader.getBounds());

		for (PolygonInfo dec : decomposed) {
			GisDebugger.addGeometry(dec.p);
			nr += dec.p.getNumPoints();
			int[] os = null;
			if (dec.openings.size() > 0) {
				os = new int[dec.openings.size()];
				for (int i = 0 ; i < dec.openings.size(); i++) {
					os[i] = dec.openings.get(i).edge;
				}
			}
			env.createAndAddSection(new IdImpl(dec.hashCode()), dec.p, os , null, 0);

		}

		new Sim2DEnvironmentWriter02(env).write("/Users/laemmel/devel/convexdecomp/gml/resolved.gml.gz");

		System.out.println(decomposed.size() + " == 105?");
		GisDebugger.dump("/Users/laemmel/devel/convexdecomp/resolved.shp");

		Sim2DEnvironment env2 = new Sim2DEnvironment(); 

		new Sim2DEnvironmentReader02(env2, false).readFile("/Users/laemmel/devel/convexdecomp/gml/resolved.gml.gz");

	}

}
