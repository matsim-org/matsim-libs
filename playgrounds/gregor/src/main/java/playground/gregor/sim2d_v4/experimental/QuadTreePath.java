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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;

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
	private final Envelope e = new Envelope(-10,30,0,40);

	
	private double time = -1;
	private final List<XYVxVyEventImpl> events = new ArrayList<XYVxVyEventImpl>();
	
	private final Dijkstra d = new Dijkstra(new Envelope(0,20,0,20));
	
	public QuadTreePath(EventsManager em) {
		this.em = em;
	}
	
	@Override
	public void reset(int iteration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleEvent(XYVxVyEventImpl event) {
		if (event.getTime() > this.time) {
//			this.quadTree = new NavigationQuadTree(this.em,this.events);
			List<TwoDObject> objs = new ArrayList<TwoDObject>();
			
			XYVxVyEventImpl target = null;
			for (XYVxVyEventImpl ee : this.events) {
				Obj obj = new Obj(ee.getX(),ee.getY());
				objs.add(obj);
				if (ee.getPersonId().toString().startsWith("b")){
					target = ee;
				}
			}
			
//			for (double x = 0; x <= 20; x +=2) {
//				for (double y = 0; y <= 20; y+=2) {
//					Obj o = new Obj(x,y);
//					objs.add(o);
//				}
//			}
			Obj s = new Obj(0.5,10);
			Obj t = new Obj(19.5,10);
			objs.add(s);
			objs.add(t);
			LinearQuadTreeLD q = new LinearQuadTreeLD(objs,this.e,this.em);
			List<Quad> from = q.query(new Envelope(0.49,.51,9.99,10.1));
			List<Quad> to = q.query(new Envelope(19.49,19.51,9.99,10.1));
			if (target != null) {
				from = q.query(new Envelope(target.getX()-.1,target.getX()+.1,target.getY()-.1,target.getY()+.1));
			}
			if (to == null) {
				from = q.query(new Envelope(19.49,19.51,9.99,10.1));
			}
			LinkedList<Quad> path = this.d.computeShortestPath(q, from.get(0), to.get(0));
			draw(path);
			this.time = event.getTime();
			this.events.clear();
		}
		this.events.add(event);
		
		
	}
	private void draw(LinkedList<Quad> path) {
		if (path.size() == 0) {
			return;
		}
		for (Quad q : path) {
			draw(q);
		}
		
		Quad start = path.get(0);
		double x = start.getEnvelope().getMinX()+start.getEnvelope().getWidth()/2;
		double y = start.getEnvelope().getMinY()+start.getEnvelope().getHeight()/2;
	
		for (int i = 1; i < path.size(); i++) {
			Quad next = path.get(i);
			LineSegment ls = new LineSegment();
			ls.x0 = x;
			ls.y0 = y;
			x = next.getEnvelope().getMinX()+next.getEnvelope().getWidth()/2;
			y = next.getEnvelope().getMinY()+next.getEnvelope().getHeight()/2;
			ls.x1 = x;
			ls.y1 = y;
			this.em.processEvent(new LineEvent(0, ls, false, 0, 255,255, 255, 0));
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
