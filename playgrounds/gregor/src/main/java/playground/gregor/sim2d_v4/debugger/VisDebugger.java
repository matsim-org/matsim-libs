/* *********************************************************************** *
 * project: org.matsim.*
 * VisDebugger.java
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

package playground.gregor.sim2d_v4.debugger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import processing.core.PApplet;

public class VisDebugger extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JFrame fr;

	public VisDebugger() {
		this.fr = new JFrame();
		this.fr.setSize(1000, 1000);
		this.fr.add(this,BorderLayout.CENTER);
		this.init();
		this.fr.setVisible(true);
	}

	private static final int W = 1000;

	private float scale = 5;
	private List<Object> elements = Collections.synchronizedList(new ArrayList<Object>());
	private final List<Object> newElements = new ArrayList<Object>();

	@Override
	public void setup() {
		addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
			@Override
			public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
				mouseWheel(evt.getWheelRotation());
			}}); 
		size(W,W);
		background(0);
	}

	void mouseWheel(int delta)
	{
		this.scale-=10*delta;


	}	

	@Override
	public void draw() {
		stroke(255);
		background(255);
		fill(255);

		synchronized(this.elements) {
			Iterator<Object> it = this.elements.iterator();
			while (it.hasNext()) {
				Object el = it.next();
				if (el instanceof Line) {
					drawLine((Line) el);
				} else if (el instanceof Circle) {
					drawCircle((Circle) el);
				} else if (el instanceof Polygon) {
					drawPolygon((Polygon)el);
				}
			}
		}
	}

	private void drawPolygon(Polygon p) {
		fill(p.r,p.g,p.b,p.a);
		stroke(p.r,p.g,p.b,p.a);
		beginShape();
		for (int i = 0; i < p.x.length; i++) {
			float x = scaleFl(p.x[i]);
			float y = scaleFl(p.y[i]);
			vertex(x,y);
		}
		endShape();
		
	}

	private void drawCircle(Circle c) {
		fill(c.r,c.g,c.b,c.a);
		stroke(c.r,c.g,c.b,c.a);
		ellipse(scaleFl(c.x),scaleFl(c.y),scaleFl(c.rr),scaleFl(c.rr));
	}

	private void drawLine(Line l) {
		stroke(l.r, l.g, l.b, l.a);
		//		stroke(20);
		strokeWeight(2);
		line(scaleFl(l.x0),scaleFl(l.y0),scaleFl(l.x1),scaleFl(l.y1));
		
	}

	private float scaleFl(float y1) {
		return this.scale * y1;
	}

	public void addLine(float x0, float y0, float x1, float y1, int r, int g, int b, int a) {
		Line l = new Line();
		l.x0 = x0;
		l.x1 = x1;
		l.y0 = y0;
		l.y1 = y1;
		l.r = r;
		l.g = g;
		l.b = b;
		l.a = a;
		StringBuffer buf = new StringBuffer();
		buf.append("line");
		buf.append(x0);
		buf.append(' ');
		buf.append(y0);
		buf.append(' ');
		buf.append(x1);
		buf.append(' ');
		buf.append(y1);
		addElement(l,buf.toString());

	}

	public void addCircle(float x, float y, float rr, int r, int g, int b, int a) {
		Circle c = new Circle();
		c.x = x;
		c.y = y;
		c.rr = rr;
		c.r = r;
		c.g = g;
		c.b = b;
		c.a = a;
		StringBuffer buf = new StringBuffer();
		buf.append("circle");
		buf.append(x);
		buf.append(' ');
		buf.append(y);
		buf.append(' ');
		buf.append(rr);
		addElement(c,buf.toString());
	}

	public void addPolygon(float [] x, float [] y, int r, int g, int b, int a) {
		Polygon p = new Polygon();
		p.x = x;
		p.y = y;
		p.r = r;
		p.g = g;
		p.b = b;
		p.a = a;
		StringBuffer buf = new StringBuffer();
		buf.append("polygon");
		buf.append(x);
		buf.append(' ');
		buf.append(y);
		buf.append(' ');
		addElement(p,buf.toString());
	}
	
	private void addElement(Object o, String key) {
		this.newElements.add(o);
	}

	public void update() {
		synchronized(this.elements) {
//			this.elements.clear();
			this.elements = Collections.synchronizedList(new ArrayList<Object>(this.newElements));
			this.newElements.clear();
		}
	}

	private static final class Line {
		float x0,x1,y0,y1;
		int r,g,b,a;
	}

	private static class Circle {
		float x,y,rr;
		int r,g,b,a;
	}
	
	private static class Polygon {
		float [] x;
		float [] y;
		int r,g,b,a;
	}
}
