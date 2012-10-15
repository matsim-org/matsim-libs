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

package playground.wdoering.grips.evacuationanalysis;


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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.grips.config.ToolConfig;
import org.matsim.core.api.experimental.events.AgentDepartureEvent;
import org.matsim.core.network.LinkQuadTree;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.collections.QuadTree.Rect;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import playground.wdoering.grips.evacuationanalysis.EvacuationAnalysis.Mode;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Polygon;

public class MyMapViewer extends JXMapViewer implements MouseListener, MouseWheelListener, KeyListener, MouseMotionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	boolean editMode = false;
	boolean freezeMode = false;
	
	private enum ColoringMode { RYG };


	private final MouseListener m [];
	private final MouseMotionListener mm [];
	private final MouseWheelListener mw [];
	private final KeyListener k [];


	private ColoringMode coloringMode = ColoringMode.RYG;


	private ArrayList<Link> links;

	private Point currentMousePosition = null;

	private final GeotoolsTransformation ct;
	private final GeotoolsTransformation ctInverse;

	private final ArrayList<Link> currentHoverLinks;


	private final EvacuationAnalysis evacAnalysis;

	private Polygon areaPolygon;
	private double gridSize;

	private double minX = Double.NaN;
	private double minY = Double.NaN;
	private double maxX = Double.NaN;
	private double maxY = Double.NaN;

	private QuadTree<Cell> cellTree;

	private EventData data;

	private boolean drawNetworkBoundingBox = false;
	
	private float cellTransparency;

	private Cell selectedCell;

	private Mode mode;


	public MyMapViewer(EvacuationAnalysis evacAnalysis) {
		super();
		
		this.cellTree = null;
		
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

		this.evacAnalysis = evacAnalysis;
		this.gridSize = evacAnalysis.getGridSize();
		this.cellTransparency = evacAnalysis.getCellTransparency();
		this.mode = evacAnalysis.getMode();

		this.ct = new GeotoolsTransformation("EPSG:4326",this.evacAnalysis.getScenario().getConfig().global().getCoordinateSystem());
		this.ctInverse = new GeotoolsTransformation(this.evacAnalysis.getScenario().getConfig().global().getCoordinateSystem(),"EPSG:4326");
		createNetworkLinks();
		this.currentHoverLinks = new ArrayList<Link>();
	}


	private void createNetworkLinks(){

		Envelope e = null;
		for (Node n : this.evacAnalysis.getScenario().getNetwork().getNodes().values()) {
			Coord c = n.getCoord();
			if (e == null) {
				e = new Envelope();
			}
			e.expandToInclude(c.getX(), c.getY());
		}
		
		minX = minY = Double.POSITIVE_INFINITY;
		maxX = maxY = Double.NEGATIVE_INFINITY;

		this.links = new ArrayList<Link>();

		NetworkImpl net = (NetworkImpl) this.evacAnalysis.getScenario().getNetwork();

		for (Link link: net.getLinks().values())
		{
			if ((link.getId().toString().contains("el")) || (link.getId().toString().contains("en")) )
				continue;
			
			minX = Math.min(minX, Math.min(link.getFromNode().getCoord().getX(),link.getToNode().getCoord().getX()));
			minY = Math.min(minY, Math.min(link.getFromNode().getCoord().getY(),link.getToNode().getCoord().getY()));
			maxX = Math.max(maxX, Math.max(link.getFromNode().getCoord().getX(),link.getToNode().getCoord().getX()));
			maxY = Math.max(maxY, Math.max(link.getFromNode().getCoord().getY(),link.getToNode().getCoord().getY()));
			this.links.add(link);

		}
		
		
		

	}

	@Override
	public void mouseClicked(MouseEvent e)
	{

		//if left mouse button was clicked
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			//if edit mode is off
//			if (!this.editMode)
//			{
//				//if there was no prior selection
//				if (!this.freezeMode)
//				{
//					//activate edition mode (in gui)
//					this.evacSel.setEditMode(true);
//
//					if ((this.currentHoverLinks!=null) && (this.currentHoverLinks.size()>0))
//					{
//						//links are being selected. Freeze the selection
//						this.freezeMode = true;
//
//						//give gui the id of the first selected link
//						this.evacSel.setLink1Id(this.currentHoverLinks.get(0).getId());
//
//						//if there are more then just one link in hover
//						if (this.currentHoverLinks.size()>1)
//						{
//							//give gui the second selection link
//							this.evacSel.setLink2Id(this.currentHoverLinks.get(1).getId());
//						}
//						else
//							//make sure the second link is null then
//							this.evacSel.setLink2Id(null); 
//
//					}
//					else
//					{
//						//if nothing is selected, set them null
//						this.evacSel.setLink1Id(null);
//						this.evacSel.setLink2Id(null);						
//					}
//				}
//				else
//				{
//					this.evacSel.setEditMode(false);
//					this.freezeMode = false;
//				}
//			}
		}


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


		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mousePressed(e);
			}
		}

	}

	@Override
	public void mouseReleased(MouseEvent e) {

		if (!this.editMode)
		{
			for (MouseListener m : this.m)
			{
				m.mouseReleased(e);
			}
		}


		if (e.getButton() == MouseEvent.BUTTON1)
			repaint();

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
	public void mouseDragged(MouseEvent arg0)
	{
		if (!this.editMode)
		{
			for (MouseMotionListener m : this.mm)
			{
				m.mouseDragged(arg0);
			}
		}
		else
			repaint();

	}

	@Override
	public void mouseMoved(MouseEvent arg0)
	{

		if (!this.editMode)
		{

			this.currentMousePosition = new Point(arg0.getX(), arg0.getY());
			repaint();

			for (MouseMotionListener m : this.mm)
			{
				m.mouseMoved(arg0);
			}
		}		
	}

	@Override
	public void paint(Graphics g){
		//paint map and links
		super.paint(g);
		{
			Graphics2D g2D = (Graphics2D) g;     
			g2D.setStroke(new BasicStroke(5F));
			
			//get viewport offset
			Rectangle b = this.getViewportBounds();
			
			//draw area polygon
			if (areaPolygon == null)
				areaPolygon = this.evacAnalysis.getAreaPolygon();
			
			if (areaPolygon != null)
			{
				if (mode.equals(Mode.EVACUATION))
					g.setColor(ToolConfig.COLOR_EVAC_AREA_BORDER);
				else
					g.setColor(ToolConfig.COLOR_EVAC_AREA);
				
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
				
				if (mode.equals(Mode.EVACUATION))
					g.setColor(ToolConfig.COLOR_EVAC_AREA);
				else
					g.setColor(ToolConfig.COLOR_DISABLED_TRANSPARENT);
					
				g.fillPolygon(x, y, areaPolygon.getExteriorRing().getNumPoints());
			}

//			if ((!this.freezeMode)&&(this.currentHoverLinks.size()>0))
//				this.currentHoverLinks.clear();
//
//			GeoPosition wPoint = null;

			/*
			 * 
			//get geo mouseposition
			if (this.currentMousePosition!=null){
				Point wldPoint = new Point(this.currentMousePosition.x+b.x,this.currentMousePosition.y+b.y);
				wPoint = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());


				Coord wCoord = new CoordImpl(wPoint.getLongitude(), wPoint.getLatitude());
				wCoord = this.ct.transform(wCoord);

				//			System.out.println(wCoord);
				//go through all links, draw them and check if the mouse cursor is nearby (for highlighting)
				//			Collection<Coord[]> collection = this.links.get(wCoord.getX(),wCoord.getY(), 1000);
				Link l = this.links.getNearest(wCoord.getX(),wCoord.getY());

				Coord from = this.ctInverse.transform(l.getFromNode().getCoord());
				Coord to = this.ctInverse.transform(l.getToNode().getCoord());

				Point2D from2D = this.getTileFactory().geoToPixel(new GeoPosition(from.getY(),from.getX()), this.getZoom());
				Point2D to2D = this.getTileFactory().geoToPixel(new GeoPosition(to.getY(),to.getX()), this.getZoom());

				int x1 = (int) (from2D.getX()-b.x);
				int y1 = (int) (from2D.getY()-b.y);
				int x2 = (int) (to2D.getX()-b.x);
				int y2 = (int) (to2D.getY()-b.y);

				g2D.setStroke(new BasicStroke(3F));

				//				//if there is already data available for the current link (road)
				//				if (this.evacSel.hasLink(fromToIds[2]))
				//					g.setColor(Color.blue);
				//				else

				
				g.setColor(ToolConfig.COLOR_ROAD_HOVER);

				int x = (x2-x1);
				int y = (y2-y1);

				//check for nearby links (mouse cursor) 
				if (wPoint!=null){
					int mouseX = this.currentMousePosition.x;
					int mouseY = this.currentMousePosition.y;

					int maxX = Math.max(x2,x1);
					int maxY = Math.max(y2,y1);

					int minX = Math.min(x2,x1);
					int minY = Math.min(y2,y1);


					if ((mouseX <= maxX) && (mouseX >= minX) && (mouseY <= maxY) && (mouseY >= minY)){
						float r1 = ((float)mouseX - (float)x1) / x;
						float r2 = ((float)mouseY - (float)y1) / y;

						//if cursor is nearby, draw roads in another color
						if ((r1 - 3f < r2) && (r1 + 3f > r2))	{
							if ((!this.freezeMode) && (this.currentHoverLinks.size()<2)) {
								this.currentHoverLinks.add(l);
								for (Link revL : l.getToNode().getOutLinks().values()) {
									if (revL.getToNode() == l.getFromNode()) {
										this.currentHoverLinks.add(revL);
										break;
									}
								}

							}
							g2D.setStroke(new BasicStroke(8F));
							g.setColor(ToolConfig.COLOR_ROAD_HOVER);
						}

					}

				}

				//draw link/road if its not a selected one 
				if ((!this.freezeMode)||(!this.currentHoverLinks.contains(l))) {
					g.drawLine(x1,y1,x2,y2);
				}
			}
			*/

			/*
			//display selected roads (with arrows)
			if ((this.freezeMode)&&(this.currentHoverLinks.size()>0))
			{
				g2D.setStroke(new BasicStroke(5F));

				//for each hover link
				for (int i = 0; i<this.currentHoverLinks.size();i++)				{
					//get the from & to nodes
					Link l = this.currentHoverLinks.get(i);

					Coord from = l.getFromNode().getCoord();
					Coord to = l.getToNode().getCoord();

					double length = Math.hypot(from.getX()-to.getX(), from.getY()-to.getY());
					double dx = to.getX() - from.getX();
					double dy = to.getY() - from.getY();
					double nX = dx/length;
					double nY = dy/length;

					//shift arrow 10% of the link length to the left
					double rightShiftX = nY * .1 * length;
					double rightShiftY = -nX * .1 * length;

					//from-to arrow
					double fXA = from.getX() + rightShiftX;
					double fYA = from.getY() + rightShiftY;
					double tXA = to.getX() + rightShiftX;
					double tYA = to.getY() + rightShiftY;
					//arrow peak is 10% of link length long;
					double leftPeakEndX = tXA-.1*length*nX + -nY * .1 * length;
					double leftPeakEndY = tYA-.1*length*nY + nX * .1 * length;
					double rightPeakEndX = tXA-.1*length*nX - -nY * .1 * length;
					double rightPeakEndY = tYA-.1*length*nY - nX * .1 * length;


					Coord tmp = this.ctInverse.transform(from);
					Point2D from2D = this.getTileFactory().geoToPixel(new GeoPosition(tmp.getY(),tmp.getX()), this.getZoom());

					tmp = this.ctInverse.transform(to);
					Point2D to2D = this.getTileFactory().geoToPixel(new GeoPosition(tmp.getY(),tmp.getX()), this.getZoom());
					int x1 = (int) (from2D.getX()-b.x);
					int y1 = (int) (from2D.getY()-b.y);
					int x2 = (int) (to2D.getX()-b.x);
					int y2 = (int) (to2D.getY()-b.y);


					tmp = this.ctInverse.transform(new CoordImpl(fXA,fYA));
					Point2D arrowFrom2D = this.getTileFactory().geoToPixel(new GeoPosition(tmp.getY(),tmp.getX()), this.getZoom());
					int ax1 = (int) (arrowFrom2D.getX()-b.x);
					int ay1 = (int) (arrowFrom2D.getY()-b.y);

					tmp = this.ctInverse.transform(new CoordImpl(tXA,tYA));
					Point2D arrowTo2D = this.getTileFactory().geoToPixel(new GeoPosition(tmp.getY(),tmp.getX()), this.getZoom());
					int ax2 = (int) (arrowTo2D.getX()-b.x);
					int ay2 = (int) (arrowTo2D.getY()-b.y);


					tmp = this.ctInverse.transform(new CoordImpl(leftPeakEndX,leftPeakEndY));
					Point2D arrowLeftPeakEnd = this.getTileFactory().geoToPixel(new GeoPosition(tmp.getY(),tmp.getX()), this.getZoom());
					int alx = (int) (arrowLeftPeakEnd.getX()-b.x);
					int aly = (int) (arrowLeftPeakEnd.getY()-b.y);

					tmp = this.ctInverse.transform(new CoordImpl(rightPeakEndX,rightPeakEndY));
					Point2D arrowRightPeakEnd = this.getTileFactory().geoToPixel(new GeoPosition(tmp.getY(),tmp.getX()), this.getZoom());
					int arx = (int) (arrowRightPeakEnd.getX()-b.x);
					int ary = (int) (arrowRightPeakEnd.getY()-b.y);


					g.setColor(ToolConfig.COLOR_ROAD_SELECTED);
					g.drawLine(x1,y1,x2,y2);

					//give each arrow a different color
					if (i ==0 ) {
						g.setColor(ToolConfig.COLOR_ROAD_1);
					} else {
						g.setColor(ToolConfig.COLOR_ROAD_2);
					}
					g.drawLine(ax1, ay1, ax2, ay2);
					g.drawLine(ax2,ay2,alx,aly);
					g.drawLine(ax2,ay2,arx,ary);


				}
			}
			*/
			
			//draw utilization
			if (mode.equals(Mode.UTILIZATION))
			{
			
				HashMap<Id, List<Tuple<Id,Double>>> linkLeaveTimes = data.getLinkLeaveTimes();
				HashMap<Id, List<Tuple<Id,Double>>> linkEnterTimes = data.getLinkEnterTimes();
				for (Link link : this.links)
				{
					List<Tuple<Id,Double>> leaveTimes = linkLeaveTimes.get(link.getId());
					List<Tuple<Id,Double>> enterTimes = linkEnterTimes.get(link.getId());
					
					if ((enterTimes != null) && (enterTimes.size() > 0) && (leaveTimes!=null))
					{
						
						Coord fromCoord = this.ctInverse.transform(new CoordImpl(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY()));
						Point2D fromP2D = this.getTileFactory().geoToPixel(new GeoPosition(fromCoord.getY(),fromCoord.getX()), this.getZoom());
						
						Coord toCoord = this.ctInverse.transform(new CoordImpl(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY()));
						Point2D toP2D = this.getTileFactory().geoToPixel(new GeoPosition(toCoord.getY(),toCoord.getX()), this.getZoom());
						
						float strokeWidth = (((float)enterTimes.size()/(float)data.getMaxUtilization())*80f) / (float)Math.pow(2,this.getZoom()); 
								
						g2D.setStroke(new BasicStroke(strokeWidth));
						
						g.setColor(Color.RED);
						g.drawLine((int)fromP2D.getX()-b.x, (int)fromP2D.getY()-b.y, (int)toP2D.getX()-b.x, (int)toP2D.getY()-b.y);
					}
					
				}
			}
			
			/**
			 * draw grid
			 * 
			 */
			if (!Double.isNaN(minX))
			{
				g.setColor(Color.BLACK);
				g2D.setStroke(new BasicStroke(1F));
				
				//get max cell time sum from data
				double maxCellTimeSum = data.getMaxCellTimeSum();
				
				//get all cells from celltree
				LinkedList<Cell> cells = new LinkedList<Cell>();
				cellTree.get(new Rect(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY), cells);
				
				this.selectedCell = null;
				for (Cell cell : cells)
				{
					g2D.setStroke(new BasicStroke(1F));
					
					//calculate travel time in relation to the overall maximum travel time per cell 
					Double relTravelTime = (cell.getTimeSum()) / maxCellTimeSum;

					//might be NAN or less than zero: make it a zero
					if ((Double.isNaN(relTravelTime)) || (relTravelTime < 0))
						relTravelTime = 0d;

					//colorize cell depending on the picked colorization, cell data and the relative travel time
					setCellColor(g, cell, relTravelTime);					
					
					//get cell coordinate (+ gridsize) and transform into pixel coordinates
					CoordImpl cellCoord = cell.getCoord();
					Coord transformedCoord = this.ctInverse.transform(new CoordImpl(cellCoord.getX(), cellCoord.getY()));
					Point2D cellCoordP2D = this.getTileFactory().geoToPixel(new GeoPosition(transformedCoord.getY(),transformedCoord.getX()), this.getZoom());
					Coord cellPlusGridCoord = this.ctInverse.transform(new CoordImpl(cellCoord.getX()+gridSize, cellCoord.getY()+gridSize));
					Point2D cellPlusGridCoordP2D = this.getTileFactory().geoToPixel(new GeoPosition(cellPlusGridCoord.getY(),cellPlusGridCoord.getX()), this.getZoom());
					
					//adjust viewport
					int gridX1 = (int)cellCoordP2D.getX()-b.x;
					int gridY1 = (int)cellCoordP2D.getY()-b.y;
					int gridX2 = (int)cellPlusGridCoordP2D.getX()-b.x;
					int gridY2 = (int)cellPlusGridCoordP2D.getY()-b.y;
					
					//make sure the first values are the smaller ones, if not: swap
					if (gridX1>gridX2)
					{
						int temp = gridX2;
						gridX2 = gridX1;
						gridX1 = temp;
					}
					if (gridY1>gridY2)
					{
						int temp = gridY2;
						gridY2 = gridY1;
						gridY1 = temp;
					}
					
					//color grid (if mode equals evacuation)
					if (mode.equals(Mode.EVACUATION))
						g.fillRect(gridX1, gridY1, gridX2-gridX1, gridY2-gridY1);
					
					//draw grid
					g.setColor(ToolConfig.COLOR_GRID);
					g.drawRect(gridX1, gridY1, gridX2-gridX1, gridY2-gridY1);
					
					if (currentMousePosition!=null)
					{
						int mouseX = this.currentMousePosition.x;
						int mouseY = this.currentMousePosition.y;
						
						if ((mouseX>=gridX1) && (mouseX<gridX2) && (mouseY>=gridY1) && (mouseY<gridY2))
						{
							g.setColor(ToolConfig.COLOR_HOVER);
							g.fillRect(gridX1, gridY1, gridX2-gridX1, gridY2-gridY1);
							g2D.setStroke(new BasicStroke(3F));
							g.drawRect(gridX1, gridY1, gridX2-gridX1, gridY2-gridY1);
							
							
							this.selectedCell = cell; 
						}
						
					}		
					
					
					
					
				}
				
				if ((this.selectedCell!=null) && (this.currentMousePosition!=null))
				{
					g.setColor(Color.white);
					g.fillRect(this.currentMousePosition.x-20, this.currentMousePosition.y+20, 120, 50);
					g.setColor(Color.black);
					g.drawRect(this.currentMousePosition.x-20, this.currentMousePosition.y+20, 120, 50);
					
					g.drawString("person count: " + selectedCell.getCount(), this.currentMousePosition.x-15, this.currentMousePosition.y+50);
					
				}
				g.setColor(Color.black);
				//////////////////////////////////
				//////////////////////////////////
			}






		}
	}


	private void setCellColor(Graphics g, Cell cell, Double relTravelTime) {
		//if is activity in the current cell (person count > 0)
		if (cell.getCount()>0)
		{
			//depending on the selected colorization, set red, green and blue values
			//RED <-> YELLOW <-> GREEN 
			if (coloringMode.equals(ColoringMode.RYG))
			{
				int red,green,blue;
				
				if (relTravelTime>.5)
				{
					red = 255;
					green = (int)(255 - 255*(relTravelTime-.5)*2);
					blue = 0;
				}
				else
				{
					red = (int)(255*relTravelTime*2);
					green = 255;
					blue = 0;
					
				}
				g.setColor(new Color(red,green,blue,(int)(255*cellTransparency)));
			}
			else
				g.setColor(new Color(0,127,(int)(255*relTravelTime),100));
		}
		else
			g.setColor(ToolConfig.COLOR_DISABLED_TRANSPARENT);
	}

	public void updateEventData(EventData data)
	{
		this.data = data;
		this.cellTree = data.getCellTree();
		this.gridSize = data.getCellSize();
		
		System.out.println("data bb: \t\t" + this.data.getBoundingBox().toString());
		Rect mvbb = new Rect(minX,minY,maxX,maxY);
		System.out.println("network link / mv bb: \t" + mvbb.toString());
		
		
	}


	public void setCellTransparency(float cellTransparency)
	{
		this.cellTransparency = cellTransparency;
		this.repaint();
	}


	public void setMode(Mode mode)
	{
		this.mode = mode;
		this.repaint();
		
	}

}
