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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.geotools.referencing.CRS;
import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.contrib.evacuation.control.algorithms.PolygonalCircleApproximation;
import org.matsim.contrib.evacuation.model.config.ToolConfig;

import playground.wdoering.grips.populationselector.PopulationAreaSelector;

import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.NoninvertibleTransformException;

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
	


	private Thread thread;

	private Polygon areaPolygon;

	private int currentIndex;
	
	private PopulationAreaSelector populationAreaSelector;

	private int selectedAreaId = -1;

	private boolean drawing = false;

	private MathTransform transform;

	private String targetS;

	private HashMap<Integer, Polygon> polygons;

	public MyMapViewer(PopulationAreaSelector populationAreaSelector) {
		super();
		
		this.populationAreaSelector = populationAreaSelector;
		
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
		
		this.currentIndex = 0;
	}

	@Override
	public void mouseClicked(MouseEvent e) {
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
				this.thread.stop();
			}
			Rectangle b = this.getViewportBounds();
			Point p0 = e.getPoint();
			Point wldPoint = new Point(p0.x+b.x,p0.y+b.y);
			this.c0 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
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
			
			//TODO
//			this.snapper.setIndexAndCoordinates(this.currentIndex, this.c0,this.c1);
			
			this.currentIndex++;
			addArea();
			repaint();
		}
	}

	private void addArea()
	{
		
		if (transform==null)
			getTransform();
		
		if (polygons==null)
			polygons = new HashMap<Integer, Polygon>();
		
		Coordinate c0 = new Coordinate(this.c0.getLongitude(),this.c0.getLatitude());
		Coordinate c1 = new Coordinate(this.c1.getLongitude(),this.c1.getLatitude());
		PolygonalCircleApproximation.transform(c0,transform);
		PolygonalCircleApproximation.transform(c1,transform);
		
		Polygon p = PolygonalCircleApproximation.getPolygonFromGeoCoords(c0, c1);
		
		if ((p!=null) && (!p.isEmpty()))
		{
			try {
				p = (Polygon) PolygonalCircleApproximation.transform(p, transform.inverse());
			} catch (NoninvertibleTransformException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
			this.polygons.put(currentIndex, p);
			this.populationAreaSelector.addNewArea(currentIndex);

		}
	
		repaint();
		this.populationAreaSelector.setSaveButtonEnabled(true);		
		
	}

	private void getTransform()
	{
		if (this.targetS==null)
			this.targetS = populationAreaSelector.getTargetSystem();

		CoordinateReferenceSystem sourceCRS = MGC.getCRS("EPSG:4326");
		CoordinateReferenceSystem targetCRS = MGC.getCRS(this.targetS);
		transform = null;
		try {
			transform = CRS.findMathTransform(sourceCRS, targetCRS,true);
		} catch (FactoryException e) {
			throw new RuntimeException(e);
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
	public void paint(Graphics g)
	{
		
		super.paint(g);
		
		Graphics2D g2D = (Graphics2D) g;     
		g2D.setStroke(new BasicStroke(2F));
		
		//set drawing boolean (to avoid race conditions (polygon hashmap))
		drawing = true;
		
		Rectangle b = this.getViewportBounds();

		//draw area polygon
		if (areaPolygon == null)
			areaPolygon = this.populationAreaSelector.getAreaPolygon();
		
		if (areaPolygon != null)
		{
			g.setColor(ToolConfig.COLOR_EVAC_AREA_BORDER);
			
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
			
			g.setColor(ToolConfig.COLOR_EVAC_AREA);
			g.fillPolygon(x, y, areaPolygon.getExteriorRing().getNumPoints());
		}
		
		//draw population polygon(s)
		if (this.c0 != null && this.c1 != null)
		{
//			g.setColor(Color.red);

//			//get already selected areas
//			HashMap<Integer, Polygon> polys = null;
//			try
//			{
//				polys = getPolygons();
//			}
//			catch (ConcurrentModificationException e) {}
			
			
			if (polygons != null)
			{
				
				
				Iterator it = polygons.entrySet().iterator();
				while (it.hasNext())
				{
					g.setColor(ToolConfig.COLOR_POP_AREA_BORDER);
					
					Map.Entry<Integer,Polygon> pairs = (Map.Entry)it.next();
					int id = pairs.getKey();
					Polygon p = pairs.getValue();
					
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
					
					if (selectedAreaId==id)
						g.setColor(ToolConfig.COLOR_AREA_SELECTED);
					else
						g.setColor(ToolConfig.COLOR_POP_AREA);
					
					g.fillPolygon(x, y, p.getExteriorRing().getNumPoints());
					
//					com.vividsolutions.jts.geom.Point c = p.getCentroid();
//					Point2D centerPoint = this.getTileFactory().geoToPixel(new GeoPosition(c.getY(),c.getX()), this.getZoom());
//					g.setColor(Color.black);
//					
//					g.drawString(""+id, (int)(centerPoint.getX()-b.x), (int)(centerPoint.getY()-b.y));
				}
				
				
			}
			
			if (editMode)
			{
				Point2D wldPoint0 = this.getTileFactory().geoToPixel(this.c0, this.getZoom());
				Point2D wldPoint1 = this.getTileFactory().geoToPixel(this.c1, this.getZoom());
				
				Point sc0 = new Point((int)(wldPoint0.getX() - b.x), (int)(wldPoint0.getY() - b.y));
				Point sc1 = new Point((int)(wldPoint1.getX() - b.x), (int)(wldPoint1.getY() - b.y));

				int r = (int) (Math.sqrt(Math.pow(sc0.x-sc1.x, 2)+Math.pow(sc0.y-sc1.y, 2))+0.5);
				
//				g.setColor(new Color(255,127,0,255));
//				g.drawOval(sc0.x-r, sc0.y-r, 2*r, 2*r);
				g.setColor(ToolConfig.COLOR_HOVER);
				g.fillOval(sc0.x-r+1, sc0.y-r+1, 2*r-2, 2*r-2);
				
			}

		}
		drawing = false;
	}

	public void setSelectedArea(int id)
	{
		selectedAreaId  = id;
		
	}

	public void removeArea(int id)
	{
		while(drawing)
			continue;
		
		this.polygons.remove(id);
		
	}
	
	public HashMap<Integer, Polygon> getPolygons() {
		return polygons;
	}

}
