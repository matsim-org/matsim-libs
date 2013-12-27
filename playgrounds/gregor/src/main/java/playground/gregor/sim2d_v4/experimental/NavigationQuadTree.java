/* *********************************************************************** *
 * project: org.matsim.*
 * NavigationQuadTree.java
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
import java.util.Iterator;
import java.util.List;

import org.matsim.core.api.experimental.events.EventsManager;

import playground.gregor.sim2d_v4.cgal.LineSegment;
import playground.gregor.sim2d_v4.events.XYVxVyEventImpl;
import playground.gregor.sim2d_v4.events.debug.LineEvent;

import com.vividsolutions.jts.geom.Envelope;

public class NavigationQuadTree {
	
	private Quadrant root;
	private EventsManager em;
	
	public NavigationQuadTree(EventsManager em, List<XYVxVyEventImpl> events) {
		if (events.size() < 3) {
			return;
		}
		this.em = em;
		Iterator<XYVxVyEventImpl> it = events.iterator();
		XYVxVyEventImpl el = it.next();
		Envelope e = new Envelope(el.getX(), el.getX(), el.getY(), el.getY());
		while(it.hasNext()) {
			el = it.next();
			e.expandToInclude(el.getX(), el.getY());
		}
		
		this.root = new Quadrant(events,e,null);
		
	}
	
	
	private final class Quadrant{

		Quadrant nEast;
		Quadrant sEast;
		Quadrant sWest;
		Quadrant nWest;
		private final Envelope e;
		private XYVxVyEventImpl element;
		private final Quadrant parent;
		
		public Quadrant(List<XYVxVyEventImpl> events,Envelope e, Quadrant parent) {
			this.parent = parent;
			this.e = e;
			
			if (events.size() <= 1) {
//				this.element = events.get(0);
//				debug();
				
			} else {
				Envelope nEast = new Envelope(e.getMinX()+e.getWidth()/2,e.getMaxX(),e.getMaxY(),e.getMaxY()-e.getHeight()/2);
				Envelope sEast = new Envelope(e.getMinX()+e.getWidth()/2,e.getMaxX(),e.getMaxY()-e.getHeight()/2,e.getMinY());
				Envelope sWest = new Envelope(e.getMinX(),e.getMaxX()-e.getWidth()/2,e.getMaxY()-e.getHeight()/2,e.getMinY());
				Envelope nWest = new Envelope(e.getMinX(),e.getMaxX()-e.getWidth()/2,e.getMaxY(),e.getMaxY()-e.getHeight()/2);
				List<XYVxVyEventImpl> nEastEv = new ArrayList<XYVxVyEventImpl>();
				List<XYVxVyEventImpl> sEastEv = new ArrayList<XYVxVyEventImpl>();
				List<XYVxVyEventImpl> sWestEv = new ArrayList<XYVxVyEventImpl>();
				List<XYVxVyEventImpl> nWestEv = new ArrayList<XYVxVyEventImpl>();
				for (XYVxVyEventImpl ev : events) {
					if (nEast.contains(ev.getX(), ev.getY())) {
						nEastEv.add(ev);
					} else if (sEast.contains(ev.getX(), ev.getY())) {
						sEastEv.add(ev);
					} else if (sWest.contains(ev.getX(), ev.getY())) {
						sWestEv.add(ev);
					} else if (nWest.contains(ev.getX(), ev.getY())) {
						nWestEv.add(ev);
					}
				}
				this.nEast = new Quadrant(nEastEv, nEast,this);
				this.sEast = new Quadrant(sEastEv, sEast,this);
				this.nWest = new Quadrant(nWestEv, nWest,this);
				this.sWest = new Quadrant(sWestEv, sWest,this);
				
				if (nEastEv.size() <= 1) {
					this.nEast.debug();
				}
				if (sEastEv.size() <= 1) {
					this.sEast.debug();
				}
				if (nWestEv.size() <= 1) {
					this.nWest.debug();
				}
				if (sWestEv.size() <= 1) {
					this.sWest.debug();
				}
			}
		}

		private void debug() {
			
			
			if (this.parent.nEast != this) {
				Envelope ee = this.parent.nEast.e;
				LineSegment ls0 = new LineSegment();
				ls0.x0 = this.e.getMaxX()-this.e.getWidth()/2;
				ls0.y0 = this.e.getMaxY()-this.e.getHeight()/2;
				ls0.x1 = ee.getMaxX()-ee.getWidth()/2;
				ls0.y1 = ee.getMaxY()-ee.getHeight()/2;
				NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls0, false, 255,0,0,255,0));
			} 
			if (this.parent.sEast != this) {
				Envelope ee = this.parent.sEast.e;
				LineSegment ls0 = new LineSegment();
				ls0.x0 = this.e.getMaxX()-this.e.getWidth()/2;
				ls0.y0 = this.e.getMaxY()-this.e.getHeight()/2;
				ls0.x1 = ee.getMaxX()-ee.getWidth()/2;
				ls0.y1 = ee.getMaxY()-ee.getHeight()/2;
				NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls0, false, 255,0,0,255,0));
			} 
			if (this.parent.nWest != this) {
				Envelope ee = this.parent.nWest.e;
				LineSegment ls0 = new LineSegment();
				ls0.x0 = this.e.getMaxX()-this.e.getWidth()/2;
				ls0.y0 = this.e.getMaxY()-this.e.getHeight()/2;
				ls0.x1 = ee.getMaxX()-ee.getWidth()/2;
				ls0.y1 = ee.getMaxY()-ee.getHeight()/2;
				NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls0, false, 255,0,0,255,0));
			} 
			if (this.parent.sWest != this) {
				Envelope ee = this.parent.sWest.e;
				LineSegment ls0 = new LineSegment();
				ls0.x0 = this.e.getMaxX()-this.e.getWidth()/2;
				ls0.y0 = this.e.getMaxY()-this.e.getHeight()/2;
				ls0.x1 = ee.getMaxX()-ee.getWidth()/2;
				ls0.y1 = ee.getMaxY()-ee.getHeight()/2;
				NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls0, false, 255,0,0,255,0));
			} 
			
			
//			LineSegment ls0 = new LineSegment();
//			ls0.x0 = this.e.getMaxX();
//			ls0.y0 = this.e.getMaxY();
//			ls0.x1 = this.e.getMaxX();
//			ls0.y1 = this.e.getMinY();
//			
//			LineSegment ls1 = new LineSegment();
//			ls1.x0 = this.e.getMaxX();
//			ls1.y0 = this.e.getMinY();
//			ls1.x1 = this.e.getMinX();
//			ls1.y1 = this.e.getMinY();
//
//			LineSegment ls2 = new LineSegment();
//			ls2.x0 = this.e.getMinX();
//			ls2.y0 = this.e.getMinY();
//			ls2.x1 = this.e.getMinX();
//			ls2.y1 = this.e.getMaxY();
//			
//			LineSegment ls3 = new LineSegment();
//			ls3.x0 = this.e.getMinX();
//			ls3.y0 = this.e.getMaxY();
//			ls3.x1 = this.e.getMaxX();
//			ls3.y1 = this.e.getMaxY();
//			
//			NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls0, false, 255,0,0,255,0));
//			NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls1, false, 255,0,0,255,0));
//			NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls2, false, 255,0,0,255,0));
//			NavigationQuadTree.this.em.processEvent(new LineEvent(0, ls3, false, 255,0,0,255,0));
			
			
		}
		
	}

}
