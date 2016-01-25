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

package matsimConnector.visualizer.debugger.eventsbaseddebugger;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;
import java.awt.*;


import matsimConnector.utility.Constants;

import org.gicentre.utils.move.ZoomPan;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

public class EventsBasedVisDebugger extends PApplet {

	private static final long serialVersionUID = 1L;

	protected final JFrame fr;

	private List<Object> elements = Collections.synchronizedList(new ArrayList<Object>());
	private List<Text> elementsText = Collections.synchronizedList(new ArrayList<Text>());
	private final List<Text> elementsTextStatic = Collections.synchronizedList(new ArrayList<Text>());
	private final List<Object> newElements = new ArrayList<Object>();
	private final List<Text> newElementsText = new ArrayList<Text>();
	private final List<Object> elementsStatic = Collections.synchronizedList(new ArrayList<Object>());

	private final List<VisDebuggerAdditionalDrawer> additionalDrawers = Collections.synchronizedList(new ArrayList<VisDebuggerAdditionalDrawer>());

	double offsetX = -1113948;
	double offsetY = -7041222;

	ZoomPan zoomer;
	//	ZoomPan zoomer;
	private final TileMap tileMap;

	private Control keyControl;

	double time;	

	int dummy = 0;

	FrameSaver fs;
	//	private final FrameSaver fs = null;
	//private String it;
	
	public EventsBasedVisDebugger(FrameSaver fs) {
		this.fs = fs;
		//computeOffsets(sc);
		this.fr = new JFrame();
		//				this.fr.setSize(1024,788);
		this.fr.setSize(1024,788);
		//		this.fr.setSize(1280,740);
		//		this.fr.setSize(720,740);
		JPanel compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));

		this.fr.add(compositePanel,BorderLayout.CENTER);

		compositePanel.add(this);
		compositePanel.setEnabled(true);
		compositePanel.setVisible(true);

		this.zoomer = new ZoomPan(this);//,this.recorder);  // Initialise the zoomer.
		this.tileMap = new TileMap(this.zoomer,this, this.offsetX, this.offsetY, Constants.COORDINATE_SYSTEM);
		this.zoomer.addZoomPanListener(this.tileMap);


		this.init();
		frameRate(90);
		//				size(1024, 768);
//		size(1024, 768);
		this.fr.setVisible(true);
	}
	
	public EventsBasedVisDebugger(Scenario sc,FrameSaver fs) {
		this.fs = fs;
		computeOffsets(sc);
		this.fr = new JFrame();
		//				this.fr.setSize(1024,788);
		this.fr.setSize(3840,2160);
		//		this.fr.setSize(1280,740);
		//		this.fr.setSize(720,740);
		JPanel compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));

		this.fr.add(compositePanel,BorderLayout.CENTER);

		compositePanel.add(this);
		compositePanel.setEnabled(true);
		compositePanel.setVisible(true);

		this.zoomer = new ZoomPan(this);//,this.recorder);  // Initialise the zoomer.
		this.tileMap = new TileMap(this.zoomer,this, this.offsetX, this.offsetY, sc.getConfig().global().getCoordinateSystem());
		this.zoomer.addZoomPanListener(this.tileMap);


		this.init();
		frameRate(90);
		//				size(1024, 768);
