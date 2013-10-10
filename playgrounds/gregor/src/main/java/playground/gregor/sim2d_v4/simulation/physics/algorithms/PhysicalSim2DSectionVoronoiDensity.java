/* *********************************************************************** *
 * project: org.matsim.*
 * SectionVoronoiDensity.java
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

package playground.gregor.sim2d_v4.simulation.physics.algorithms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection;
import playground.gregor.sim2d_v4.simulation.physics.PhysicalSim2DSection.Segment;
import playground.gregor.sim2d_v4.simulation.physics.Sim2DAgent;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

public class PhysicalSim2DSectionVoronoiDensity {

	private final PhysicalSim2DSection pSec;
	private final Envelope bounds;
	private final double offsetX;
	private final double offsetY;
	private final List<Segment> obstacles = new ArrayList<Segment>();
	private ArrayList<Cell> cells;

	//TODO look for a more efficient way ...
	private final Map<Sim2DAgent,Cell> agentCellMapping = new HashMap<Sim2DAgent,Cell>();
	
	
	public PhysicalSim2DSectionVoronoiDensity(PhysicalSim2DSection pSec){
		this.pSec = pSec;
		this.bounds = new Envelope(pSec.getObstacles().get(0).x0,pSec.getObstacles().get(0).x1,pSec.getObstacles().get(0).y0,pSec.getObstacles().get(0).y1);
		for (Segment o : pSec.getOpenings()) {
			this.bounds.expandToInclude(o.x0-2*o.dy, o.y0+2*o.dx);
			this.bounds.expandToInclude(o.x1-2*o.dy, o.y1+2*o.dx);
		}

		for (Segment o : pSec.getObstacles()) {
			this.bounds.expandToInclude(o.x0, o.y0);
			this.bounds.expandToInclude(o.x1, o.y1);
		}

		this.offsetX = -this.bounds.getMinX();
		this.offsetY = -this.bounds.getMinY();
		this.bounds.translate(this.offsetX, this.offsetY);
		for (Segment o : this.pSec.getObstacles()) {
			Segment s = new Segment();
			s.x0 = o.x0 + this.offsetX;
			s.x1 = o.x1 + this.offsetX;
			s.y0 = o.y0 + this.offsetY;
			s.y1 = o.y1 + this.offsetY;
			this.obstacles.add(s);
		}
	}

	public void buildDensityMap() {
		List<Sim2DAgent> agents = new ArrayList<Sim2DAgent>(this.pSec.getAgents());
		if (agents.size() == 0) {
			return;
		}
		this.agentCellMapping.clear();

		for (Segment o : this.pSec.getOpenings()) {
			PhysicalSim2DSection n = this.pSec.getNeighbor(o);
			//FIXME repair this!!
			if (n == null) {
				continue;
			}
			
			for (Sim2DAgent a : n.getAgents()) {
				if (this.bounds.contains(a.getPos()[0], a.getPos()[1])) {
					agents.add(a);
				}
			}
		}

		double [] x = new double[agents.size()];
		double [] y = new double[agents.size()];
		int idx = 0;
		for (Sim2DAgent a : agents) {
			x[idx] = a.getPos()[0]+this.offsetX;
			y[idx] = a.getPos()[1]+this.offsetY;
			idx++;
		}
		Voronoi vd = new Voronoi(CGAL.EPSILON);
		List<GraphEdge> edges = vd.generateVoronoi(x, y,this.bounds.getMinX(),this.bounds.getMaxX(),this.bounds.getMinY(),this.bounds.getMaxY());

		this.cells = new ArrayList<Cell>(x.length);
		for (int i = 0; i < agents.size(); i++) {
			Cell c = new Cell();
			c.vx = x[i];
			c.vy = y[i];
			c.agent = agents.get(i);
			this.cells.add(c);
			this.agentCellMapping.put(c.agent, c);
		}
		for (GraphEdge e : edges) {

			if (e.x1 == e.x2 && e.y1 == e.y2) {
				//				System.out.println("err");
				continue;
			}




			Cell c1 = this.cells.get(e.site1);
			Cell c2 = this.cells.get(e.site2);
			c1.neighbors.add(e.site2);
			c2.neighbors.add(e.site1);
			
			intersection(c1,c2,e);

			double contr = e.x1*e.y2 - e.x2*e.y1;
			double leftOf = CGAL.isLeftOfLine(e.x2, e.y2, x[e.site1], y[e.site1], e.x1, e.y1) < 0 ? -1 : 1;
			contr *= leftOf;
			c1.area += contr/2;
			c2.area -= contr/2;
			
//			c1.contrs++;
//			c2.contrs++;
//			if (Math.abs(contr) > 10) {
//				c1.area = Double.POSITIVE_INFINITY;
//				c2.area = Double.POSITIVE_INFINITY;
//			}
//			System.out.println(Math.abs(contr));
			
//			if (!c1.xCoords.remove(e.x1)) {
//				c1.xCoords.add(e.x1);
//			}
//			if (!c1.xCoords.remove(e.x2)) {
//				c1.xCoords.add(e.x2);
//			}
//			if (!c2.xCoords.remove(e.x1)) {
//				c2.xCoords.add(e.x1);
//			}
//			if (!c2.xCoords.remove(e.x2)) {
//				c2.xCoords.add(e.x2);
//			}			
//			
			
			//centroid computation
//			double cntr1x = (e.x1+e.x2)*(e.x1*e.y2 - e.x2*e.y1);
//			double cntr1y = (e.y1+e.y2)*(e.x1*e.y2 - e.x2*e.y1);
//			c1.cx += leftOf*cntr1x;
//			c1.cy += leftOf*cntr1y;
//			c2.cx -= leftOf*cntr1x;
//			c2.cy -= leftOf*cntr1y;
			
			
			
			
			//DEBUG
			Segment seg = new Segment();
			c1.segments.add(seg);
			c2.segments.add(seg);
			seg.x0 = e.x1 - this.offsetX;
			seg.x1 = e.x2 - this.offsetX;
			seg.y0 = e.y1 - this.offsetY;
			seg.y1 = e.y2 - this.offsetY;

		}

		
		//		debug();
		//		debug(edges);
		debug(this.cells,true);
	}

	//find obstacle (wall) intersections and handle them accordingly
	private void intersection(Cell c1, Cell c2, GraphEdge e) {
		double dxu = e.x2 - e.x1;
		double dyu = e.y2 - e.y1;
		for (int i = 0; i < this.obstacles.size(); i++) {
			Segment o = this.obstacles.get(i);
			double lft0 = CGAL.isLeftOfLine(e.x1, e.y1, o.x0, o.y0, o.x1, o.y1);
			double lft1 = CGAL.isLeftOfLine(e.x2, e.y2, o.x0, o.y0, o.x1, o.y1);
			if (lft0 * lft1 <= 0) { //in case of convex polygons we've an intersection here; otherwise it is not guaranteed
				double dxv = o.x1 - o.x0;
				double dyv = o.y1 - o.y0;
				double dxw = e.x1 - o.x0;
				double dyw = e.y1 - o.y0;
				double d = CGAL.perpDot(dxu, dyu, dxv, dyv);
				double c = CGAL.perpDot(dxv, dyv, dxw, dyw)/d;
				if (c < 0 || c > 1) {
					continue;
				}
				double cc = CGAL.perpDot(dxu, dyu, dxw, dyw)/d;
				if (cc < 0 || cc > 1) {
					continue;
				}

				double xx = e.x1 + dxu * c;
				double yy = e.y1 + dyu * c;
				if (lft0 >= 0) {
					e.x1 = xx;
					e.y1 = yy;
				} else {
					e.x2 = xx;
					e.y2 = yy;
				}
				
				handleContribution(c1,i,xx,yy);
				handleContribution(c2,i,xx,yy);
				
				//					break;
			}
		}
		
	}

	//intersection with an obstacle (wall)
	private void handleContribution(Cell c1, int i, double xx,
			double yy) {
		Coordinate intersec1 = c1.intersectsions.remove(i);
		if (intersec1 == null) {
			intersec1 = new Coordinate(xx,yy);
			c1.intersectsions.put(i, intersec1);
		} else {
			double cntr = xx *intersec1.y - intersec1.x * yy;
			double lft = CGAL.isLeftOfLine(intersec1.x, intersec1.y,c1.vx, c1.vy, xx, yy) < 0? -1 : 1;
			cntr *= lft;
			c1.area += cntr/2;
			
			double cntr1x = (xx+intersec1.x)*(xx*intersec1.y - intersec1.x*yy);
			double cntr1y = (yy+intersec1.y)*(xx*intersec1.y - intersec1.x*yy);
			c1.cx += lft*cntr1x;
			c1.cy += lft*cntr1y;
			
			//DEBUG
			Segment s = new Segment();
			s.x0 = xx - this.offsetX;
			s.x1 = intersec1.x - this.offsetX;
			s.y0 = yy - this.offsetY;
			s.y1 = intersec1.y - this.offsetY;
			c1.segments.add(s);
		}
		
	}
	
	public Cell getCell(int i) {
		
		Cell ret = this.cells.get(i);
		
//		//centroid computation
		if (!ret.finalized) {
			if (ret.contrs < 3) {
				ret.area = Double.POSITIVE_INFINITY;
			}
//			if (ret.xCoords.size() > 0) {
//				ret.area = 5.4;
//			}
//////			s.x0 = c.vx-this.offsetX;
//////			s.y0 = c.vy-this.offsetY;
////			ret.cx = ret.cx/(6*ret.area)-this.offsetX;
////			ret.cy = ret.cy/(6*ret.area)-this.offsetY;
//			ret.finalized = true;
		}
		return ret;
		
	}
	
	public Cell getCell(Sim2DAgent agent) {
		return this.agentCellMapping.get(agent);
	}
	
	private void debug(List<Cell> cells,boolean dummy) {
		EventsManager em = this.pSec.getPhysicalEnvironment().getEventsManager();
		for (Cell c : cells) {

			
			for (Segment edge : c.segments) {
				em.processEvent(new LineEvent(0, edge, false, 0, 255, 255, 255, 0, .8, .2));
			}
//			if (c.area < 1/50. || c.area > 100.) {
//				continue;
//			}
//			Segment s = new Segment();
//			s.x0 = c.vx-this.offsetX;
//			s.y0 = c.vy-this.offsetY;
//			s.x1 = c.cx/(6*c.area)-this.offsetX;
//			s.y1 = c.cy/(6*c.area)-this.offsetY;
//			System.out.println(c.area);
//			em.processEvent(new LineEvent(0, s, false, 255, 0, 255, 255, 0, .8, .2));
//			em.processEvent(new CircleEvent(0,s.x1,s.y1));
		}

	}

	private void debug(List<GraphEdge> edges) {
		EventsManager em = this.pSec.getPhysicalEnvironment().getEventsManager();
		for (GraphEdge e : edges) {
			Segment seg = new Segment();
			seg.x0 = e.x1;
			seg.y0 = e.y1;
			seg.x1 = e.x2;
			seg.y1 = e.y2;
			em.processEvent(new LineEvent(0, seg, false,0,255,255,128,0,.2,.2));
		}

	}

	private void debug() {
		EventsManager em = this.pSec.getPhysicalEnvironment().getEventsManager();
		Segment seg0 = new Segment();
		seg0.x0 = this.bounds.getMaxX();
		seg0.y0 = this.bounds.getMaxY();
		seg0.x1 = this.bounds.getMinX();
		seg0.y1 = this.bounds.getMaxY();
		Segment seg1 = new Segment();
		seg1.x0 = this.bounds.getMinX();
		seg1.y0 = this.bounds.getMaxY();
		seg1.x1 = this.bounds.getMinX();
		seg1.y1 = this.bounds.getMinY();
		Segment seg2 = new Segment();
		seg2.x0 = this.bounds.getMinX();
		seg2.y0 = this.bounds.getMinY();
		seg2.x1 = this.bounds.getMaxX();
		seg2.y1 = this.bounds.getMinY();
		Segment seg3 = new Segment();
		seg3.x0 = this.bounds.getMaxX();
		seg3.y0 = this.bounds.getMinY();
		seg3.x1 = this.bounds.getMaxX();
		seg3.y1 = this.bounds.getMaxY();		

		em.processEvent(new LineEvent(0, seg0, false,0,0,255,255,0,.5,.2));
		em.processEvent(new LineEvent(0, seg1, false,0,0,255,255,0,.5,.2));
		em.processEvent(new LineEvent(0, seg2, false,0,0,255,255,0,.5,.2));
		em.processEvent(new LineEvent(0, seg3, false,0,0,255,255,0,.5,.2));

	}

	public static final class Cell {

		public int contrs = 0;

		public Sim2DAgent agent;

		private final boolean finalized = false;
		
		private final Set<Double> xCoords = new HashSet<Double>();
		
		public Map<Integer,Coordinate> intersectsions = new HashMap<Integer,Coordinate>();
		public List<Integer> neighbors = new ArrayList<Integer>();
		public double vy;
		public double vx;
		// area of a simple polygon A = 1/2 * sum_{i=0}^{n-1} (x_i*y_{i+1}-x_{i+1}*y_i)
		public double area;
		List<Segment> segments = new ArrayList<Segment>();
		//centroid of a simple polygon is 	cx = 1/(6*A) * sum_{i=0}^{n-1}(x_i+x_{i+1})*(x_i*y_{i+1} - x_{i+1) * y_i)
		//									cy = 1/(6*A) * sum_{i=0}^{n-1}(y_i+y_{i+1})*(y_i*x_{i+1} - y_{i+1) * x_i)
		double cx;
		double cy;

	}
	//	private void init() {
	//		
	//		
	//	}

}
