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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;

import org.matsim.core.utils.misc.Time;

import processing.core.PApplet;

public class VisDebugger extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JFrame fr;

	public long lastUpdate = -1;
	
	public VisDebugger() {
		this.fr = new JFrame();
		this.fr.setSize(1000, 1000);
		this.fr.add(this,BorderLayout.CENTER);
		this.init();
		this.fr.setVisible(true);
	}

	private static final int W = 1000;

	private float scale = 50;
	private List<Object> elements = Collections.synchronizedList(new ArrayList<Object>());
	private final List<Object> elementsStatic = Collections.synchronizedList(new ArrayList<Object>());
	private final List<Object> newElements = new ArrayList<Object>();
	protected int dragX = 0;
	protected int dragY = 0;
	protected int mx = 0;
	protected int my = 0;
	protected int omy;
	protected int omx;
	
	private boolean first = true;
	private String time = "00:00.00.0";
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
//				System.out.println(e.getX() + " " + e.getY());
				
			}
			
			@Override
			public void mouseDragged(MouseEvent e) {
				VisDebugger.this.dragX = e.getX()-VisDebugger.this.mx;
				VisDebugger.this.dragY = e.getY()-VisDebugger.this.my;
//				System.out.println(VisDebugger.this.dragX + " " + VisDebugger.this.dragY);
				
			}
		});
		addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
				VisDebugger.this.omx += VisDebugger.this.dragX; 
				VisDebugger.this.dragX = 0;
				VisDebugger.this.omy += VisDebugger.this.dragY; 
				VisDebugger.this.dragY = 0;
				System.out.println(VisDebugger.this.omx + " " + VisDebugger.this.omy);
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
				VisDebugger.this.mx = arg0.getX();
				VisDebugger.this.my = arg0.getY();
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
		});
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

		synchronized(this.elementsStatic) {
			Iterator<Object> it = this.elementsStatic.iterator();
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
				} else if (el instanceof Text) {
					drawText((Text)el);
				}
			}
		}
		drawTime();
	}

	private void drawText(Text el) {
		stroke(el.r,el.g,el.b,el.a);
		fill(el.r,el.g,el.b,el.a);
		text(el.text,scaleFlX(el.x),scaleFlY(el.y));
	}

	private void drawTime() {
		String strTime = setTime(-1);
		fill(128, 128);
		stroke(0);
		rect(0, 0, 105, 25);
		fill(0);
		text(strTime, 10, 18);
		
	}

	private void drawPolygon(Polygon p) {
		fill(p.r,p.g,p.b,p.a);
		stroke(p.r,p.g,p.b,p.a);
		beginShape();
		for (int i = 0; i < p.x.length; i++) {
			float x = scaleFlX(p.x[i]);
			float y = scaleFlY(p.y[i]);
			vertex(x,y);
		}
		endShape();
		
	}

	private void drawCircle(Circle c) {
		fill(c.r,c.g,c.b,c.a);
		stroke(c.r,c.g,c.b,c.a);
		ellipse(scaleFlX(c.x),scaleFlY(c.y),scaleFl(c.rr),scaleFl(c.rr));
	}

	private void drawLine(Line l) {
		stroke(l.r, l.g, l.b, l.a);
		//		stroke(20);
		strokeWeight(2);
		line(scaleFlX(l.x0),scaleFlY(l.y0),scaleFlX(l.x1),scaleFlY(l.y1));
		
	}

	private float scaleFl(float y1) {
		return this.scale/10 * y1;
	}
	
	private float scaleFlX(float y1) {
		return this.scale/10 * y1 + this.dragX + this.omx;
	}

	private float scaleFlY(float y1) {
		return 50-this.scale/10 * y1 + this.dragY + this.omy;
	}
	public void addLineStatic(float x0, float y0, float x1, float y1, int r, int g, int b, int a) {
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
		addElementStatic(l,buf.toString());

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
	
	public void addText(float x, float y, String string) {
		Text text = new Text();
		text.x = x;
		text.y = y;
		text.text = string;
		text.a = 255;
		addElement(text, string);
	}
	
	private void addElement(Object o, String key) {
		this.newElements.add(o);
	}
	private void addElementStatic(Object o, String key) {
		synchronized(this.elementsStatic){
			this.elementsStatic.add(o);
		}
	}

	public void update() {
		synchronized(this.elements) {
//			this.elements.clear();
			this.elements = Collections.synchronizedList(new ArrayList<Object>(this.newElements));
			this.newElements.clear();
		}
		this.first = false;
	}

	public boolean isFirst() {
		return this.first;
	}

	

	
	private static final class Text {
		float x,y;
		String text;
		int r,g,b,a;
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

	synchronized public String setTime(double time2) {
		if (time2 > 0) {
			String strTime = Time.writeTime(time2, Time.TIMEFORMAT_HHMMSSDOTSS);
			if (strTime.length() > 11) {
				strTime = strTime.substring(0, 11);
			} else if (strTime.length() < 11) {
				strTime = strTime + "0";
			}
			this.time = strTime;
		}
		return this.time;
		
	}


}
