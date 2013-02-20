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
import javax.swing.JPanel;
import javax.swing.OverlayLayout;

import org.matsim.core.utils.misc.Time;

import processing.core.PApplet;
import processing.core.PImage;

public class VisDebugger extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final JFrame fr;

	public long lastUpdate = -1;






	//	private static final int W = 1000;

	private float scale = 2;
	private List<Object> elements = Collections.synchronizedList(new ArrayList<Object>());
	private final List<Object> elementsStatic = Collections.synchronizedList(new ArrayList<Object>());
	private final List<Object> newElements = new ArrayList<Object>();
	protected int dragX = 0;
	protected int dragY = 0;
	protected int mx =0;
	protected int my = 0;
	protected int omx= 0;
	protected int omy= 0;

	private boolean first = true;
	private String time = "00:00.00.0";
	private String iteration = "it: 0";
	private final double dT;
	private float speedup = 10;

	private int bgXShift;
	private int bgYShift;



	private PTileFactory pTileFac = null;
	private final List<VisDebuggerAdditionalDrawer> additionalDrawers = new ArrayList<VisDebuggerAdditionalDrawer>();

	private FrameSaver fs = null;
	private int it = 0;

	public VisDebugger(double dT) {
		this.dT = dT;
		this.fr = new JFrame();
		this.fr.setSize(1024, 768);

		JPanel compositePanel = new JPanel();
		compositePanel.setLayout(new OverlayLayout(compositePanel));

		this.fr.add(compositePanel,BorderLayout.CENTER);

		compositePanel.add(this);
		compositePanel.setEnabled(true);
		compositePanel.setVisible(true);
		this.init();
		this.fr.setVisible(true);
	}


	@Override
	public void setup() {
		addMouseWheelListener(new java.awt.event.MouseWheelListener() { 
			@Override
			public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) { 
				if (evt.getModifiers() == java.awt.event.MouseWheelEvent.SHIFT_MASK) {
					speedup(evt.getWheelRotation());
				} else {
					mouseWheel(evt.getWheelRotation());
				}
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
				VisDebugger.this.computeBGShift();
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
				VisDebugger.this.computeBGShift();

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
		size(1024,768);
		background(0);

	}

	void mouseWheel(int delta)
	{

		float x1 = deScaleX(getWidth()/2);
		float y1 = deScaleY(getHeight()/2);
		this.scale-= delta;
		this.scale = Math.max(1, this.scale);
		this.scale = Math.min(100, this.scale);
		float x2 = deScaleX(getWidth()/2);
		float y2 = deScaleY(getHeight()/2);
		float xShift = -(x1-x2)*this.scale;
		float yShift = (y1-y2)*this.scale;
		this.omx += xShift;
		this.omy += yShift;
		computeBGShift();

	}	

	void speedup(int delta) {
		this.speedup += delta/10.;
		this.speedup = Math.max(0.05f, this.speedup);
		this.speedup = Math.min(100, this.speedup);
		//		System.out.println(this.speedup);
	}

	private void computeBGShift(){
		this.bgYShift = (this.dragY+this.omy)/(256);
		this.bgXShift = (-(VisDebugger.this.dragX +VisDebugger.this.omx))/(256);
//		System.out.println("scale:" + this.scale + " omx:" + this.omx + " omy:" + this.omy + " width:" + getWidth() + " height:" + getHeight());
	}

	@Override
	public void draw() {

		stroke(255);
		background(255,237,187,255);
		fill(255);

		drawBG();

		for (VisDebuggerAdditionalDrawer d : this.additionalDrawers) {
			d.draw(this);
		}
		
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
		int agents = 0;
		synchronized(this.elements) {
			Iterator<Object> it = this.elements.iterator();
			while (it.hasNext()) {
				Object el = it.next();
				if (el instanceof Line) {
					drawLine((Line) el);
				} else if (el instanceof Circle) {
					drawCircle((Circle) el);
					agents++;
				} else if (el instanceof Polygon) {
					drawPolygon((Polygon)el);
				} else if (el instanceof Text) {
					drawText((Text)el);
				}
			}
		}

		strokeWeight(2);
		drawTime();
		drawIteration();
		drawAgentsCount(agents);
		drawSpeedup();
		
		if (this.fs != null) {
			this.fs.saveFrame(this,this.it + "_" + this.time);
		}
	}


	private void drawBG() {
		if (this.pTileFac == null) {
			return;
		}
		//		System.out.println("xshift:" + this.bgXShift + " yshift:" + this.bgYShift + " scale:" + this.scale);
		int xidx = this.bgXShift-1;
		int yidx = this.bgYShift-1;
		int xTiles = getWidth()/256;
		int yTiles = getHeight()/256;
		for (int  y = 0; y <= yTiles+2; y++) {
			for (int x = 0; x <= xTiles+2; x++) {
				//				int logscale = (int)(log(this.scale)/log(2));
				//				System.out.println(logscale);
				PImage img = this.pTileFac.getTile(xidx, yidx, (int)(this.scale+.5f));

				float xx =  xidx * 256/this.scale;
				//				float yy =  yidx * 256/this.scale;

				//				float xx1 = (xidx+1) * 256/this.scale;
				float yy1 = (yidx+1) * 256/this.scale;

				//				PImage img = loadPImage(xx, yy, xx1, yy1);
				if (img != null) {
					image(img,scaleFlX(xx),scaleFlY(yy1),256,256);
				}



				//				System.out.print(xx +"," + yy + "\t");
				xidx++;
			}
			//			System.out.println();
			yidx++;
			xidx -= xTiles+3;
		}

	}


	public void addAdditionalDrawer(VisDebuggerAdditionalDrawer drawer) {
		this.additionalDrawers .add(drawer);
	}

	void drawText(Text el) {
		if (this.scale < el.minScale) {
			return;
		}
		stroke(el.r,el.g,el.b,el.a);
		fill(el.r,el.g,el.b,el.a);
		text(el.text,scaleFlX(el.x),scaleFlY(el.y));
	}

	private void drawTime() {
		String strTime = setTime(-1);
		fill(64, 192);
		stroke(0);
		rect(0, 0, 105, 25);
		fill(192,192,192,255);
		text(strTime, 10, 18);

	}

	private void drawIteration() {
		String iteration = setIteration(-1);
		fill(64, 192);
		stroke(0);
		rect(0, 26, 105, 25);
		fill(192,192,192,255);
		text(iteration, 10, 44);

	}
	private void drawAgentsCount(int agents) {
		fill(64, 192);
		stroke(0);
		rect(0,52,105,25);
		fill(192,192,192,255);
		text("agents: " + agents,10,70);

	}

	private void drawSpeedup() {
		fill(64, 192);
		stroke(0);
		rect(0,78,105,25);
		fill(192,192,192,255);
		int a = (int) this.speedup;
		int b = (int) ((this.speedup - a) * 10);
		text("accel.: " + a + "." + b,10,96);
	}


	private void drawPolygon(Polygon p) {
		if (this.scale < p.minScale) {
			return;
		}
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
		if (this.scale < c.minScale) {
			return;
		}
		if (c.fill) {
			fill(c.r,c.g,c.b,c.a);
		} else {
			fill(c.r,c.g,c.b,0);
		}
		stroke(c.r,c.g,c.b,c.a);
		ellipseMode(RADIUS);
		ellipse(scaleFlX(c.x),scaleFlY(c.y),scaleFl(c.rr),scaleFl(c.rr));
	}

	private void drawLine(Line l) {
		if (this.scale < (l.minScale-l.a)) {
			return;
		}
		int a = l.a;
		if (this.scale < l.minScale) {
			a -= (int) (l.minScale-this.scale);
		} 
		stroke(l.r, l.g, l.b, a);
		//		stroke(20);
		strokeWeight(2);
		line(scaleFlX(l.x0),scaleFlY(l.y0),scaleFlX(l.x1),scaleFlY(l.y1));

	}

	void drawWeightedLine(WeightedLine l) {
		if (this.scale < (l.minScale-l.a)) {
			return;
		}
		int a = l.a;
		if (this.scale < l.minScale) {
			a -= (int) (l.minScale-this.scale);
		} 
		strokeCap(SQUARE);
		stroke(l.r, l.g, l.b, a);
		//		stroke(20);
		final float w = scaleFl(l.weight);
		if (w < 0) {
			return;
		}
		strokeWeight(w);
		line(scaleFlX(l.x0),scaleFlY(l.y0),scaleFlX(l.x1),scaleFlY(l.y1));

	}

	private float scaleFl(float y1) {
		return this.scale * y1;
	}

	private float scaleFlX(float y1) {
		return this.scale * y1 + this.dragX + this.omx;
	}

	private float deScaleX(float x) {
		return (x -this.dragX - this.omx)/this.scale;
	}

	private float scaleFlY(float y1) {
		return this.getHeight() - this.scale* y1 + this.dragY + this.omy;
	}

	private float deScaleY(float y) {
		return (this.getHeight()/2 + this.dragY + this.omy)/this.scale;
	}

	public void addLineStatic(float x0, float y0, float x1, float y1, int r, int g, int b, int a, int minScale) {
		Line l = new Line();
		l.x0 = x0;
		l.x1 = x1;
		l.y0 = y0;
		l.y1 = y1;
		l.r = r;
		l.g = g;
		l.b = b;
		l.a = a;
		l.minScale = minScale;
		addElementStatic(l);

	}

	public void addCircleStatic(float x, float y, float rr, int r, int g, int b, int a, int minScale) {
		Circle c = new Circle();
		c.x = x;
		c.y = y;
		c.rr = rr;
		c.r = r;
		c.g = g;
		c.b = b;
		c.a = a;
		c.minScale = minScale;
		addElementStatic(c);
	}

	public void addLine(float x0, float y0, float x1, float y1, int r, int g, int b, int a, int minScale) {
		Line l = new Line();
		l.x0 = x0;
		l.x1 = x1;
		l.y0 = y0;
		l.y1 = y1;
		l.r = r;
		l.g = g;
		l.b = b;
		l.a = a;
		l.minScale = minScale;
		addElement(l);

	}

	public void addTextStatic(float x, float y, String string, int minScale) {
		Text text = new Text();
		text.x = x;
		text.y = y;
		text.text = string;
		text.a = 255;
		text.minScale = minScale;
		addElementStatic(text);
	}

	public void addCircle(float x, float y, float rr, int r, int g, int b, int a, int minScale, boolean fill) {
		Circle c = new Circle();
		c.x = x;
		c.y = y;
		c.rr = rr;
		c.r = r;
		c.g = g;
		c.b = b;
		c.a = a;
		c.minScale = minScale;
		c.fill  = fill;
		addElement(c);
	}

	public void addPolygon(float [] x, float [] y, int r, int g, int b, int a, int minScale) {
		Polygon p = new Polygon();
		p.x = x;
		p.y = y;
		p.r = r;
		p.g = g;
		p.b = b;
		p.a = a;
		p.minScale = minScale;
		addElement(p);
	}

	public void addText(float x, float y, String string, int minScale) {
		Text text = new Text();
		text.x = x;
		text.y = y;
		text.text = string;
		text.a = 255;
		text.minScale = minScale;
		addElement(text);
	}

	private void addElement(Object o) {
		this.newElements.add(o);
	}
	private void addElementStatic(Object o) {
		synchronized(this.elementsStatic){
			this.elementsStatic.add(o);
		}
	}

	public void update(double time) {

		if (this.fs != null) {
			this.fs.await();
		}
		long timel = System.currentTimeMillis();

		long last = this.lastUpdate;
		long diff = timel - last;
		if (diff < this.dT*1000/this.speedup) {
			long wait = (long) (this.dT *1000/this.speedup-diff);
			try {
				Thread.sleep(wait);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		setTime(time);
		synchronized(this.elements) {
			//			this.elements.clear();
			this.elements = Collections.synchronizedList(new ArrayList<Object>(this.newElements));
			this.newElements.clear();
		}
		this.first = false;
		this.lastUpdate = System.currentTimeMillis();
	}

	public void addAll() {
		synchronized(this.elements) {
			//			this.elements.clear();
			this.elements.addAll(this.newElements);
			this.newElements.clear();
		}
	}

	public boolean isFirst() {
		return this.first;
	}




	static final class Text {
		float x,y;
		String text;
		int r = 192, g = 192, b = 192, a = 222; 
		int minScale = 0;
	}

	private static final class Line {
		float x0,x1,y0,y1;
		int r,g,b,a, minScale = 0;
	}

	static final class WeightedLine {
		float x0, y0, x1, y1, weight = 1, count = 0;
		int r,g,b,a, minScale = 0;
		public int cap;
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

	synchronized public String setIteration(int it) {
		if (it > 0) {
			this.iteration = "iteration: " + it;
//			this.speedup = 1;
			this.it  = it;
		}
		return this.iteration;
	}

	public void setTransformationStuff(float x, float y) {
		if (this.pTileFac != null) {
			return;
		}
		//		this.offsetX = x;
		//		this.offsetY = y;
		//		TileFactory tf = PTileFactory.getWMSTileFactory("http://localhost:8080/geoserver/wms?service=WMS&", "ch","EPSG:3395",x,y);
		this.pTileFac = new PTileFactory(this,"EPSG:3395",x,y);
	}


	public void setFrameSaver(FrameSaver fs) {
		this.fs = fs;
		
	}


	public float getScale() {
		return this.scale;
	}


}
