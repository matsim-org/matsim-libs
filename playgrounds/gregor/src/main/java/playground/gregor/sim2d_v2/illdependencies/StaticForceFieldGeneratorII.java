/* *********************************************************************** *
 * project: org.matsim.*
 * StaticForceFieldGeneratorII.java
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
package playground.gregor.sim2d_v2.illdependencies;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.data.FeatureSource;
import org.geotools.feature.Feature;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.gis.ShapeFileReader;

import playground.gregor.sim2d_v2.controller.Sim2DConfig;
import playground.gregor.sim2d_v2.gisdebug.GisDebugger;
import playground.gregor.sim2d_v2.simulation.floor.StaticForceField;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.distance.DistanceOp;

public class StaticForceFieldGeneratorII  {

	private static final Logger log = Logger.getLogger(StaticForceFieldGeneratorII.class);


	private static final double minX = 649000;
	private static final double maxX = 654000;
	private static final double minY = 9892489;
	private static final double maxY = 9897331;
	
	private static final double incr = 2*Math.PI/8;

	private static final String links = "/home/laemmel/devel/sim2d/data/links.shp";
	private static final String nodes = "/home/laemmel/arbeit/svn/shared-svn/studies/countries/id/padang/gis/network_v20080618/nodes_v20090728.shp";

	private final GeometryFactory geofac = new GeometryFactory();

	private final List<Feature> ll = new ArrayList<Feature>();
	private final Map<Long,Feature> nodesMapping = new HashMap<Long, Feature>();
	private MultiPolygon structure;

	private QuadTree<Force> forcesQuadTree;


	private LinearRing envelope;


	private int count;



	public StaticForceFieldGeneratorII(MultiPolygon structure) {
		this.structure = structure;
	}


	public StaticForceField loadStaticForceField() {
		init();

		calculateForces();
		return new StaticForceField(this.forcesQuadTree);
	}

	private void calculateForces() {
		this.count = 0;
		for (Feature ft : ll) {
			calculateLinkForces(ft);


		}
		GisDebugger.dump("/home/laemmel/devel/sim2d/tmp/staticForcesDbg.shp");
	}


	private void calculateLinkForces(Feature ft) {
	
		
		Geometry link = ft.getDefaultGeometry();

		Coordinate c = link.getCoordinate();
		if (c.x > maxX || c.y > maxY || c.x < minX || c.y < minY) {
			return;
		}
		
		if (++count % 1 == 0) {
			log.info(count + " links processed");
		} 
//		if (++count < 824) {
//			return;
//		} 
		
		Geometry from = this.nodesMapping.get(ft.getAttribute("from")).getDefaultGeometry().buffer(0.53);
		Geometry to = this.nodesMapping.get(ft.getAttribute("to")).getDefaultGeometry().buffer(0.53);
		Geometry ttt = link.union(from).union(to);
		if (ttt instanceof MultiPolygon) {
			
			log.error("expected polygon but got multi polygon with id:" + ft.getAttribute("ID") + " at:" + ttt.getCentroid() );
			from = from.buffer(.5);
			to = from.buffer(.5);
			ttt = link.union(from).union(to);
		}
		Polygon hole = (Polygon) ttt;
		
		Geometry geo = this.geofac.createPolygon(this.envelope, new LinearRing[]{(LinearRing) hole.getExteriorRing()});
		
		Envelope envelope = getEnvelope(hole);
//		int loop = 0;
//		int yloop = 0;
		for (double x = envelope.getMinX(); x <= envelope.getMaxX(); x += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
//			log.info("xloop:" + ++loop + "  yloop:" + yloop);
			for (double y = envelope.getMinY(); y <= envelope.getMaxY(); y += Sim2DConfig.STATIC_FORCE_RESOLUTION) {
//				yloop++;
				Point point = this.geofac.createPoint(new Coordinate(x,y));
				if (!link.covers(point) ) {
					continue;
				}
				double dist = hole.getExteriorRing().distance(point);
				if (dist > 0.1 && dist <= Sim2DConfig.Bw ) {
					Force f = calculateSimpleForce(x,y,hole.getExteriorRing(), dist);
					if (f != null && (f.getFx() > 0 || f.getFy() > 0) ) {
						this.forcesQuadTree.put(f.getXCoord(), f.getYCoord(), f);
					}

				}
			}
		}
		if (++count % 1 == 0) {
			log.info(count + " links processed");
		} 
//		if (this.count >= 100) {
			
//			throw new RuntimeException();
//		}
	}


	private Force calculateSimpleForce(double x, double y,
			LineString shell, double dist2) {
		Force f = new Force(0,0,x,y);
		DistanceOp op = new DistanceOp(shell,this.geofac.createPoint(new Coordinate(x,y)));
		Coordinate[] tmp = op.closestPoints();
		double fX = tmp[1].x - tmp[0].x;
		double fY = tmp[1].y - tmp[0].y;
//		double dist = Math.sqrt(Math.pow(fX,2)+Math.pow(fY,2 ));
//		if (dist > Sim2DConfig.Bw || dist <= 0.01) {
//			return f;
//		} 		
////		//DEBUG
		Coordinate[] tmp1 = new Coordinate[]{tmp[0],new Coordinate(tmp[0].x+0.01,tmp[0].y+0.01),tmp[1],tmp[0]};
		LinearRing lr = this.geofac.createLinearRing(tmp1);
		Polygon tmpP = this.geofac.createPolygon(lr, null);
		GisDebugger.addGeometry(tmpP);
		
		double exp = Math.exp(Sim2DConfig.Bw/dist2);
		fX *= exp/dist2;
		fY *= exp/dist2;
//		fX /= dist;
//		fY /= dist;
		
//		double l2 = Math.sqrt(Math.pow(fX, 2)+Math.pow(fY, 2));
//		System.out.println(dist + " " + l2);
		
		f.setFx(f.getFx()+fX);
		f.setFy(f.getFy()+fY);
		return f;
		
	}


	private Force calculateForce(double x, double y, Geometry geo) {
		Force f = new Force(0,0,x,y);

		Coordinate c0 = new Coordinate(x,y);
		double alpha = 0;
		double cos = 0;
		double sin = 0;
		int contr = 0;
		for (; alpha < 2*Math.PI; ) {
			Coordinate [] coords = new Coordinate[4];
			coords[0] = c0;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x1 = x + cos * Sim2DConfig.Bw;
			double y1 = y + sin * Sim2DConfig.Bw;
			Coordinate c1 = new Coordinate(x1,y1);
			coords[1] = c1;

			alpha += incr;

			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			double x2 = x + cos * Sim2DConfig.Bw;
			double y2 = y + sin * Sim2DConfig.Bw;
			Coordinate c2 = new Coordinate(x2,y2);
			coords[2] = c2;
			coords[3] = c0;

			if(calcAndAddSectorForce(f,coords,geo)) {
				contr++;
			}
		}
		if (contr == 0) {
			return null;
		}
		f.setFx(f.getFx()/contr);
		f.setFy(f.getFy()/contr);

//
//		Coordinate [] cooo = new Coordinate[] {new Coordinate(f.getXCoord(),f.getYCoord()),new Coordinate(f.getXCoord()+0.01,f.getYCoord()+0.01),new Coordinate(f.getXCoord()+f.getFx(),f.getYCoord()+f.getFy()),new Coordinate(f.getXCoord(),f.getYCoord())};
//		LinearRing lr = this.geofac.createLinearRing(cooo);
//		Polygon ppp = this.geofac.createPolygon(lr, null);
//		GisDebugger.addGeometry(ppp);
//		
		return f;	
	}


	private boolean calcAndAddSectorForce(Force f, Coordinate[] coords, Geometry geo) {
		Polygon p = this.geofac.createPolygon(this.geofac.createLinearRing(coords), null);

		Geometry g = geo.intersection(p);
		if (!(g instanceof GeometryCollection)) {
			DistanceOp op = new DistanceOp(g,this.geofac.createPoint(coords[0]));
			Coordinate[] tmp = op.closestPoints();
			double fX = tmp[1].x - tmp[0].x;
			double fY = tmp[1].y - tmp[0].y;
			double dist = Math.sqrt(Math.pow(fX,2)+Math.pow(fY,2 ));
			if (dist > Sim2DConfig.Bw) {
				throw new RuntimeException("this should not happen!!");
			} else if (dist <= 0.01) {
				return false;
			}
			
////			//DEBUG
//			GisDebugger.addGeometry(p);
//			Coordinate[] tmp1 = new Coordinate[]{tmp[0],new Coordinate(tmp[0].x+0.01,tmp[0].y+0.01),tmp[1],tmp[0]};
//			LinearRing lr = this.geofac.createLinearRing(tmp1);
//			Polygon tmpP = this.geofac.createPolygon(lr, null);
//			GisDebugger.addGeometry(tmpP);
			
			double exp = Math.exp(Sim2DConfig.Bw/dist);
			fX *= exp/dist;
			fY *= exp/dist;
//			fX /= dist;
//			fY /= dist;
			
//			double l2 = Math.sqrt(Math.pow(fX, 2)+Math.pow(fY, 2));
//			System.out.println(dist + " " + l2);
			
			f.setFx(f.getFx()+fX);
			f.setFy(f.getFy()+fY);
			return true;
		}
		
		return false;
		
	}


	private void init() {
		try {
			readLinks();
			readNode();
		} catch (IOException e) {
			e.printStackTrace();
		}
		initQuadTree();

	}


	private Envelope getEnvelope(Geometry geo) {
		Envelope envelope = new Envelope();
		for (Coordinate c : geo.getCoordinates()) {
			envelope.expandToInclude(c);
		}
		return envelope;
	}

	private void initQuadTree() {
		Geometry geo = this.structure.getEnvelope();
		Envelope envelope = getEnvelope(geo);
		Coordinate c1 = new Coordinate(envelope.getMaxX(),envelope.getMaxY());
		Coordinate c2 = new Coordinate(envelope.getMaxX(),envelope.getMinY());
		Coordinate c3 = new Coordinate(envelope.getMinX(),envelope.getMinY());
		Coordinate c4 = new Coordinate(envelope.getMinX(),envelope.getMaxY());
		this.envelope = this.geofac.createLinearRing(new Coordinate[] {c1,c2,c3,c4,c1});
		this.forcesQuadTree = new QuadTree<Force>(envelope.getMinX(),envelope.getMinY(),envelope.getMaxX(),envelope.getMaxY());
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
