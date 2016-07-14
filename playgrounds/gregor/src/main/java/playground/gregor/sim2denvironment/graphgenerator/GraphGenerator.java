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

package playground.gregor.sim2denvironment.graphgenerator;


import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;
import org.opengis.feature.simple.SimpleFeature;

import java.util.*;

public class GraphGenerator {

	private static GeometryFactory  geofac = new GeometryFactory();
	private final Scenario sc;

	private final Envelope envelope;
	private final Collection<Geometry> geometries;
	private final double offsetX;
	private final double offsetY;


	public GraphGenerator(Scenario sc, Collection<Geometry> geometries, Envelope envelope) {
		this.sc = sc;
		this.geometries = geometries;
		this.offsetX = -envelope.getMinX();
		this.offsetY = -envelope.getMinY();
//		
//		this.offsetX = 0;//-envelope.getMinX();
//		this.offsetY = 0;//-envelope.getMinY();
//		
		this.envelope = new Envelope(0,envelope.getMaxX()+this.offsetX,0,envelope.getMaxY()+this.offsetY);
	}


	public void run() {

		offsetGeometries();
		
		
		List<LineString> ls =  extractLineStrings();
		//		new LineStringSnapper().run(ls, this.envelope);


		MultiPoint mp = new DenseMultiPointFromLineString().getDenseMultiPointFromLineString(ls);
		
		
//		PrecisionModel p = new PrecisionModel(PrecisionModel.FLOATING_SINGLE);
//		mp = (MultiPoint) com.vividsolutions.jts.precision.SimpleGeometryPrecisionReducer.reduce(mp, p);
		Geometry boundary = new MultiPolygonFromLineStrings().getMultiPolygon(ls, this.envelope);

		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		vdb.setTolerance(0.2);

		vdb.setSites(mp);
		GeometryCollection dia = (GeometryCollection) vdb.getDiagram(geofac);
		boundary = boundary.buffer(0.01);

		Skeleton skeleton = new SkeletonExtractor().extractSkeleton(dia, boundary);
		skeleton.dumpLinks("/Users/laemmel/tmp/vis/skeleton.shp");
		skeleton.dumpIntersectingNodes("/Users/laemmel/tmp/vis/skeletonNodes.shp");

		new SkeletonSimplifier().simplifySkeleton(skeleton,boundary);
		skeleton.dumpLinks("/Users/laemmel/tmp/vis/skeletonSimpl.shp");

		new SkeletonLinksContraction().contractShortSkeletonLinks(skeleton, boundary);
		skeleton.dumpLinks("/Users/laemmel/tmp/vis/skeletonContr.shp");

		new Puncher().punchSkeleton(skeleton, this.envelope);

		new StubRemover().run(skeleton);
		skeleton.dumpLinks("/Users/laemmel/tmp/vis/skeletonStubRM.shp");

		
		unoffsetSkeleton(skeleton);
		
		createNetwork(skeleton);

		new NetworkCleaner().run(this.sc.getNetwork());

	}


	private void unoffsetSkeleton(Skeleton skeleton) {
		Set<Coordinate> handled = new HashSet<Coordinate>();
		for (SkeletonLink l : skeleton.getLinks()) {
			Coordinate from = l.getFromNode().getCoord();
			if (!handled.contains(from)) {
				from.x -= this.offsetX;
				from.y -= this.offsetY;
				handled.add(from);
			}
			Coordinate to = l.getToNode().getCoord();
			if (!handled.contains(to)) {
				to.x -= this.offsetX;
				to.y -= this.offsetY;
				handled.add(to);
			}
		}
		
	}


	private void offsetGeometries() {
		Set<Coordinate> handled = new HashSet<Coordinate>();
		for (Geometry geo : this.geometries) {
			for (Coordinate c : geo.getCoordinates()) {
				if (handled.contains(c)){
					continue;
				}
				c.x += this.offsetX;
				c.y += this.offsetY;
				handled.add(c);
			}
		}
	}


