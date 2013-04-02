/* *********************************************************************** *
 * project: org.matsim.*
 * DecompGuiDebugger.java
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

package playground.gregor.sim2denvironment.approxdecomp;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import processing.core.PApplet;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;


public class DecompGuiDebugger extends JFrame {
	
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DecompGuiDebugger() {
		setSize(1000, 1000);
		GuiDebugger dbg = new GuiDebugger();
		add(dbg,BorderLayout.CENTER);
		dbg.init();
	}
	
	
	/*package*/ static final class GuiDebugger extends PApplet {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private static final List<Object> objects = new ArrayList<Object>();
		
		private static final int W = 1000;

		private double scale = 500;
		
		@Override
		public void setup() {
			addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
				@Override
				public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
					mouseWheel(evt.getWheelRotation());
				}});
			addMouseMotionListener(new MouseMotionListener() {
				
				@Override
				public void mouseMoved(MouseEvent e) {
					draw();
				}
				
				@Override
				public void mouseDragged(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
			});
			size(W,W);
			background(0);
		}
		
		void mouseWheel(int delta)
		{
			this.scale-=10*delta;
			System.out.println(this.scale);
			draw();
		}
		
	
		
		@Override
		public void draw() {
			stroke(0);
			background(255);
			for (int i = 0; i < objects.size(); i++) {
				stroke(0);
				Object o = objects.get(i);
				if (i == objects.size()-1) {//newst in red
					stroke(192f,0.f,0.f);
				}
				if (o instanceof Polygon) {
					drawPolygon((Polygon)o);
				} else if (o instanceof MultiPolygon) {
					drawMultiPolygon((MultiPolygon)o);
				} else if (o instanceof CoordinatePair) {
					stroke(0.f,193.f,0.f);
					drawSegment(((CoordinatePair) o).c0,((CoordinatePair) o).c1);
				}
			}
		}

		private void drawMultiPolygon(MultiPolygon o) {
			for (int i = 0; i < o.getNumGeometries(); i++) {
				drawPolygon((Polygon)o.getGeometryN(i));
			}
			
		}

		private void drawPolygon(Polygon o) {
			Coordinate[] shell = o.getExteriorRing().getCoordinates();
			for (int i = 1; i < shell.length; i++) {
				drawSegment(shell[i-1],shell[i]);
			}
		}

		private void drawSegment(Coordinate c0, Coordinate c1) {
			float x0 = getFloat(c0.x);
			float y0 = getFloat(c0.y);
			float x1 = getFloat(c1.x);
			float y1 = getFloat(c1.y);
			line(x0,y0,x1,y1);
		}

		private float getFloat(double x) {
			float scaled = (float) (this.scale/10 * x);
			return scaled + W/2;
		}
		
		
		public static void addObject(Object o) {
			objects.add(o);
		}

		public static void addCoordinatePair(Coordinate c0, Coordinate c1) {
			CoordinatePair cp = new CoordinatePair();
			cp.c0 = c0;
			cp.c1 = c1;
			objects.add(cp);
		}
		
		private static final class CoordinatePair {
			Coordinate c0;
			Coordinate c1;
		}
		
	}
	
}
