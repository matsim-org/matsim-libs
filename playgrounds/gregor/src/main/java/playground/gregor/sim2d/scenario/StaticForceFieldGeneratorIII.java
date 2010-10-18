/* *********************************************************************** *
 * project: org.matsim.*
 * StaticForceFieldGeneratorIII.java
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
package playground.gregor.sim2d.scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

import playground.gregor.sim2d.gisdebug.GisDebugger;
import playground.gregor.sim2d.simulation.Force;
import playground.gregor.sim2d.simulation.SegmentedStaticForceField;
import playground.gregor.sim2d_v2.controller.Sim2DConfig;


public class StaticForceFieldGeneratorIII {


	private static final Logger log = Logger.getLogger(StaticForceFieldGeneratorIII.class);


	private static final double minX = 0;
	private static final double maxX = Double.POSITIVE_INFINITY;
	private static final double minY = 0;
	private static final double maxY = Double.POSITIVE_INFINITY;
	
//	private static final double minX = 649000;
//	private static final double maxX = 654000;
//	private static final double minY = 9892489;
//	private static final double maxY = 9897331;

	private static final double incr = 2*Math.PI/8;

	private static final String links = "/home/laemmel/devel/sim2d/data/duisburg/links.shp";
	private static final String nodes = "/home/laemmel/devel/sim2d/data/duisburg/nodes.shp";

	private final GeometryFactory geofac = new GeometryFactory();

	private final List<Feature> ll = new ArrayList<Feature>();

	private Map<Id, List<Id>> mapping;

	private final Map<Long,Feature> nodesMapping = new HashMap<Long, Feature>();

	private int count = 0;

	public StaticForceFieldGeneratorIII(Map<Id, List<Id>> linkSubLinkMapping) {
		this.mapping = linkSubLinkMapping;
	}

	public SegmentedStaticForceField getStaticForceField() {
		init();
		Map<Id,QuadTree<Force>> forces = new HashMap<Id, QuadTree<Force>>();
		for (Feature ft : ll) {
			QuadTree<Force> forcesQuadTree = calculateLinkForces(ft);
			if (forcesQuadTree != null && forcesQuadTree.size() > 0) {
				Long id = (Long) ft.getAttribute("ID");
				for (Id subId : this.mapping.get(new IdImpl(id))) {
					forces.put(subId, forcesQuadTree);
				}


			}

		}
		GisDebugger.dump("/home/laemmel/devel/sim2d/tmp/staticForcesDbg.shp");
		return new SegmentedStaticForceField(forces);
	}


	private QuadTree<Force> calculateLinkForces(Feature ft) {



		Geometry link = ft.getDefaultGeometry();


		Coordinate c = link.getCoordinate();
		if (c.x > maxX || c.y > maxY || c.x < minX || c.y < minY) {
			return null;
		}




		Geometry from = this.nodesMapping.get(ft.getAttribute("fromID")).getDefaultGeometry().buffer(0.1);
		Geometry to = this.nodesMapping.get(ft.getAttribute("toID")).getDefaultGeometry().buffer(0.1);
		Geometry ttt = link.union(from).union(to);
		if (ttt instanceof MultiPolygon) {

			log.error("expected polygon but got multi polygon with id:" + ft.getAttribute("ID") + " at:" + ttt.getCentroid() );
			from = from.buffer(.5);
			to = from.buffer(.5);
			ttt = link.union(from).union(to);
		}
		Polygon hole = (Polygon) ttt;
		Envelope env = getEnvelope(hole);
		QuadTree<Force> ret = new QuadTree<Force>(env.getMinX(),env.getMinY(),env.getMaxX(),env.getMaxY());

		//		int loop = 0;
		//		int yloop = 0;
		for (double x = env.getMinX(); x <= env.getMaxX(); x += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
			//			log.info("xloop:" + ++loop + "  yloop:" + yloop);
			for (double y = env.getMinY(); y <= env.getMaxY(); y += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
				//				yloop++;
				Point point = this.geofac.createPoint(new Coordinate(x,y));
				if (!hole.covers(point) ) {
					continue;
				}
				double dist = hole.getExteriorRing().distance(point);
				if (dist > 0.1 && dist <= Sim2DConfig.Bw ) {
					Force f = calculateSimpleForce(x,y,hole.getExteriorRing(), dist);
					if (f != null && (f.getFx() > 0 || f.getFy() > 0) ) {
						ret.put(f.getXCoord(), f.getYCoord(), f);
					}

				}
			}
		}
		if (++count % 1 == 0) {
			log.info(count + " links processed");
		} 
		return ret;
	}

	private Force calculateSimpleForce(double x, double y,
			LineString shell, double dist2) {
		Force f = new Force(0,0,x,y);
		DistanceOp op = new DistanceOp(shell,this.geofac.createPoint(new Coordinate(x,y)));
		Coordinate[] tmp = op.closestPoints();
		double fX = tmp[1].x - tmp[0].x;
		double fY = tmp[1].y - tmp[0].y;
//		//////		//DEBUG
		Coordinate[] tmp1 = new Coordinate[]{tmp[0],new Coordinate(tmp[0].x+0.01,tmp[0].y+0.01),tmp[1],tmp[0]};
		LinearRing lr = this.geofac.createLinearRing(tmp1);
		Polygon tmpP = this.geofac.createPolygon(lr, null);
		GisDebugger.addGeometry(tmpP);

		double exp = Math.exp(Sim2DConfig.Bw/dist2);
		fX *= exp/dist2;
		fY *= exp/dist2;


		f.setFx(f.getFx()+fX);
		f.setFy(f.getFy()+fY);
		return f;

	}

	private Envelope getEnvelope(Geometry geo) {
		Envelope envelope = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			envelope.expandToInclude(c);
		}
		return envelope;
	}

	private void init() {
		try {
			readLinks();
			readNode();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void readNode() throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(nodes);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature)it.next();
			Long id = (Long) ft.getAttribute("ID");
			this.nodesMapping.put(id, ft);

		}
	}


	private void readLinks() throws IOException {
		FeatureSource fts = ShapeFileReader.readDataFile(links);
		Iterator it = fts.getFeatures().iterator();
		while (it.hasNext()) {
			Feature ft = (Feature)it.next();
			this.ll.add(ft);
		}
	}

}
