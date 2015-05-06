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

package playground.wdoering.grips.areaselector;


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
import java.util.LinkedList;
import java.util.List;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.contrib.evacuation.model.config.ToolConfig;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class MyMapViewer extends JXMapViewer implements MouseListener, MouseWheelListener, KeyListener, MouseMotionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	boolean editMode = false;
	
	public static enum SelectionMode { POLYGON, CIRCLE };
	private SelectionMode selectionMode = SelectionMode.POLYGON;
	
	private final MouseListener m [];
	private final MouseMotionListener mm [];
	private final MouseWheelListener mw [];
	private final KeyListener k [];
	
	private List<GeoPosition> polygon;

	private GeoPosition c0;
	private GeoPosition c1;

	private ShapeToStreetSnapperThreadWrapper snapper;

	private Thread thread;

	private Point mousePoint;

	private boolean hoveringOverPoint;

	public MyMapViewer() {
		super();
		
		this.polygon = new LinkedList<GeoPosition>();
		
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
//		System.out.println("klick!" + " e.getButton():" + e.getButton());
		
		
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
		
		this.mousePoint = e.getPoint();

		
		if (e.getButton() == MouseEvent.BUTTON1) {
			
			
			Rectangle b = this.getViewportBounds();
			
			if (this.selectionMode.equals(SelectionMode.POLYGON))
			{
				
				if (this.hoveringOverPoint)
				{
					this.snapper.setGeoPolygon(polygon);
					this.thread = new Thread(this.snapper);
					this.thread.start();
					this.editMode=false;
					this.polygon=null;
					
				}
				else
				{
			
					if (!this.editMode)
					{
						polygon = new LinkedList<GeoPosition>();
						this.editMode = true;
					}
					else
					{
						Point wldPoint = new Point(this.mousePoint.x+b.x,this.mousePoint.y+b.y);
						polygon.add(this.getTileFactory().pixelToGeo(wldPoint, this.getZoom()));
					}
				}
				 
			}
			else
			{
			
				if (this.thread != null) {
	//				System.out.println("interrupt");
					this.thread.stop();
				}
				
				this.snapper.reset();
				Point p0 = e.getPoint();
				Point wldPoint = new Point(p0.x+b.x,p0.y+b.y);
				this.c0 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
			}
		}
		
		
		if ((!this.editMode) || (selectionMode.equals(SelectionMode.POLYGON)&&(e.getButton() == MouseEvent.BUTTON2)) ) {
			for (MouseListener m : this.m) {
				m.mousePressed(e);
			}
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
		this.mousePoint = e.getPoint();
		
		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mouseReleased(e);
			}
		}
		
		if (this.selectionMode.equals(SelectionMode.CIRCLE))
		{
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
	//			this.thread.
				repaint();
			}
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
//		if (!this.editMode)
		{
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
		
		if (this.selectionMode.equals(SelectionMode.CIRCLE))
		{
			if (!this.editMode)
			{
				for (MouseMotionListener m : this.mm) {
					m.mouseDragged(arg0);
				}
			} else {
				Point p = arg0.getPoint();
				Rectangle b = this.getViewportBounds();
				Point wldPoint = new Point(p.x+b.x,p.y+b.y);
				this.c1 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
				repaint();
			}
		}
//		else //if (arg0.getButton() == MouseEvent.BUTTON2)
//		{
//			for (MouseMotionListener m : this.mm) {
//				if (arg0!=null)
//					m.mouseDragged(arg0);
//			}			
//		}
		
//		System.out.println(arg0.getButton()+"");
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0) {
		
		this.mousePoint = arg0.getPoint();
		
		if (this.editMode)
			repaint();
		
		if (!this.editMode) {
			for (MouseMotionListener m : this.mm) {
				m.mouseMoved(arg0);
			}
		}		
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2D = (Graphics2D) g;     
		
		Rectangle b = this.getViewportBounds();
		
		if (selectionMode.equals(SelectionMode.CIRCLE))
		{
			if (this.c0 != null && this.c1 != null) {
				Point2D wldPoint0 = this.getTileFactory().geoToPixel(this.c0, this.getZoom());
				Point2D wldPoint1 = this.getTileFactory().geoToPixel(this.c1, this.getZoom());
				Point sc0 = new Point((int)(wldPoint0.getX() - b.x), (int)(wldPoint0.getY() - b.y));
				Point sc1 = new Point((int)(wldPoint1.getX() - b.x), (int)(wldPoint1.getY() - b.y));
				g.setColor(Color.black);
			    g2D.setStroke(new BasicStroke(5F));
				
	
				
				
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
					g.setColor(ToolConfig.COLOR_EVAC_AREA);
					g.fillPolygon(x, y, p.getExteriorRing().getNumPoints());
					
					
				} else {
					int r = (int) (Math.sqrt(Math.pow(sc0.x-sc1.x, 2)+Math.pow(sc0.y-sc1.y, 2))+0.5);
					g.setColor(ToolConfig.COLOR_EVAC_AREA_BORDER);
					g.drawOval(sc0.x-r, sc0.y-r, 2*r, 2*r);
					g.setColor(ToolConfig.COLOR_EVAC_AREA);
					g.fillOval(sc0.x-r+1, sc0.y-r+1, 2*r-2, 2*r-2);
				}
			}
		}
		else if (selectionMode.equals(SelectionMode.POLYGON))
		{
			
			//visualizing the closed polygon
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
				g.setColor(ToolConfig.COLOR_EVAC_AREA);
				g.fillPolygon(x, y, p.getExteriorRing().getNumPoints());			
			}
			
			if ((this.editMode) && (this.polygon != null) && (this.polygon.size()>0))
			{
				g.setColor(Color.red);
				GeoPosition lastPoint = this.polygon.get(0);
				GeoPosition currentPoint = lastPoint;
				Point2D lastPoint2D = this.getTileFactory().geoToPixel(lastPoint, this.getZoom());
				Point2D currentPoint2D = lastPoint2D;
				
				if ((lastPoint2D.getX()-b.x >= mousePoint.x-3) &&
					(lastPoint2D.getX()-b.x < mousePoint.x+3) &&
					(lastPoint2D.getY()-b.y >= mousePoint.y-3) &&
					(lastPoint2D.getY()-b.y < mousePoint.y+3))
				{
					this.hoveringOverPoint = true;
					g.setColor(Color.yellow);
				}
				else
					this.hoveringOverPoint = false;
				
				if ((this.polygon.size()==1) && (this.mousePoint!=null))
				{
					g.drawLine((int)lastPoint2D.getX()-b.x,(int)lastPoint2D.getY()-b.y,mousePoint.x,mousePoint.y);
					g.fillOval((int)lastPoint2D.getX()-b.x-3,(int)lastPoint2D.getY()-b.y-3, 6, 6);
				}
				else
				{
					g.fillOval((int)currentPoint2D.getX()-b.x-3,(int)currentPoint2D.getY()-b.y-3, 6, 6);
					g.setColor(Color.red);
					for (int i = 1; i < this.polygon.size(); i++)
					{
						currentPoint = this.polygon.get(i);
						currentPoint2D = this.getTileFactory().geoToPixel(currentPoint, this.getZoom());
						
						g.drawLine((int)lastPoint2D.getX()-b.x, (int)lastPoint2D.getY()-b.y, (int)currentPoint2D.getX()-b.x, (int)currentPoint2D.getY()-b.y);
						g.fillOval((int)currentPoint2D.getX()-b.x-3,(int)currentPoint2D.getY()-b.y-3, 6, 6);
						
						lastPoint2D = currentPoint2D;
					}
					g.drawLine((int)currentPoint2D.getX()-b.x,(int)currentPoint2D.getY()-b.y,mousePoint.x,mousePoint.y);
//					g.fillOval((int)currentPoint2D.getX()-b.x-3,(int)currentPoint2D.getY()-b.y-3, 6, 6);
				}
				
				 
				
			}
			
		}
	}

	public void setSnapper(ShapeToStreetSnapperThreadWrapper snapper) {
		this.snapper = snapper;
		this.snapper.setView(this);		
		
	}
	
	public void setSelectionMode(SelectionMode selectionMode) {
		this.selectionMode = selectionMode;
		
		if (this.snapper!=null)
		{
			this.snapper.setSelectionMode(this.selectionMode);
		}
	}

}
