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

package playground.gregor.sim2denvironment.approxdecomp;

import com.vividsolutions.jts.geom.*;
import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import playground.gregor.sim2denvironment.GisDebugger;
import playground.gregor.sim2denvironment.approxdecomp.ApproxConvexDecomposer.Opening;
import playground.gregor.sim2denvironment.approxdecomp.ApproxConvexDecomposer.PolygonInfo;

import java.util.*;

public class Test {




	public static void main(String [] args) throws NoSuchAuthorityCodeException, FactoryException {
		//		String ref = "6 6 6 6 4 6 4 5 5 5 5 4 5 7 4 5 4 7 5 6 5";
//		String p = "/Users/laemmel/devel/gr90_sim2d_v4/raw_input/env.shp";
//		String network = "/Users/laemmel/devel/gr90_sim2d_v4/raw_input/raw_network2d_0.xml";
//		String output = "/Users/laemmel/devel/gr90_sim2d_v4/raw_input_stage2/sim2dEnv_0.gml.gz";

		String baseName = "env";
		String dir = "/Users/laemmel/devel/hhw3/raw/";
		String p = dir + baseName +".shp";
//		String network = dir + "raw_42nd_north_east_p_c.xml";
		String network = dir + "network.xml.gz";
		String output = dir + "s2d_environment_raw_"+ baseName + ".gml.gz";
		
		GeometryFactory geofac = new GeometryFactory();

		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(p);

		List<LineString> openings = new ArrayList<LineString>();
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		new MatsimNetworkReader(sc.getNetwork()).readFile(network);
		
		for (Link  l : sc.getNetwork().getLinks().values()) {
//			if (l.getFromNode().getInLinks().size() <= 2 ||l.getToNode().getInLinks().size() <= 2){
//				continue;
//			}
			Coordinate c0 = MGC.coord2Coordinate(l.getFromNode().getCoord());
			Coordinate c1 = MGC.coord2Coordinate(l.getToNode().getCoord());
			Coordinate[] coords = new Coordinate[]{c0,c1};
			LineString ls = geofac.createLineString(coords);
			openings.add(ls);
		}

		ApproxConvexDecomposer decomp = new ApproxConvexDecomposer(openings);

		//		com.vividsolutions.jts.algorithm.CGAlgorithms.isCCW(null)


		Collection<SimpleFeature> geos = reader.getFeatureSet();
		Collection<PolygonInfo> decomposed = new ArrayList<PolygonInfo>();
		for (SimpleFeature ft : geos) {
			Geometry geo = (Geometry) ft.getDefaultGeometry();
			List<PolygonInfo> decs = decomp.decompose(geo);
			for (PolygonInfo  pi : decs) {
				decomposed.add(pi);
				Coordinate[] coords = pi.p.getExteriorRing().getCoordinates();
				Set<Integer> handled = new HashSet<Integer>();
				for (Opening open : pi.openings) {
					handled.add(open.edge);
				}
				for (int i = 0; i < coords.length-1; i++) {
					if (handled.contains(i)) {
						continue;
					}
					Coordinate c0 = coords[i];
					Coordinate c1 = coords[i+1];
					if (c0.x == c1.x) {
						if (c0.x == reader.getBounds().getMinX() || c0.x == reader.getBounds().getMaxX()){
							Opening no = new Opening();
							no.edge = i;
							pi.openings.add(no );
						}
					} else if (c0.y == c1.y) {
						if (c0.y == reader.getBounds().getMinY() || c0.y == reader.getBounds().getMaxY()) {
							Opening no = new Opening();
							no.edge = i;
							pi.openings.add(no );							
						}
					}
				}
				for (Opening open : pi.openings) {
					Coordinate c0 = coords[open.edge];
					Coordinate c1 = coords[open.edge+1];
					LineString ls = geofac.createLineString(new Coordinate[]{c0,c1});
					GisDebugger.addGeometry(ls);
					handled.add(open.edge);
				}

				
			}

		}
		GisDebugger.dump(dir + "openings.shp");
		int nr = 0;
		CoordinateReferenceSystem crs = CRS.decode("EPSG:3395");
//		Sim2DEnvironment env = new Sim2DEnvironment();
//		env.setCRS(crs);
//		env.setEnvelope(reader.getBounds());

//		QuadTree<PolygonInfo> quad = buildQuadTree(decomposed,env.getEnvelope());
		QuadTree<PolygonInfo> quad = buildQuadTree(decomposed,new Envelope());
		
		for (PolygonInfo dec : decomposed) {
			GisDebugger.addGeometry(dec.p);
			nr += dec.p.getNumPoints();
			int[] os = {};
			if (dec.openings.size() > 0) {
				os = new int[dec.openings.size()];
				for (int i = 0 ; i < dec.openings.size(); i++) {
					os[i] = dec.openings.get(i).edge;
				}
				
			}
			
			//TODO neighbor relation test does not work for neighbors with Steiner points at the corresponding opening
			Set<Id> neighborsIds = new HashSet<Id>();
			for (Opening o : dec.openings) {
				int idx0 = o.edge;
				int idx1 = idx0+1;
				Coordinate c0 = dec.p.getExteriorRing().getCoordinates()[idx0];
				Coordinate c1 = dec.p.getExteriorRing().getCoordinates()[idx1];
				Set<PolygonInfo> s0 = new HashSet<PolygonInfo>();
				Set<PolygonInfo> s1 = new HashSet<PolygonInfo>();
				quad.getRectangle(c0.x, c0.y, c0.x, c0.y, s0);
				quad.getRectangle(c1.x, c1.y, c1.x, c1.y, s1);
				for (PolygonInfo pi : s0) {
					if (pi == dec || !s1.contains(pi)) {
						continue;
					}
					if (hasOpenEdge(c0,c1,pi)){
						neighborsIds.add(Id.create("sec"+pi.hashCode(), Node.class));
					}

					
				}
			}
			Id[] n = neighborsIds.toArray(new Id[0]);			
//			env.createAndAddSection(Id.create("sec" + dec.hashCode(), Section.class), dec.p, os , n, 0);
		}

//		env.setId(Id.create(baseName, Sim2DEnvironment.class));
//		new Sim2DEnvironmentWriter02(env).write(output);
//
//		System.out.println(decomposed.size() + " == 105?");
//		GisDebugger.setCRSString("EPSG:3395");
//		GisDebugger.dump(dir + "resolved.shp");
//
//		Sim2DEnvironment env2 = new Sim2DEnvironment();
//
//		new Sim2DEnvironmentReader02(env2, false).readFile(output);
//
//		Sim2DConfig conf = Sim2DConfigUtils.createConfig();
//		conf.setEventsInterval(1);
//		conf.setTimeStepSize(0.1);
//		conf.addSim2DEnvironmentPath(output);
//		conf.addSim2DEnvNetworkMapping(output, network);
//		new Sim2DConfigWriter01(conf).write(dir + "sim2dConfig" +baseName+".xml");
//
		
		
	}

	private static boolean hasOpenEdge(Coordinate c0, Coordinate c1,
			PolygonInfo pi) {
		for (Opening o : pi.openings) {
			int idx0 = o.edge;
			int idx1 = idx0+1;
			Coordinate d0 = pi.p.getExteriorRing().getCoordinates()[idx0];
			Coordinate d1 = pi.p.getExteriorRing().getCoordinates()[idx1];
			if (d0.equals(c0) && d1.equals(c1) || d0.equals(c1) && d1.equals(c0)) {
				return true;
			}
			
		}
		return false;
	}

	private static QuadTree<PolygonInfo> buildQuadTree(
			Collection<PolygonInfo> decomposed, Envelope e) {
		
		QuadTree<PolygonInfo> ret = new QuadTree<ApproxConvexDecomposer.PolygonInfo>(e.getMinX(), e.getMinY(), e.getMaxX(), e.getMaxY());
		
		Iterator<PolygonInfo> it = decomposed.iterator();
		
		while (it.hasNext()) {
			PolygonInfo pi = it.next();
			for (Coordinate c : pi.p.getExteriorRing().getCoordinates()) {
				ret.put(c.x, c.y, pi);
			}
			
		}
		return ret;
	}

}
