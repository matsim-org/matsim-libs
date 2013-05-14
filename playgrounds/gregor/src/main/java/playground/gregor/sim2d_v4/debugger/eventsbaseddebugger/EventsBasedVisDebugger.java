/* *********************************************************************** *
 * project: org.matsim.*
 * EventsBasedVisDebugger.java
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

package playground.gregor.sim2d_v4.debugger.eventsbaseddebugger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.gicentre.utils.move.ZoomPan;
import org.matsim.api.core.v01.Scenario;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

public class EventsBasedVisDebugger extends PApplet {


	private final JFrame fr;

	private List<Object> elements = Collections.synchronizedList(new ArrayList<Object>());
	private List<Text> elementsText = Collections.synchronizedList(new ArrayList<Text>());
	private final List<Object> newElements = new ArrayList<Object>();
	private final List<Text> newElementsText = new ArrayList<Text>();
	private final List<Object> elementsStatic = Collections.synchronizedList(new ArrayList<Object>());
	
	private final List<VisDebuggerAdditionalDrawer> additionalDrawers = new ArrayList<VisDebuggerAdditionalDrawer>();

	//	private final List<VisDebuggerAdditionalDrawer> additionalDrawers = new ArrayList<VisDebuggerAdditionalDrawer>();

	//TODO make this dynamic!!
	final double offsetX = -1113948;
	final double offsetY = -7041222;

	ZoomPan zoomer;
	private final TileMap tileMap;	
	public EventsBasedVisDebugger(Scenario sc) {
		this.fr = new JFrame();
		this.fr.setSize(1024,788);
		JPanel compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));

		this.fr.add(compositePanel,BorderLayout.CENTER);

		compositePanel.add(this);
		compositePanel.setEnabled(true);
		compositePanel.setVisible(true);

		this.zoomer = new ZoomPan(this);  // Initialise the zoomer.
		this.tileMap = new TileMap(this.zoomer,this, this.offsetX, this.offsetY, sc.getConfig().global().getCoordinateSystem());
		this.zoomer.addZoomPanListener(this.tileMap);

		this.init();
		this.fr.setVisible(true);




	}

	@Override
	public void setup() {
		size(1024,768);
		background(255);
	}

	@Override
	public void draw() {

		pushMatrix();

		// This enables zooming/panning and should be in the draw method.
		this.zoomer.transform();
		background(255);	
		
		

		List<PVector> coords = new ArrayList<PVector>();
		for (int x = 0; x <= this.width; x += 128) {
			for (int y = 0; y <= this.height; y += 128) {
				PVector d = this.zoomer.getDispToCoord(new PVector(x,y));
				coords.add(d);
			}
		}
		Collection<Tile> tiles = this.tileMap.getTiles(coords);
		for (Tile tile : tiles) {
			drawTile(tile);
		}
		for (VisDebuggerAdditionalDrawer d : this.additionalDrawers) {
			d.draw(this);
		}
		
	
		strokeWeight((float) (2/this.zoomer.getZoomScale()));
		strokeCap(ROUND);
	
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
				}else if (el instanceof Text) {
					drawText((Text)el);
				}
			}
		}

		for (Object obj : this.elements ) {
			if (obj instanceof Circle) {
				drawCircle((Circle)obj);
			} else if (obj instanceof Line) {
				drawLine((Line) obj);
			}
		}
		


		//		System.out.println(this.zoomer.getDispToCoord(new PVector(this.width/2, this.height/2)));
		popMatrix();
		strokeWeight(1);
		stroke(1);
		for (Text t : this.elementsText) {
			if (this.zoomer.getZoomScale() < t.minScale) {
				continue;
			}
			float ts = (float) (10*this.zoomer.getZoomScale()/t.minScale);
			textSize(ts);	
			PVector cv = this.zoomer.getCoordToDisp(new PVector(t.x,t.y));
			fill(t.r,t.g,t.b,t.a);
			float w = textWidth(t.text);
			if (t.theta != 0) {
				pushMatrix();
				translate(cv.x, cv.y);
				rotate(t.theta);
				textAlign(CENTER) ;
				text(t.text, 0,0);
				popMatrix();
			} else {
				textAlign(LEFT);
				text(t.text, cv.x-w/2, cv.y+ts/2);
			}


		}
		//		popMatrix();
		//draw non transformable stuff here ...
	}


	private void drawTile(Tile tile) {
		PImage pImage = null;
		synchronized (tile) {
			pImage = tile.getPImage();
		}
		if (pImage != null) {
			float sz = (float) (-tile.getTy()+tile.getBy());
			image(pImage, (float)(tile.getTx()+this.offsetX), (float)-(tile.getBy()+this.offsetY),sz,sz);

		} 
		else {
			//			fill(122,122,122,122);
			stroke(0);
			line((float)(tile.getTx()+this.offsetX),-(float)(tile.getTy()+this.offsetY),(float)(tile.getBx()+this.offsetX),(float)-(tile.getTy()+this.offsetY));
			line((float)(tile.getBx()+this.offsetX),-(float)(tile.getTy()+this.offsetY),(float)(tile.getBx()+this.offsetX),(float)(tile.getBy()+this.offsetY));
			line((float)(tile.getBx()+this.offsetX),-(float)(tile.getBy()+this.offsetY),(float)(tile.getTx()+this.offsetX),-(float)(tile.getBy()+this.offsetY));
			line((float)(tile.getTx()+this.offsetX),-(float)(tile.getBy()+this.offsetY),(float)(tile.getTx()+this.offsetX),-(float)(tile.getTy()+this.offsetY));
		}
	}

	void drawText(Text el) {
		stroke(el.r,el.g,el.b,el.a);
		fill(el.r,el.g,el.b,el.a);
		//		text(el.text,el.x,el.y);
		//		double fs = Math.max(1, 14/this.zoomer.getZoomScale());
		//		System.out.println(fs);
		//		textSize((float) fs);
		text(el.text, el.x, el.y);
		//		this.zoomer.
		//		this.zoomer.text(el.text, POSTERIZE, POSTERIZE)
	}

	private void drawCircle(Circle c) {
		if (this.zoomer.getZoomScale() < c.minScale) {
			return;
		}
		if (c.fill) {
			fill(c.r,c.g,c.b,c.a);
		} else {
			fill(255);
		}
		stroke(c.r,c.g,c.b,c.a);
		ellipseMode(RADIUS);
		ellipse(c.x,c.y,c.rr,c.rr);
	}

	private void drawPolygon(Polygon p) {
		//		if (this.scale < p.minScale) {
		//			return;
		//		}
		if (this.zoomer.getZoomScale() < p.minScale) {
			return;
		}

		fill(p.r,p.g,p.b,p.a);
		stroke(p.r,p.g,p.b,p.a);
		beginShape();
		for (int i = 0; i < p.x.length; i++) {
			vertex(p.x[i],p.y[i]);
		}
		endShape();

	}

	private void drawLine(Line l) {
		if (this.zoomer.getZoomScale() < l.minScale) {
			return;
		}
		//		if (this.scale < (l.minScale-l.a)) {
		//			return;
		//		}
		int a = l.a;
		//		if (this.scale < l.minScale) {
		//			a -= (int) (l.minScale-this.scale);
		//		} 
		stroke(l.r, l.g, l.b, a);
		//		stroke(20);
		//		strokeWeight(2);
		line(l.x0,l.y0,l.x1,l.y1);

	}

	/*package*/ void addCircle(double x, double y, float rr, int r, int g, int b, int a, int minScale, boolean fill) {
		Circle c = new Circle();
		c.x = (float) (x + this.offsetX);
		c.y = (float) -(y + this.offsetY);
		c.rr = rr;
		c.r = r;
		c.g = g;
		c.b = b;
		c.a = a;
		c.minScale = minScale;
		c.fill  = fill;
		addElement(c);
	}

	/*package*/ void addLineStatic(double x0, double y0, double x1, double y1, int r, int g, int b, int a, int minScale) {
		Line l = new Line();
		l.x0 = (float) (x0 + this.offsetX);
		l.x1 = (float) (x1 + this.offsetX);
		l.y0 = (float) -(y0 + this.offsetY);
		l.y1 = (float) -(y1 + this.offsetY);
		l.r = r;
		l.g = g;
		l.b = b;
		l.a = a;
		l.minScale = minScale;
		addElementStatic(l);

	}

	/*package*/ void addCircleStatic(double x, double y, float rr, int r, int g, int b, int a, int minScale) {
		Circle c = new Circle();
		c.x = (float) (x + this.offsetX);
		c.y = (float) -(y + this.offsetY);
		c.rr = rr;
		c.r = r;
		c.g = g;
		c.b = b;
		c.a = a;
		c.minScale = minScale;
		addElementStatic(c);
	}

	/*package*/ void addLine(double x0, double y0, double x1, double y1, int r, int g, int b, int a, int minScale) {
		Line l = new Line();
		l.x0 = (float) (x0 + this.offsetX);
		l.x1 = (float) (x1 + this.offsetX);
		l.y0 = (float) -(y0 + this.offsetY);
		l.y1 = (float) -(y1 + this.offsetY);
		l.r = r;
		l.g = g;
		l.b = b;
		l.a = a;
		l.minScale = minScale;
		addElement(l);

	}

	/*package*/ void addTextStatic(double x, double y, String string, int minScale) {
		Text text = new Text();
		text.x = (float) (x + this.offsetX);
		text.y = (float) -(y + this.offsetY);
		text.text = string;
		text.a = 255;
		text.minScale = minScale;
		addElementStatic(text);
	}
	/*package*/ void addText(double x, double y, String string, int minScale) {
		Text text = new Text();
		text.x = (float) (x + this.offsetX);
		text.y = (float) -(y + this.offsetY);
		text.text = string;
		text.a = 255;
		text.minScale = minScale;
		addElement(text);
	}

	public void addText(double x, double y, String string, int minScale, float atan) {
		Text text = new Text();
		text.x = (float) (x + this.offsetX);
		text.y = (float) -(y + this.offsetY);
		text.text = string;
		text.a = 255;
		text.minScale = minScale;
		text.theta = atan;
		addElement(text);

	}

	public void addPolygonStatic(double [] x, double [] y, int r, int g, int b, int a, int minScale) {
		Polygon p = new Polygon();
		float [] fx = new float[x.length];
		float [] fy = new float[x.length];
		for (int i = 0; i < x.length; i++) {
			fx[i] = (float) (x[i] + this.offsetX);
			fy[i] = (float) -(y[i] + this.offsetY);
		}
		
		p.x = fx;
		p.y = fy;
		p.r = r;
		p.g = g;
		p.b = b;
		p.a = a;
		p.minScale = minScale;
		addElementStatic(p);
	}

	private void addElement(Object o) {
		if (o instanceof Text) {
			this.newElementsText.add((Text) o);
		} else {
			this.newElements.add(o);
		}
	}

	private void addElementStatic(Object o) {
		synchronized(this.elementsStatic){
			this.elementsStatic.add(o);
		}
	}

	private static class Circle {
		boolean fill = true;
		float x,y,rr;
		int r,g,b,a, minScale = 0;
	}

	private static class Polygon {
		float [] x;
		float [] y;
		int r,g,b,a, minScale = 0;
	}

	private static final class Line {
		float x0,x1,y0,y1;
		int r,g,b,a, minScale = 0;
	}

	static final class Text {
		float x,y, theta = 0;
		String text;
		int r = 0, g = 0, b = 0, a = 255; 
		int minScale = 0;
	}


	/*package*/ void update(double time) {
		synchronized(this.elements) {
			//			this.elements.clear();
			this.elements = Collections.synchronizedList(new ArrayList<Object>(this.newElements));

			this.newElements.clear();
		}
		synchronized (this.elementsText) {
			this.elementsText = Collections.synchronizedList(new ArrayList<Text>(this.newElementsText));
			this.newElementsText.clear();
		}
	}


	public void addDashedLineStatic(double x, double y, double x2, double y2,
			int r, int g, int b, int a, int minScale, double dash, double gap) {
		double dx = x2-x;
		double dy = y2-y;
		double l = Math.sqrt(dx*dx+dy*dy);
		dx /= l;
		dy /= l;
		double tl = 0;

		double tx = x;
		double ty = y;
		while (tl < l) {
			if (tl + dash > l) {
				addLineStatic(tx, ty, x2, y2, r, g, b, a, minScale);
			} else {
				addLineStatic(tx,ty,tx+dx*dash,ty+dy*dash,r,g,b,a,minScale);
			}
			tx += dx *(dash+gap);
			ty += dy *(dash+gap);
			tl += dash + gap;
		}

		// TODO Auto-generated method stub

	}

	void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		this.additionalDrawers.add(drawer);
	}



}
