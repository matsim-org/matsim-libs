/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreePath.java
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.gbl.MatsimRandom;

import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD;
import playground.gregor.sim2d_v4.cgal.LinearQuadTreeLD.Quad;
import playground.gregor.sim2d_v4.cgal.TwoDObject;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.XYVxVyEventsHandler;
import playground.gregor.sim2d_v4.events.debug.LineEvent;
import playground.gregor.sim2d_v4.events.debug.RectEvent;

import com.vividsolutions.jts.geom.Envelope;

public class QuadTreePath implements XYVxVyEventsHandler{


	private final EventsManager em;
	private final Envelope e = new Envelope(-6,5,-6,5);


	private double time = -1;
	private final List<XYVxVyEventImpl> events = new ArrayList<XYVxVyEventImpl>();

	private final Dijkstra d = new Dijkstra(new Envelope(-6,5,-3,5));
	private final BufferedWriter bf;

	public QuadTreePath(EventsManager em, String outFile) {
		this.em = em;
		try {
			this.bf = new BufferedWriter(new FileWriter(new File (outFile)));
			this.bf.append("#time id x y qx qy\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void finish() {
		try {
			this.bf.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void reset(int iteration) {
		this.time = -1;

	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.time) {
			//			this.quadTree = new NavigationQuadTree(this.em,this.events);
			List<TwoDObject> objs = new ArrayList<TwoDObject>();

			XYVxVyEventImpl target = null;
			for (XYVxVyEventImpl ee : this.events) {
				objs.add(ee);
			}
			if (objs.size() < 4) {
				this.time = event.getTime();
				this.events.clear();

				this.events.add(event);
				return;
			}

			//			for (double x = 0; x <= 20; x +=2) {
			//				for (double y = 0; y <= 20; y+=2) {
			//					Obj o = new Obj(x,y);
			//					objs.add(o);
			//				}
			//			}
			Obj s = new Obj(-5,-2);
			Obj t = new Obj(-1,4);
			objs.add(s);
			objs.add(t);
			LinearQuadTreeLD q = new LinearQuadTreeLD(objs,this.e,this.em);
			List<Quad> to = q.query(new Envelope(-0.9,-1.1,3.9,4.1));


			for (XYVxVyEventImpl ee : this.events) {
				String frame = ee.getTime() + " " + ee.getPersonId() + " " + ee.getX() + " " + ee.getY() + " ";
				
				try {
					this.bf.append(frame);
					this.bf.append(ee.getX() + " " + ee.getY() + "\n");
				} catch (IOException e1) {
					throw new RuntimeException(e1);
				}
				List<Quad> from = q.query(new Envelope(ee.getX()-.1,ee.getX()+.1,ee.getY()-.1,ee.getY()+.1));
				Quad f = null;
				for (Quad qq : from){
					if (qq.getEnvelope().contains(ee.getX(), ee.getY()) && qq.getColor() == LinearQuadTreeLD.BLACK) {
//						if (qq.getColor() != LinearQuadTreeLD.BLACK) {
//							throw new RuntimeException("error!!");
//						}
						f = qq;
					}
				}
				LinkedList<Quad> path = this.d.computeShortestPath(q,f, to.get(0));
				for (Quad pq : path) {
					try {
						this.bf.append(frame);
						double x = pq.getEnvelope().getMaxX() - pq.getEnvelope().getWidth()/2;
						double y = pq.getEnvelope().getMaxY() - pq.getEnvelope().getHeight()/2;
						this.bf.append(x + " " + y + "\n");
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					}	
				}
				draw(ee,path);
			}

			this.time = event.getTime();
			this.events.clear();
		}
		this.events.add(event);


	}
	private void draw(XYVxVyEventImpl ee, LinkedList<Quad> path) {
		if (path.size() == 0) {
			return;
		}
		//		for (Quad q : path) {
		//			draw(q);
		//		}

		Quad start = path.get(0);
		double x = start.getEnvelope().getMinX()+start.getEnvelope().getWidth()/2;
		double y = start.getEnvelope().getMinY()+start.getEnvelope().getHeight()/2;
		
		LineSegment first = new LineSegment();
		first.x0 = ee.getX();
		first.y0 = ee.getY();
		first.x1 = x;
		first.y1 = y;
		
		this.em.processEvent(new LineEvent(0, first, false, 192, 0,0, 255, 0));
		for (int i = 1; i < path.size(); i++) {
			Quad next = path.get(i);
			LineSegment ls = new LineSegment();
			ls.x0 = x+MatsimRandom.getRandom().nextDouble()*0.1-0.05;
			ls.y0 = y+MatsimRandom.getRandom().nextDouble()*0.1-0.05;;
			x = next.getEnvelope().getMinX()+next.getEnvelope().getWidth()/2;
			y = next.getEnvelope().getMinY()+next.getEnvelope().getHeight()/2;
			ls.x1 = x+MatsimRandom.getRandom().nextDouble()*0.1-0.05;;
			ls.y1 = y+MatsimRandom.getRandom().nextDouble()*0.1-0.05;;
			this.em.processEvent(new LineEvent(0, ls, false, 192, 0,0, 255, 0));
		}

	}
	private void draw(Quad quad) {
		RectEvent re = new RectEvent(this.time, quad.getEnvelope().getMinX(), quad.getEnvelope().getMaxY(), quad.getEnvelope().getWidth(), quad.getEnvelope().getHeight(), false);
		this.em.processEvent(re);

	}
	public static final class Obj implements TwoDObject {

		double x;
		double y;

		public Obj(double x, double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public double getX() {
			return this.x;
		}

		@Override
		public double getY() {
			return this.y;
		}

	}

}
