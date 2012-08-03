/* *********************************************************************** *
 * project: org.matsim.*
 * MyMapViewer.java
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

package playground.wdoering.grips.populationselector;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class MyMapViewer extends JXMapViewer implements MouseListener, MouseWheelListener, KeyListener, MouseMotionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	boolean editMode = false;
	
	
	private final MouseListener m [];
	private final MouseMotionListener mm [];
	private final MouseWheelListener mw [];
	private final KeyListener k [];

	private GeoPosition c0;
	private GeoPosition c1;
	
	private ArrayList<GeoPosition> newLine = null;

	private ShapeToStreetSnapperThreadWrapper snapper;

	private Thread thread;

	private Polygon areaPolygon;

	public MyMapViewer() {
		super();
		this.m = super.getMouseListeners();
		for (MouseListener l : this.m) {
			super.removeMouseListener(l);
		}
		this.mm = super.getMouseMotionListeners();
		for (MouseMotionListener m : this.mm) {
			super.removeMouseMotionListener(m);
		}
		this.mw = super.getMouseWheelListeners();
		for (MouseWheelListener mw : this.mw) {
			super.removeMouseWheelListener(mw);
		}
		this.k = super.getKeyListeners();
		for (KeyListener k : this.k) {
			super.removeKeyListener(k);
		}

		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		System.out.println("klick!" + " e.getButton():" + e.getButton());
		
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mouseEntered(e);
			}
		}
		
	}


	@Override
	public void mouseExited(MouseEvent e) {
		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mouseExited(e);
			}
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		
		if (e.getButton() == MouseEvent.BUTTON1) {
			this.editMode = true;
			if (this.thread != null) {
//				System.out.println("interrupt");
				this.thread.stop();
			}
			this.snapper.reset();
			Rectangle b = this.getViewportBounds();
			Point p0 = e.getPoint();
			Point wldPoint = new Point(p0.x+b.x,p0.y+b.y);
			this.c0 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
			this.newLine = new ArrayList<GeoPosition>();
		}
		
		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mousePressed(e);
			}
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mouseReleased(e);
			}
		}
		if (e.getButton() == MouseEvent.BUTTON1) {
			this.editMode = false;
			Point p1 = e.getPoint();
			Rectangle b = this.getViewportBounds();
			Point wldPoint = new Point(p1.x+b.x,p1.y+b.y);
			this.c1 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
			this.snapper.setCoordinates(this.c0,this.c1);
//			this.thread.interrupt();
			this.thread = new Thread(this.snapper);
			this.thread.start();
			this.newLine=null;
//			this.thread.
			repaint();
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (!this.editMode) {
			for (MouseWheelListener m : this.mw) {
				m.mouseWheelMoved(e);
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if (!this.editMode) {
			for (KeyListener k : this.k) {
				k.keyPressed(e);
			}
		}
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if (!this.editMode) {
			for (KeyListener k : this.k) {
				k.keyReleased(e);
			}
		}
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		if (!this.editMode) {
			for (KeyListener k : this.k) {
				k.keyTyped(e);
			}
		}
	}

	@Override
	public void mouseDragged(MouseEvent arg0) {
		if (!this.editMode) {
			for (MouseMotionListener m : this.mm) {
				m.mouseDragged(arg0);
			}
		} else {
			Point p = arg0.getPoint();
			Rectangle b = this.getViewportBounds();
			Point wldPoint = new Point(p.x+b.x,p.y+b.y);
			this.c1 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
			this.newLine.add(c1);
			repaint();
		}
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		if (!this.editMode) {
			for (MouseMotionListener m : this.mm) {
				m.mouseMoved(arg0);
			}
		}		
	}

	@Override
	public void paint(Graphics g) {
		
		super.paint(g);
		Rectangle b = this.getViewportBounds();

		//draw area polygon
		if (areaPolygon == null)
			areaPolygon = this.snapper.getAreaPolygon();
		if (areaPolygon != null)
		{
			
			int [] x = new int[areaPolygon.getExteriorRing().getNumPoints()];
			int [] y = new int[areaPolygon.getExteriorRing().getNumPoints()];
			for (int i = 0; i < areaPolygon.getExteriorRing().getNumPoints(); i++) {
				Coordinate c = areaPolygon.getExteriorRing().getCoordinateN(i);
				Point2D wldPoint = this.getTileFactory().geoToPixel(new GeoPosition(c.y,c.x), this.getZoom());
				x[i] = (int) (wldPoint.getX()-b.x);
				y[i] = (int) (wldPoint.getY()-b.y);
				if (i > 0) {
					g.drawLine(x[i-1], y[i-1], x[i], y[i]);
				}
			}
			g.setColor(new Color(0,0,255,128));
			g.fillPolygon(x, y, areaPolygon.getExteriorRing().getNumPoints());
		}
		
		if (this.c0 != null && this.c1 != null)
		{
			g.setColor(Color.magenta);
			Graphics2D g2D = (Graphics2D) g;     
			g2D.setStroke(new BasicStroke(2F));
			

			
			
			Polygon p = this.snapper.getPolygon();
			if (p != null) {
				int [] x = new int[p.getExteriorRing().getNumPoints()];
				int [] y = new int[p.getExteriorRing().getNumPoints()];
				for (int i = 0; i < p.getExteriorRing().getNumPoints(); i++) {
					Coordinate c = p.getExteriorRing().getCoordinateN(i);
					Point2D wldPoint = this.getTileFactory().geoToPixel(new GeoPosition(c.y,c.x), this.getZoom());
					x[i] = (int) (wldPoint.getX()-b.x);
					y[i] = (int) (wldPoint.getY()-b.y);
					if (i > 0) {
						g.drawLine(x[i-1], y[i-1], x[i], y[i]);
					}
				}
				g.setColor(new Color(255,0,0,128));
				g.fillPolygon(x, y, p.getExteriorRing().getNumPoints());
				
				
			} else if (newLine!=null) {
				
				//first point
				Point2D wldPoint0 = this.getTileFactory().geoToPixel(this.c0, this.getZoom());
				Point sc0 = new Point((int)(wldPoint0.getX() - b.x), (int)(wldPoint0.getY() - b.y));
				
				//draw from first point to second
				Point2D wldPoint = this.getTileFactory().geoToPixel(newLine.get(0), this.getZoom());
				Point sc = new Point((int)(wldPoint.getX() - b.x), (int)(wldPoint.getY() - b.y));
				Point lastSc = (Point) sc.clone();
				
				g.drawLine(sc0.x, sc0.y, sc.x, sc.y);
				
				for (GeoPosition gp : newLine)
				{
					wldPoint = this.getTileFactory().geoToPixel(gp, this.getZoom());
					sc = new Point((int)(wldPoint.getX() - b.x), (int)(wldPoint.getY() - b.y));
					
					g.drawLine(lastSc.x, lastSc.y, sc.x, sc.y);
					
					lastSc = (Point) sc.clone();
					
				}
				
//				int r = (int) (Math.sqrt(Math.pow(sc0.x-sc1.x, 2)+Math.pow(sc0.y-sc1.y, 2))+0.5);
//				g.drawOval(sc0.x-r, sc0.y-r, 2*r, 2*r);
//				g.setColor(new Color(255,0,0,128));
//				g.fillOval(sc0.x-r+1, sc0.y-r+1, 2*r-2, 2*r-2);
			}
		}
	}

	public void setSnapper(ShapeToStreetSnapperThreadWrapper snapper) {
		this.snapper = snapper;
		this.snapper.setView(this);		
		
	}

}