	private void createNetwork(Skeleton skeleton) {
		NetworkFactory fac = this.sc.getNetwork().getFactory();
//		for (SkeletonNode n : skeleton.getNodes()) {
//			Node mn = fac.createNode(n.getId(), MGC.coordinate2Coord(n.getCoord()));
//			this.sc.getNetwork().addNode(mn);
//		}
		int linkNums = 0;
		for (SkeletonLink l : skeleton.getLinks()) {
			if (this.sc.getNetwork().getNodes().get(l.getFromNode().getId()) == null) {
				Node mn = fac.createNode(l.getFromNode().getId(), MGC.coordinate2Coord(l.getFromNode().getCoord()));
				this.sc.getNetwork().addNode(mn);
			}
			if (this.sc.getNetwork().getNodes().get(l.getToNode().getId()) == null) {
				Node mn = fac.createNode(l.getToNode().getId(), MGC.coordinate2Coord(l.getToNode().getCoord()));
				this.sc.getNetwork().addNode(mn);
			}
			
			Node fromNode = this.sc.getNetwork().getNodes().get(l.getFromNode().getId());
			Node toNode = this.sc.getNetwork().getNodes().get(l.getToNode().getId());

			Link ml1 = fac.createLink(Id.create(linkNums++, Link.class), fromNode, toNode);
			ml1.setFreespeed(1.34);
			ml1.setLength(l.getLength());
			this.sc.getNetwork().addLink(ml1);

			Link ml2 = fac.createLink(Id.create(linkNums++, Link.class), toNode, fromNode);
			ml2.setFreespeed(1.34);
			ml2.setLength(l.getLength());
			this.sc.getNetwork().addLink(ml2);
		}

	}


	private List<LineString> extractLineStrings() {
		List<LineString> ret = new ArrayList<LineString>();
		for (Geometry geo : this.geometries) {
			if (geo instanceof LineString) {
				ret.add((LineString) geo);
			} else if (geo instanceof MultiLineString) {
				MultiLineString ml = (MultiLineString)geo;
				for (int i = 0; i < ml.getNumGeometries(); i++) {
					Geometry ggeo = ml.getGeometryN(i);
					ret.add((LineString) ggeo);
				}
			} else if (geo instanceof MultiPolygon){
				MultiPolygon mp = (MultiPolygon)geo;
				for (int i = 0; i < mp.getNumGeometries(); i++) {
					Geometry gg = mp.getGeometryN(i);
					Polygon p = (Polygon)gg;
					ret.add(p.getExteriorRing());
				}
			}else if (geo instanceof Polygon){
				Polygon p = (Polygon)geo;
				ret.add(p.getExteriorRing());
			}else{
				throw new RuntimeException("Geometry type: " +geo.getGeometryType() +" not (yet) supported.");
			}
		}

		return ret;
	}


	public static void main(String [] args) throws FactoryRegistryException, SchemaException, IllegalAttributeException {
		//		String floorplan = "/Users/laemmel/tmp/voronoi/test.shp";
				String dir = "/Users/laemmel/devel/gct/floorpl/";
				String floorplan = dir + "floorplan.shp";
				String net = dir + "raw_network2d_floorplan.xml";
//		String floorplan = "/Users/laemmel/devel/convexdecomp/03.shp";
		Config c = ConfigUtils.createConfig();
		Scenario sc = ScenarioUtils.createScenario(c);
		ShapeFileReader reader = new ShapeFileReader();
		reader.readFileAndInitialize(floorplan);
		Collection<Geometry> geos = new ArrayList<Geometry>();
		for (SimpleFeature ft : reader.getFeatureSet()) {
			geos.add((Geometry) ft.getDefaultGeometry());
		}

		new GraphGenerator(sc,geos,reader.getBounds()).run();
		new NetworkWriter(sc.getNetwork()).write(net);
		//		String [] argsII = {"/Users/laemmel/devel/sim2dDemoII/input/network.xml","/Users/laemmel/devel/sim2dDemoII/raw_input/networkL.shp","/Users/laemmel/devel/sim2dDemoII/raw_input/networkP.shp","EPSG:3395"};
		//		new NetworkWriter(sc.getNetwork()).write("/Users/laemmel/tmp/vis/network.xml");
		String [] argsII = {net,dir + "networkL.shp",dir + "networkP.shp","EPSG:3395"};
		Links2ESRIShape.main(argsII);
	}


}
