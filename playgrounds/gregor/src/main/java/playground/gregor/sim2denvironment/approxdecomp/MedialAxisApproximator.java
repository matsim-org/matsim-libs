/* *********************************************************************** *
 * project: org.matsim.*
 * VoronoiDiagram.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.util.ArrayList;
import java.util.List;

import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;

import playground.gregor.sim2denvironment.Algorithms;
import playground.gregor.sim2denvironment.approxdecomp.Graph.Link;
import playground.gregor.sim2denvironment.approxdecomp.Graph.Node;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

public class MedialAxisApproximator {

	private static final double EPSILON = 0.0001;
	private final GeometryFactory geofac = new GeometryFactory();
	private double stepSize = .1;
	//	public 


	public Node run(Polygon p) {

		p = (Polygon) p.clone();
		
		Geometry envG = p.getEnvelope();
		Envelope e = new Envelope();
		for (Coordinate c : envG.getCoordinates()) {
			e.expandToInclude(c);
		}
		double ox = e.getMinX();
		double oy = e.getMinY();
		int nr = p.getCoordinates().length;
		for (int i = 0; i < nr; i++) {
			if (i == nr-1 && p.getCoordinates()[i]==p.getCoordinates()[0]) {
				continue;
			}
			Coordinate c = p.getCoordinates()[i];
			c.x -= e.getMinX();
			c.y -= e.getMinY();
		}
		
		envG = p.getEnvelope();
		int nrEnv = envG.getCoordinates().length;
		for (int i = 0; i < nrEnv; i++) {
			if (i == nrEnv-1 && envG.getCoordinates()[i]==envG.getCoordinates()[0]){
				continue;
			}
			Coordinate c = envG.getCoordinates()[i];
			c.x -= e.getMinX();
			c.y -= e.getMinY();
		}
		e = new Envelope();
		for (Coordinate c : envG.getCoordinates()) {
			e.expandToInclude(c);
		}
		
		
		double w = e.getWidth();
		double h = e.getHeight();
		double d = Math.sqrt(w*w+h*h);
		this.stepSize = d/100;

		MultiPoint mp = getDenseMultiPointFromPolygon(p);

//		PrecisionModel pm = new PrecisionModel(10);
//		mp = (MultiPoint) SimpleGeometryPrecisionReducer.reduce(mp, pm);
//		mp = (MultiPoint) mp.intersection(mp);
		VoronoiDiagramBuilder vdb = new VoronoiDiagramBuilder();
		vdb.setSites(mp);
		Geometry diag = vdb.getDiagram(this.geofac);
		//		
		//		

		QuadTree<Node> quadTree = new QuadTree<Node>(e.getMinX(), e.getMinY(), e.getMaxX()+1, e.getMaxY()+1);
		if (diag instanceof GeometryCollection) {
			GeometryCollection geocoll = (GeometryCollection)diag;
			int nid = 0;
			for (int i = 0; i < geocoll.getNumGeometries(); i++){
				Geometry geo = geocoll.getGeometryN(i);
				if (geo instanceof Polygon) {
					Polygon vp = (Polygon)geo;
					Coordinate[] coords = vp.getExteriorRing().getCoordinates();
					for (int j = 0; j < coords.length-1; j++) {
						if (Algorithms.contains(coords[j], p.getExteriorRing().getCoordinates()) && Algorithms.contains(coords[j+1], p.getExteriorRing().getCoordinates()) ){
							boolean cont = false;
							for (int k = 0; k < p.getExteriorRing().getCoordinates().length-1; k++) {
								Coordinate cp0 = p.getExteriorRing().getCoordinates()[k ];
								Coordinate cp1 = p.getExteriorRing().getCoordinates()[k+1];
								double left0 = Algorithms.isLeftOfLine(coords[j], cp0, cp1);
								double left1 = Algorithms.isLeftOfLine(coords[j+1], cp0, cp1);
								if (left0*left1 < 0) {
									double left2 = Algorithms.isLeftOfLine(cp0, coords[j],coords[j+1]);
									double left3 = Algorithms.isLeftOfLine(cp1, coords[j],coords[j+1]);
									if (left2*left3 < 0) {
										cont = true;
										break;
									}
								}

							}
							if (cont) {
								continue;
							}

							Node n0 = quadTree.getClosest(coords[j].x, coords[j].y);
							if (n0 == null || n0.c.distance(coords[j]) > EPSILON) {
								n0 = new Node(coords[j], nid++);
								quadTree.put(coords[j].x, coords[j].y, n0);
							}
							Node n1 = quadTree.getClosest(coords[j + 1].x, coords[j + 1].y);
							if (n1 == null || n1.c.distance(coords[j+1]) > EPSILON) {
								n1 = new Node(coords[j+1], nid++);
								quadTree.put(coords[j+1].x, coords[j+1].y, n1);
							}
							Link l = new Link(n0,n1);
							n0.addOutLink(l);
						}
					}
				} else {
					throw new RuntimeException("Geometries of type:" + geo.getGeometryType() + " can not be handled be this piece of code.");
				}
			}
		}
		for (Node n : quadTree.values()) {
			n.c.x += ox;
			n.c.y += oy;
		}
//		GisDebugger.dump("/Users/laemmel/tmp/vis/!pp.shp");
//		for (Node n : quadTree.values()) {
//			for (Link l : n.outLinks) {
//				Coordinate c0 = l.n0.c;
//				Coordinate c1 = l.n1.c;
//				LineString ls = this.geofac.createLineString(new Coordinate[]{c0,c1});
//			}
//		}
//		GisDebugger.dump("/Users/laemmel/tmp/vis/!v0.shp");
//		throw new RuntimeException();

		return quadTree.getClosest(0, 0);
	}


	private MultiPoint getDenseMultiPointFromPolygon(Polygon pp) {
		MultiPoint ret = null;

		List<Point> points = new ArrayList<Point>();
		for (int i = 0; i < pp.getNumPoints(); i++) {
			Point p = pp.getExteriorRing().getPointN(i);
			if (i > 0) {
				Point old = pp.getExteriorRing().getPointN(i-1);
				double dist = old.distance(p);
				double dx = (p.getX() - old.getX())/dist;
				double dy = (p.getY() - old.getY())/dist;
				double steps = dist/this.stepSize;
				for (int j = 1; j < steps; j++) {
					double x = old.getX() + j*this.stepSize*dx;
					double y = old.getY() + j*this.stepSize*dy;
					Point tmp = this.geofac.createPoint(new Coordinate(x,y));
					points.add(tmp);
				}
			}
			points.add(p);
		}

		Point [] pointsA = new Point[points.size()];
		for (int i = 0; i < points.size(); i++) {
			pointsA[i] = points.get(i);
		}

		ret = this.geofac.createMultiPoint(pointsA);
		return ret;
	}


	public static void main(String [] args) {

		Coordinate c0 = new Coordinate(0,0);
		Coordinate c1 = new Coordinate(10,0);
		Coordinate c2 = new Coordinate(10,10);
		Coordinate c3 = new Coordinate(5,10);
		Coordinate c4 = new Coordinate(5,5);
		Coordinate c41 = new Coordinate(2.5,7);
		Coordinate c42 = new Coordinate(1.5,3);
		Coordinate c5 = new Coordinate(0,10);
		Coordinate [] coords = {c0,c1,c2,c3,c4,c41,c42,c5,c0};

		GeometryFactory geofac = new GeometryFactory();	
		LinearRing lr = geofac.createLinearRing(coords);
		Polygon p = geofac.createPolygon(lr, null);
		

		MedialAxisApproximator maa = new MedialAxisApproximator();
		Node n = maa.run(p);
		
		ShortestPath aStar = new ShortestPath();
		Tuple<Node,Double> tupl = aStar.getFarestNode(n);
		Tuple<Node, Double> tupl2 = aStar.getFarestNode(tupl.getFirst());
		System.out.println("n:" + n.c + " f:" + tupl.getFirst().c + "cost:" + tupl.getSecond());
		System.out.println(tupl2.getFirst().c + "cost:" + tupl2.getSecond());

		
	}

}
