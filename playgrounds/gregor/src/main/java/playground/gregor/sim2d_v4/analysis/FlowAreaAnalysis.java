/* *********************************************************************** *
 * project: org.matsim.*
 * FlowAnalysis.java
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

package playground.gregor.sim2d_v4.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.geotools.MGC;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class FlowAreaAnalysis implements XYVxVyEventsHandler{

	private final Map<Double,Measurement> measurements = new TreeMap<Double, FlowAreaAnalysis.Measurement>();
	private final List<Measurement> chronologicalMeasurements = new ArrayList<FlowAreaAnalysis.Measurement>();

	private final Geometry e;

	private double lastUpdate = -1;

	ArrayList<XYVxVyEventImpl> events = new ArrayList<XYVxVyEventImpl>();

	private final String fileName;
	private final EventsManager em;
	private Geometry buffer;

	private final GeometryFactory geofac = new GeometryFactory();


	public FlowAreaAnalysis(Envelope e, String fileName,EventsManager em) {
		Coordinate [] coords = new Coordinate[5];
		coords[0] = new Coordinate(e.getMinX(),e.getMinY());
		coords[1] = new Coordinate(e.getMaxX(),e.getMinY());
		coords[2] = new Coordinate(e.getMaxX(),e.getMaxY());
		coords[3] = new Coordinate(e.getMinX(),e.getMaxY());
		coords[4] = coords[0];
		LinearRing lr = this.geofac.createLinearRing(coords);
		Polygon p = this.geofac.createPolygon(lr, null);
		this.e = p;
		this.fileName = fileName;
		this.em = em;
		init();
	}
	public FlowAreaAnalysis(Geometry g, String fileName, EventsManager em) {
		this.e = g;
		this.fileName = fileName;
		this.em = em;
		init();
	}

	private void init() {
		List<Coordinate> coords = new ArrayList<Coordinate>();
		Coordinate[] ec = ((Polygon)this.e.convexHull()).getExteriorRing().getCoordinates();
		for (int i = 1; i < ec.length; i++) {
			Coordinate c0 = ec[i-1];
			Coordinate c1 = ec[i];
			if (Math.abs(c0.x - c1.x) < 5) {
				double dx = (c1.x - c0.x)*10;
				double dy = (c1.y - c0.y)*10;
				Coordinate cc0 = new Coordinate(c0.x - dx, c0.y - dy);
				Coordinate cc1 = new Coordinate(c0.x + dx, c0.y + dy);
				//				this.bounds.add(new Tuple<Coordinate,Coordinate>(cc0,cc1));
				//				double dx2 = cc1.x-cc0.x;
				//				double dy2 = cc1.y-cc0.y;
				//				System.out.println(dx2 + "  " + (c0.x-c1.x));
				coords.add(cc0);
				coords.add(cc1);
			}
		}
		Coordinate[] c = coords.toArray(new Coordinate[0]);

		this.buffer = this.geofac.createMultiPoint(c).convexHull();

	}
	@Override
	public void reset(int iteration) {
		//there is no time for a nicer way of doing this!! needs to be fixed!
		if (this.measurements.size() == 0) {
			return;
		}
		try {
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File(this.fileName+".it"+iteration)));
			for (Measurement m : this.measurements.values()) {
				bf.append(m.rho + " " + m.j+ " "+ m.v + "\n");
			}
			bf.close();
			BufferedWriter bf2 = new BufferedWriter(new FileWriter(new File(this.fileName+"_flow.it"+iteration)));
			LinkedList<Measurement> list = new LinkedList<Measurement>();
			Iterator<Measurement> it = this.chronologicalMeasurements.iterator();
			int cnt = 0;
			double time = 0;
			double j = 0;
			while (it.hasNext()){// && cnt < 5) {
				Measurement next = it.next();
				time = next.time;
				j = next.j;
				bf2.append(time + " " + j + "\n");
				//				cnt++;
			}
			//			double windowsz = list.size();
			//			time /= windowsz;
			//			j /= windowsz;
			//			bf2.append(time + " " + j + "\n");
			//			while (it.hasNext()) {
			//				Measurement next = it.next();
			//				Measurement old = list.removeFirst();
			//				time += (next.time/windowsz - old.time/windowsz);
			//				j += (next.j/windowsz - old.j/windowsz);
			//				bf2.append(time + " " + j + "\n");
			//				list.addLast(next);
			//			}
			bf2.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.lastUpdate) {
			processFrame(event.getTime());
			this.lastUpdate = event.getTime();
			this.events.clear();
		}
		this.events.add(event);


	}

	private void processFrame(double time) {

		if (this.events.size() < 2) {
			return;
		}
		double x[] = new double[this.events.size()];
		double y[] = new double[this.events.size()];
		double minX = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		ArrayList<Cell> cells = new ArrayList<Cell>(this.events.size());
		Set<Cell> cands = new HashSet<Cell>();
		for (int i = 0; i < this.events.size(); i++) {
			XYVxVyEventImpl ev = this.events.get(i);
			Cell cell = new Cell(i);
			cells.add(cell);
			x[i] = ev.getX();
			y[i] = ev.getY();
			if (x[i] > maxX) {
				maxX = x[i];
			}
			if (x[i] < minX) {
				minX = x[i];
			}
			if (y[i] > maxY) {
				maxY = y[i];
			}
			if (y[i] < minY) {
				minY = y[i];
			}
			if (this.e.contains(MGC.xy2Point(x[i],y[i]))) {
				cands.add(cell);
			}
		}
		minX -= 2; maxX += 2; minY -= 2; maxY += 2;
		Voronoi vd = new Voronoi(0.01);
		List<GraphEdge> edges = vd.generateVoronoi(x, y, minX, maxX, minY,maxY);
		for (GraphEdge ed : edges) {
			Cell c1 = cells.get(ed.site1);
			Cell c2 = cells.get(ed.site2);
			c1.edges.add(ed);
			c2.edges.add(ed);
			
				Point p0 = MGC.xy2Point(ed.x1, ed.y1);
				Point p1 = MGC.xy2Point(ed.x2, ed.y2);
				c1.points.add(p0);
				c1.points.add(p1);
				c2.points.add(p0);
				c2.points.add(p1);
			
		}


		for (Cell cand : cands) {
			QuadTree<Coordinate> qt = new QuadTree<Coordinate>(minX, minY, maxX, maxY);
			for (GraphEdge ed : cand.edges){
				Collection<Coordinate> tmpColl = qt.getDisk(ed.x1, ed.y1, 0.0001);
				if (tmpColl.size() > 1) {
					throw new RuntimeException("needs to be handled!!");
				} else if (tmpColl.size() == 1) {
					Coordinate rm = tmpColl.iterator().next();
					qt.remove(rm.x, rm.y, rm);
				} else {
					qt.put(ed.x1, ed.y1, new Coordinate(ed.x1,ed.y1));
				}
				tmpColl = qt.getDisk(ed.x2, ed.y2, 0.0001);
				if (tmpColl.size() > 1) {
					throw new RuntimeException("needs to be handled!!");
				} else if (tmpColl.size() == 1) {
					Coordinate rm = tmpColl.iterator().next();
					qt.remove(rm.x, rm.y, rm);
				} else {
					qt.put(ed.x2, ed.y2, new Coordinate(ed.x2,ed.y2));
				}
				
				double contr = ed.x1*ed.y2 - ed.x2*ed.y1;
				double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[cand.idx], y[cand.idx], ed.x1, ed.y1) < 0 ? -1 : 1;
				contr *= leftOf;
				cand.area  += contr;
			}
			if (qt.size() > 0) {
				cand.valid = false;
			}
		}
		
		
		
		double area = 0; double v = 0; double j = 0;
		int cnt = 0;
		for (Cell cand : cands) {

			Point[] points = cand.points.toArray(new Point[0]);

			MultiPoint mp = this.geofac.createMultiPoint(points);
			Geometry hull = mp.convexHull();
			Geometry intersection = this.buffer.intersection(hull);
			//			if (!cand.valid) {
			//				continue;
			//			}
			cand.area = intersection.getArea();
			cand.hull = intersection;
			
			if (!cand.valid) {
				continue;
			}
			if (cand.area == 0) {
				cand.valid = false;
				continue;
			}
			area += cand.area;

			double vx = this.events.get(cand.idx).getVX();
			double vy = this.events.get(cand.idx).getVY();
			double sp = Math.sqrt(vx * vx + vy * vy);
			v += sp * cand.area;
			j += (sp/cand.area)*cand.area;
			cnt++;
		}

		if (area == 0) {
			return;
		}

		double rho = cnt/area;

		v /= area;
		j /= area;
		Measurement m = new Measurement();
		m.rho = rho;
		m.v = v;
		m.j = j;
		m.time = time;
		this.measurements.put(rho, m);
		this.chronologicalMeasurements.add(m);

		debug(cands);
	}

	private void debug(Collection<Cell> cands) {



		for (Cell c : cands) {
			

			int r,g,b;
			if (c.valid) {
				r = 255;
				g = 0;
				b = 0;
			} else {
				r =128;
				b=128;
				g=128;
				continue;
			}
			Geometry hull = c.hull;
			for ( int i = 1; i < hull.getCoordinates().length; i++) {
				LineSegment ls = new LineSegment();
				ls.x0 = hull.getCoordinates()[i-1].x;
				ls.y0 = hull.getCoordinates()[i-1].y;
				ls.x1 = hull.getCoordinates()[i].x;
				ls.y1 = hull.getCoordinates()[i].y;
				LineEvent l = new LineEvent(0, ls, false, r, g, b,255,0);
				this.em.processEvent(l);

			}

		}

		for ( int i = 1; i < this.buffer.getCoordinates().length; i++) {
			LineSegment ls = new LineSegment();
			ls.x0 = this.buffer.getCoordinates()[i-1].x;
			ls.y0 = this.buffer.getCoordinates()[i-1].y;
			ls.x1 = this.buffer.getCoordinates()[i].x;
			ls.y1 = this.buffer.getCoordinates()[i].y;
			LineEvent l = new LineEvent(0, ls, false, 0, 0, 255,255,0);
			this.em.processEvent(l);

		}
	}


	private boolean intersection(Coordinate c1,Coordinate c2,GraphEdge e,Coordinate intersection)
	{
		double x1 = c1.x;
		double y1 = c1.y;
		double x2 = c2.x;
		double y2 = c2.y;
		double x3 = e.x1;
		double y3 = e.y1;
		double x4 = e.x2;
		double y4 = e.y2;


		double denom  = (y4-y3) * (x2-x1) - (x4-x3) * (y2-y1);
		double numera = (x4-x3) * (y1-y3) - (y4-y3) * (x1-x3);
		double numerb = (x2-x1) * (y1-y3) - (y2-y1) * (x1-x3);

		if (Math.abs(numera) < CGAL.EPSILON && Math.abs(numerb) < CGAL.EPSILON && Math.abs(denom) < CGAL.EPSILON) {
			intersection.x = (x1 + x2) / 2;
			intersection.y = (y1 + y2) / 2;
			return true;
		}

		if (Math.abs(denom) < CGAL.EPSILON) {
			return false;
		}

		double mua = numera / denom;
		double mub = numerb / denom;
		if (mua < 0 || mua > 1 || mub < 0 || mub > 1) {
			return false;
		}
		intersection.x = x1 + mua * (x2 - x1);
		intersection.y = y1 + mua * (y2 - y1);
		return true;
	}


	private static final class Cell {
		public Geometry hull;
		boolean valid = true;
		public double area = 0;
		public Cell(int i) {
			this.idx = i;
		}
		int idx;
		List<Point> points = new ArrayList<Point>();
		List<GraphEdge> edges = new LinkedList<GraphEdge>();
		//		List<Cell> neighbors = new LinkedList<Cell>();
	}

	private static final class Measurement {
		double rho;
		double v;
		double j;
		double time;
	}

}