//		size(1024, 768);
		this.fr.setVisible(true);
	}

	private void computeOffsets(Scenario sc) {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (Node node : sc.getNetwork().getNodes().values()) {
			double x = node.getCoord().getX();
			double y = node.getCoord().getY();
			if (x < minX) {
				minX = x;
			}
			if (x > maxX) {
				maxX = x;
			}
			if (y < minY) {
				minY = y;
			}
			if (y > maxY) {
				maxY = y;
			}

		}
		this.offsetX = -(maxX+minX)/2;
		this.offsetY = -(maxY+minY)/2;
	}

	@Override
	public void setup() {
		size(1024,768);
		//		size(1280,720);
		//		size(720,720);
		background(0);

	}

	@Override
	public void draw() {
		//Font f = this.getFont();

		boolean recording = false;
		ZoomPan old = null; 
		if (this.keyControl != null && this.keyControl.isScreenshotRequested() && this.keyControl.isOneObjectWaitingAtScreenshotBarrier()) {
			beginRecord(PDF, "/Users/laemmel/tmp/processing/" + "sim2d_screenshot_at_" + this.time + "_" + System.currentTimeMillis()+ ".pdf");
			recording = true;
			old = this.zoomer;
			this.zoomer = new ZoomPan(this, this.recorder);
			this.zoomer.setZoomScale(old.getZoomScale());
			this.zoomer.setPanOffset(old.getPanOffset().x, old.getPanOffset().y);

		}


		pushMatrix();
		if (recording) {
			this.recorder.pushMatrix();
		}
		// This enables zooming/panning and should be in the draw method.
		this.zoomer.transform();
		background(255);	



		List<PVector> coords = new ArrayList<PVector>();
		for (int x = 0; x <= this.width; x += 128) {
			for (int y = 0; y <= this.height+128; y += 128) {
				//PVector d = 
				this.zoomer.getDispToCoord(new PVector(x,y));
//				coords.add(d);
			}
		}


		Collection<Tile> tiles = this.tileMap.getTiles(coords);
		for (Tile tile : tiles) {
			drawTile(tile);
		}

		synchronized (this.additionalDrawers) {
			for (VisDebuggerAdditionalDrawer d : this.additionalDrawers) {
				d.draw(this);
			}
		}

		popMatrix();
		if (recording) {
			this.recorder.popMatrix();
		}
		synchronized (this.additionalDrawers) {
			for (VisDebuggerAdditionalDrawer d : this.additionalDrawers) {
				if (d instanceof VisDebuggerOverlay) {
					continue;
				}
				d.drawText(this);
			}
		}
		pushMatrix();
		if (recording) {
			this.recorder.pushMatrix();
		}
		this.zoomer.transform();

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
				}else if (el instanceof Rect) {
					drawRect((Rect) el);
				} else if (el instanceof Polygon) {
					drawPolygon((Polygon)el);
				}
			}
		}

		for (Object obj : this.elements ) {
			if (obj instanceof Circle) {
				drawCircle((Circle)obj);
			} else if (obj instanceof Line) {
				drawLine((Line) obj);
			} else if (obj instanceof Triangle) {
				drawTriangle((Triangle) obj);
			} else if (obj instanceof Rect) {
				drawRect((Rect) obj);
			}
		}


		popMatrix();
		if (recording) {
			this.recorder.popMatrix();
		}
		strokeWeight(1);
		stroke(1);
		synchronized(this.elementsTextStatic) {
			for (Text t : this.elementsTextStatic) {
				if (this.zoomer.getZoomScale() < t.minScale) {
					continue;
				}
				drawText(t);
			}
		}
		for (Text t : this.elementsText) {
			if (this.zoomer.getZoomScale() < t.minScale) {
				continue;
			}
			drawText(t);
		}

		synchronized (this.additionalDrawers) {
			for (VisDebuggerAdditionalDrawer d : this.additionalDrawers) {
				if (d instanceof VisDebuggerOverlay) {
					d.drawText(this);
				}
			}
		}


		if (recording) {
			endRecord();
			this.zoomer = old;
			this.keyControl.awaitScreenshot();
			this.keyControl.informScreenshotPerformed();
		}

		if (this.fs != null) {
			this.fs.saveFrame(this);//+Time.writeTime(this.time, Time.TIMEFORMAT_HHMMSSDOTSS));
		}

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
			line((float)(tile.getTx()+this.offsetX),-(float)(tile.getTy()+this.offsetY),(float)(tile.getBx()+this.offsetX),(float)-(tile.getBy()+this.offsetY));
			line((float)(tile.getBx()+this.offsetX),-(float)(tile.getTy()+this.offsetY),(float)(tile.getTx()+this.offsetX),(float)-(tile.getBy()+this.offsetY));
			line((float)(tile.getTx()+this.offsetX),-(float)(tile.getTy()+this.offsetY),(float)(tile.getBx()+this.offsetX),(float)-(tile.getTy()+this.offsetY));
			line((float)(tile.getBx()+this.offsetX),-(float)(tile.getBy()+this.offsetY),(float)(tile.getTx()+this.offsetX),(float)-(tile.getBy()+this.offsetY));

			//			line((float)(tile.getBx()+this.offsetX),-(float)(tile.getBy()+this.offsetY),(float)(tile.getTx()+this.offsetX),-(float)(tile.getBy()+this.offsetY));
			//			line((float)(tile.getTx()+this.offsetX),-(float)(tile.getBy()+this.offsetY),(float)(tile.getTx()+this.offsetX),-(float)(tile.getTy()+this.offsetY));
		}
	}

	private void drawText(Text t) {
		float ts = (float) (18*this.zoomer.getZoomScale()/t.minScale);
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
		//		System.out.println(cv.x  + "  " +  cv.y);
	}

	private void drawTriangle(Triangle t) {
		if (this.zoomer.getZoomScale() < t.minScale) {
			return;
		}
		stroke(t.r,t.g,t.b,t.a);
		if (t.fill) {
			fill(t.r,t.g,t.b,t.a);	
		} else {
			fill(0, 0);
		}

		triangle(t.x0, t.y0, t.x1, t.y1, t.x2, t.y2);
	}

	private void drawCircle(Circle c) {
		if (this.zoomer.getZoomScale() < c.minScale) {
			return;
		}
		if (c.fill) {
			fill(c.r,c.g,c.b,c.a);
			//			fill(c.r,c.g,c.b,0);
		} else {
			fill(255,0);
		}
		//		stroke(c.r,c.g,c.b,c.a);
		//		stroke(0,0,0,128);;
		stroke(0,(float) (255*this.zoomer.getZoomScale()/100)+32);
		ellipseMode(RADIUS);
		ellipse(c.x,c.y,c.rr,c.rr);
		//		filter(BLUR, 4);
	}

	private void drawPolygon(Polygon p) {
		//		if (this.scale < p.minScale) {
		//			return;
		//		}
		if (this.zoomer.getZoomScale() < p.minScale) {
			return;
		}

		fill(p.r,p.g,p.b,p.a);
		//		stroke(p.r,p.g,p.b,p.a);
		//		strokeWeight(.5f);
		stroke(0);
		//		stroke(0,0);
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
		//				strokeWeight(2);

		line(l.x0,l.y0,l.x1,l.y1);

	}

	private void drawRect(Rect r) {
		if (this.zoomer.getZoomScale() < r.minScale) {
			return;
		}

		stroke(0,255);
		if (r.fill){
			fill(r.r,r.g,r.b,r.a);
		} else {
			fill(0,0);
		}
		rect(r.tx,r.ty,r.sx,r.sy);
	}


	/*package*/ void addTriangle(double x0, double y0, double x1, double y1, double x2, double y2, int r, int g, int b, int a, int minScale, boolean fill) {
		Triangle t = new Triangle();
		t.x0 = (float)(x0+this.offsetX);
		t.x1 = (float)(x1+this.offsetX);
		t.x2 = (float)(x2+this.offsetX);

		t.y0 = (float)-(y0+this.offsetY);
		t.y1 = (float)-(y1+this.offsetY);
		t.y2 = (float)-(y2+this.offsetY);

		t.r = r; t.g = g; t.b = b; t.a = a; t.minScale = minScale; t.fill = fill;
		addElement(t);

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

	public void addRect(double tx, double ty, double sx, double sy, int r,
			int g, int b, int a, int minScale, boolean fill) {
		Rect rect = new Rect();
		rect.tx = (float) (tx + this.offsetX);
		rect.ty = (float) -(ty + this.offsetY);
		rect.sx = (float)sx;
		rect.sy = (float)sy;
		rect.a = a;
		rect.r = r;
		rect.g = g;
		rect.b = b;
		rect.minScale = minScale;
		rect.fill = fill;

		addElement(rect);
	}
	
	/*package*/ void addRectStatic(double tx, double ty, double sx, double sy, int r,
			int g, int b, int a, int minScale, boolean fill) {
		Rect rect = new Rect();
		rect.tx = (float) (tx + this.offsetX);
		rect.ty = (float) -(ty + this.offsetY);
		rect.sx = (float)sx;
		rect.sy = (float)sy;
		rect.a = a;
		rect.r = r;
		rect.g = g;
		rect.b = b;
		rect.minScale = minScale;
		rect.fill = fill;

		addElementStatic(rect);
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
			if (o instanceof Text) {
				this.elementsTextStatic.add((Text) o);
			} else {
				this.elementsStatic.add(o);
			}
		}
	}

	private static class Triangle {
		boolean fill = true;
		float x0,x1,x2,y0,y1,y2;
		int r,g,b,a,minScale;
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

	private static final class Rect {
		public boolean fill;
		float tx,ty,sx,sy;
		int r,g,b,a, minScale = 0;
	}

	static final class Text {
		float x,y, theta = 0;
		String text = "";
		int r = 0, g = 0, b = 0, a = 255; 
		int minScale = 0;
	}


	/*package*/ void update(double time) {
		if (this.fs != null && this.fs.incrSkipped()) {
			this.fs.await();
		}
		this.time = time;
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

	public void addDashedLine(double x, double y, double x2, double y2,
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
				addLine(tx, ty, x2, y2, r, g, b, a, minScale);
			} else {
				addLine(tx,ty,tx+dx*dash,ty+dy*dash,r,g,b,a,minScale);
			}
			tx += dx *(dash+gap);
			ty += dy *(dash+gap);
			tl += dash + gap;
		}

		// TODO Auto-generated method stub

	}

	void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		synchronized (this.additionalDrawers) {
			this.additionalDrawers.add(drawer);
		}
	}

	public void addKeyControl(Control keyControl) {
		this.addKeyListener(keyControl);
		this.addMouseWheelListener(keyControl);
		this.keyControl = keyControl;
		this.keyControl.addTileMap(this.tileMap);
	}
	/*
	public void reset(int it) {
		if (it < 10) {
			this.it = "it.00"+it+"_";
		} else if (it < 100) {
			this.it = "it.0"+it+"_";
		} else {
			this.it = "it."+it+"_";
		}

	}*/
}
