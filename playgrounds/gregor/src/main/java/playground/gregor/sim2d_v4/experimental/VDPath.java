/* *********************************************************************** *
 * project: org.matsim.*
 * VDPath.java
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

package playground.gregor.sim2d_v4.experimental;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.utils.collections.QuadTree;

import playground.gregor.sim2d_v4.cgal.CGAL;
import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import be.humphreys.simplevoronoi.GraphEdge;
import be.humphreys.simplevoronoi.Voronoi;

import com.vividsolutions.jts.geom.Coordinate;

public class VDPath implements XYVxVyEventsHandler {

	private double time = -1;
	private final List<XYVxVyEventImpl> events = new ArrayList<XYVxVyEventImpl>();
	private final EventsManager em;


	public VDPath(EventsManager em) {
		this.em = em;
	}

	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.time) {
			processFrame();
			this.time = event.getTime();
			this.events.clear();
		}
		this.events.add(event);

	}

	private void processFrame() {
		if (this.events.size() < 2) {
			return;
		}
		double x[] = new double[this.events.size()+10];
		double y[] = new double[this.events.size()+10];
		double minX = 0;
		double maxX = 20;
		double minY = 0;
		double maxY = 40;
		ArrayList<Cell> cells = new ArrayList<Cell>(this.events.size());
		int i = 0;
		for (; i < this.events.size(); i++) {
			XYVxVyEventImpl ev = this.events.get(i);
			Cell cell = new Cell(i);
			cells.add(cell);
			x[i] = ev.getX();
			y[i] = ev.getY();
		}
//		i++;
		x[i] = 0;
		y[i] = 0;
		Cell cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 0;
		y[i] = 36;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 0;
		y[i] = 38;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 0;
		y[i] = 40;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 20;
		y[i] = 40;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 20;
		y[i] = 38;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 20;
		y[i] = 36;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 20;
		y[i] = 4;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 20;
		y[i] = 2;
		cell = new Cell(i);
		cells.add(cell);
		i++;
		x[i] = 20;
		y[i] = 0;
		cell = new Cell(i);
		cells.add(cell);
//		minX -= 2; maxX += 2; minY -= 2; maxY += 2;
		Voronoi vd = new Voronoi(0.01);
		List<GraphEdge> edges = vd.generateVoronoi(x, y, minX, maxX, minY,maxY);
		for (GraphEdge ed : edges) {
			Cell c1 = cells.get(ed.site1);
			Cell c2 = cells.get(ed.site2);
			c1.edges.add(ed);
			c2.edges.add(ed);
			c1.neighbors.add(c2);
			c2.neighbors.add(c1);


		}


		for (Cell c : cells) {
			QuadTree<Coordinate> qt = new QuadTree<Coordinate>(minX, minY, maxX, maxY);
			for (GraphEdge ed : c.edges){
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
				double leftOf = CGAL.isLeftOfLine(ed.x2, ed.y2, x[c.idx], y[c.idx], ed.x1, ed.y1) < 0 ? -1 : 1;
				contr *= leftOf;
				c.area  += contr;
				c.cx = (ed.x1+ed.x2)*contr;
				c.cy = (ed.y1+ed.y2)*contr;
			}
			if (qt.size() > 0) {
//				c.valid  = false;
				c.area = 10;
				int cnt = 0;
				c.cx = 0;
				c.cy = 0;
				for (GraphEdge ed : c.edges) {
					c.cx += ed.x1;
					c.cx += ed.x2;
					c.cy += ed.y1;
					c.cy += ed.y2;
					cnt += 2;
				}
				c.cx /= cnt;
				c.cy /= cnt;
			} else {
//				cell.area /= 2;
//				cell.cx /= 6*cell.area;
//				cell.cy /= 6*cell.area;
				c.area = 10;
				int cnt = 0;
				c.cx = 0;
				c.cy = 0;
				for (GraphEdge ed : c.edges) {
					c.cx += ed.x1;
					c.cx += ed.x2;
					c.cy += ed.y1;
					c.cy += ed.y2;
					cnt += 2;
				}
				c.cx /= cnt;
				c.cy /= cnt;
			}
		}

		debug(cells);
	}

	private void debug(ArrayList<Cell> cells) {
		for (Cell cell : cells) {
			if(!cell.valid){
				continue;
			}
			for (GraphEdge ed : cell.edges) {
				LineSegment ls = new LineSegment();
				ls.x0 = ed.x1;
				ls.x1 = ed.x2;
				ls.y0 = ed.y1;
				ls.y1 = ed.y2;
				LineEvent e = new LineEvent(0, ls, false);
				this.em.processEvent(e);
			}
			for (Cell n : cell.neighbors) {
				if (!n.valid) {
					continue;
				}
				LineSegment ls = new LineSegment();
				ls.x0 = cell.cx;
				ls.y0 = cell.cy;
				ls.x1 = n.cx;
				ls.y1 = n.cy;
				LineEvent e = new LineEvent(this.time, ls, false, 255, 0, 0, 255, 0);
				this.em.processEvent(e);
			}
		}

	}

	private static final class Cell {
		public boolean valid = true;
		public double cy;
		public double cx;
		public double area = 0;
		public Cell(int i) {
			this.idx = i;
		}
		int idx;
		List<GraphEdge> edges = new LinkedList<GraphEdge>();
		List<Cell> neighbors = new LinkedList<Cell>();
	}
}
