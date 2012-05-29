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

package playground.wdoering.linkselector;


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
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.jdesktop.swingx.JXMapViewer;
import org.jdesktop.swingx.mapviewer.GeoPosition;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;

public class MyMapViewer extends JXMapViewer implements MouseListener, MouseWheelListener, KeyListener, MouseMotionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	boolean editMode = false;
	boolean freezeMode = false;
	
	
	private final MouseListener m [];
	private final MouseMotionListener mm [];
	private final MouseWheelListener mw [];
	private final KeyListener k [];

	private GeoPosition c0;
	private GeoPosition c1;

	private ShapeToStreetSnapperThreadWrapper snapper;

	private Thread thread;

	private HashMap<Id[], Coord[]> links;

	private GeotoolsTransformation ct;
	
	private Point currentMousePosition = null;

	private CoordImpl mousePoint;

	private Object GeoPosition;

	private GeoPosition wPoint;

	private ArrayList<Id[]> currentHoverLinkIds;

	private boolean displayMultipleLinks; 
	
	private EvacuationAreaSelector evacSel;

	public MyMapViewer(EvacuationAreaSelector evacSel) {
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
		
		this.evacSel = evacSel;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
//		System.out.println("klick!" + " e.getButton():" + e.getButton());
		
		if (e.getButton() == MouseEvent.BUTTON1)
		{
			if (!this.editMode)
			{
				if (!freezeMode)
				{
					evacSel.setEditMode(true);
					
					if ((currentHoverLinkIds!=null) && (currentHoverLinkIds.size()>0))
					{
						freezeMode = true;
						
						evacSel.setLink1Id(currentHoverLinkIds.get(0)[2]);
						
						Rectangle b = this.getViewportBounds();
						
						if (currentHoverLinkIds.size()>1)
						{
							displayMultipleLinks = true;
							
							evacSel.setLink2Id(currentHoverLinkIds.get(1)[2]);
							
							
//							for (int i = 0; i<currentHoverLinkIds.size(); i++)
//							{
//								Coord[] fromToCoords = links.get(currentHoverLinkIds.get(i));
//								
//								Point2D from2D = this.getTileFactory().geoToPixel(new GeoPosition(fromToCoords[0].getY(),fromToCoords[0].getX()), this.getZoom());
//								Point2D to2D = this.getTileFactory().geoToPixel(new GeoPosition(fromToCoords[1].getY(), fromToCoords[1].getX()), this.getZoom());
//								
//								int x1 = (int) (from2D.getX()-b.x);
//								int y1 = (int) (from2D.getY()-b.y);
//								int x2 = (int) (to2D.getX()-b.x);
//								int y2 = (int) (to2D.getY()-b.y);
//								
//								if (i==0)
//									
//								
//							}
						}
						else
						{
							evacSel.setLink2Id(null);
						}

						
					}
					else
					{
						evacSel.setLink1Id(null);
						evacSel.setLink2Id(null);						
					}
				}
				else
				{
					evacSel.setEditMode(false);
					freezeMode = false;
					displayMultipleLinks = false;
				}
			}
		}
		
		//TODO FIXME 
//		updateTable();
		
	}

	private void updateTable()
	{
		HashMap<Id, String> roadClosures = evacSel.getRoadClosures();
		
		if (roadClosures!=null)
		{
			String[] columnNames = {"ID", "time"};
			Object[][] data = new Object[roadClosures.size()][2];
			
			Iterator it = roadClosures.entrySet().iterator();
			int i = 0;
		    while (it.hasNext())
		    {
		        Map.Entry pairs = (Map.Entry)it.next();
		        
		        data[i][0] = (String)pairs.getKey().toString();
		        data[i][1] = (String)pairs.getValue();
		        
		        System.out.println("i:" + i + " ; " + pairs.getKey().toString());
		        i++;
		    }
			
//		    Tab
			
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
		
		if (e.getButton() == MouseEvent.BUTTON1)
		{
//			this.editMode = true;
//			if (this.thread != null)
//			{
////				System.out.println("interrupt");
//				this.thread.stop();
//			}
//			
//			this.snapper.reset();
//			
//			Rectangle b = this.getViewportBounds();
//			Point p0 = e.getPoint();
//			Point wldPoint = new Point(p0.x+b.x,p0.y+b.y);
//			
//			this.c0 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
		}
		
		if (!this.editMode) {
			for (MouseListener m : this.m) {
				m.mousePressed(e);
			}
		}
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		
		//forward standard jxmapviewer listener handling 
		if (!this.editMode)
		{
			for (MouseListener m : this.m)
			{
				m.mouseReleased(e);
			}
		}
		
		
		if (e.getButton() == MouseEvent.BUTTON1)
		{
//			this.editMode = false;
//			Point p1 = e.getPoint();
//			Rectangle b = this.getViewportBounds();
//			Point wldPoint = new Point(p1.x+b.x,p1.y+b.y);
//			
//			this.c1 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
//			this.snapper.setCoordinates(this.c0,this.c1);
////			this.thread.interrupt();
//			this.thread = new Thread(this.snapper);
//			this.thread.start();
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
		{
//			Point p = arg0.getPoint();
//			Rectangle b = this.getViewportBounds();
//			Point wldPoint = new Point(p.x+b.x,p.y+b.y);
//			this.c1 = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
			repaint();
		}
		
	}

	@Override
	public void mouseMoved(MouseEvent arg0)
	{
		
		if (!this.editMode)
		{
//			System.out.println("x: " + arg0.getX() + " | y: " + arg0.getY());
			
			this.currentMousePosition = new Point(arg0.getX(), arg0.getY());
			repaint();
			
			for (MouseMotionListener m : this.mm)
			{
				m.mouseMoved(arg0);
			}
		}		
	}

	@Override
	public void paint(Graphics g)
	{
		super.paint(g);
		{
			Rectangle b = this.getViewportBounds();
			
			g.setColor(Color.black);
			Graphics2D g2D = (Graphics2D) g;     
		    g2D.setStroke(new BasicStroke(5F));
			Polygon p = this.snapper.getPolygon();
			
			if (links==null)
			{
				links = this.snapper.getNetworkLinks();
				currentHoverLinkIds = new ArrayList<Id[]>();
//				ct =  new GeotoolsTransformation("EPSG:32633", "EPSG:4326"); 
				
			}
				
			if (links!=null)
				
				
				if ((!freezeMode)&&(currentHoverLinkIds.size()>0))
					currentHoverLinkIds.clear();
				
				wPoint = null;
			
				if (currentMousePosition!=null)
				{
					Point wldPoint = new Point(currentMousePosition.x+b.x,currentMousePosition.y+b.y);
					wPoint = this.getTileFactory().pixelToGeo(wldPoint, this.getZoom());
					
				}
				
				
			    Iterator it = links.entrySet().iterator();
			    
			    
			    while (it.hasNext())
			    {
			    	
			        Map.Entry pairs = (Map.Entry)it.next();
			        Id[] fromToIds = (Id[]) pairs.getKey();
			        Coord[] fromToCoords = (Coord[]) pairs.getValue();
			        
					Point2D from2D = this.getTileFactory().geoToPixel(new GeoPosition(fromToCoords[0].getY(),fromToCoords[0].getX()), this.getZoom());
					Point2D to2D = this.getTileFactory().geoToPixel(new GeoPosition(fromToCoords[1].getY(), fromToCoords[1].getX()), this.getZoom());
					
					int x1 = (int) (from2D.getX()-b.x);
					int y1 = (int) (from2D.getY()-b.y);
					int x2 = (int) (to2D.getX()-b.x);
					int y2 = (int) (to2D.getY()-b.y);
					
					g2D.setStroke(new BasicStroke(5F));
					
					if (evacSel.hasLink(fromToIds[2]))
						g.setColor(Color.blue);
					else
						g.setColor(new Color(255,255,0,100));
					
					int x = (x2-x1);
					int y = (y2-y1);
					
					if (wPoint!=null)
					{
						int mouseX = currentMousePosition.x;
						int mouseY = currentMousePosition.y;
						
						int maxX = Math.max(x2,x1);
						int maxY = Math.max(y2,y1);
						
						int minX = Math.min(x2,x1);
						int minY = Math.min(y2,y1);
						
						CoordImpl wCoord = new CoordImpl(wPoint.getLongitude(), wPoint.getLatitude());

						//Collection<Node> nodes = this.snapper.getNearestNodes(wCoord);
						
//						for (Node node : nodes)
//						{
////							System.out.println("_:_:_:_:_:_" + node.getId());
//						}
						
//						if ( (y2-y1)*x3 + (-(x2-x1) * y3 ) == (y2-y1)*x2 + (-(x2-x1)*y2))

						if ((mouseX <= maxX) && (mouseX >= minX) && (mouseY <= maxY) && (mouseY >= minY))
						{
//							( MX - AX ) / BX = R
//							( MY - AY ) / BY = R
							
//							float r1 = ((float)mouseX - (float)minX) / (float)maxX;
//							float r2 = ((float)mouseY - (float)minY) / (float)maxY;
							float r1 = ((float)mouseX - (float)x1) / (float)x;
							float r2 = ((float)mouseY - (float)y1) / (float)y;
							
							if ((r1 - .3f < r2) && (r1 + .3f > r2))
							{
																	
										
//								if (currentHoverLinkIds.size()>0)
//								{
//									Id[] tempFromToIds;
//								}	
								
								if ((!freezeMode) && (currentHoverLinkIds.size()<2))
										currentHoverLinkIds.add(fromToIds);
								
								g2D.setStroke(new BasicStroke(8F));
								g.setColor(Color.blue);
							}
						
						}
						
					}
					
					
					if ((freezeMode)&&(currentHoverLinkIds.contains(fromToIds)))
					{
//						g.drawLine(x1,y1,x2,y2);
//						
//						if (currentMultipleLink==0)
//							g.setColor(new Color(255,0,0,250));
//						else if (currentMultipleLink==1)
//							g.setColor(new Color(0,255,0,250));
//						else
//							g.setColor(new Color(0,0,255,50));
//						
//						
//						x1+=(-y)/8;
//						y1+=x/8;
//						x2+=(-y)/8;
//						y2+=x/8;
//						
////						if (currentMultipleLink==0)
////						{
////						}
////						if (currentMultipleLink==1)
////						{
////							x1-=(-y)/8;
////							y1-=x/8;
////							x2-=(-y)/8;
////							y2-=x/8;
//////							x1-=Math.max(Math.abs(y)/8,20);
//////							y1-=Math.max(Math.abs(x)/8,20);
//////							x2-=Math.max(Math.abs(y)/8,20);
//////							y2-=Math.max(Math.abs(x)/8,20);
////						}
//						
//						int leftArrowX = x1 + (-y)/5 + x/5;
//						int leftArrowY = y1 + x/5  + y/5;
//						int rightArrowX = x1 - (-y)/5 +  x/5;
//						int rightArrowY = y1 - x/5  + y/5;
//						
//						g.drawLine(x1,y1,leftArrowX,leftArrowY);
//						g.drawLine(x1,y1,rightArrowX,rightArrowY);
//						g.drawLine(x1,y1,x2,y2);
//						
//						currentMultipleLink++;
						
//						int length = (int)Math.sqrt(Math.abs(x1-x2)*Math.abs(x1-x2) + Math.abs(y1-y2)*Math.abs(y1-y2));
//						g.fillOval((x1+x2)/2 - length/2, (y1+y2)/2 - length/2, length, length);
						
						
					}
					else
						g.drawLine(x1,y1,x2,y2);
					
					g.setColor(Color.CYAN);
				}
			    
			    if ((freezeMode)&&(currentHoverLinkIds.size()>0))
			    {
					g2D.setStroke(new BasicStroke(5F));

					int currentMultipleLink = 0;
					
			    	for (int i = 0; i<currentHoverLinkIds.size();i++)
			    	{
			    		
			    		Coord[] fromToCoords = links.get(currentHoverLinkIds.get(i));
			    		
			    		
			    		if (fromToCoords!=null)
			    		{
				    		
							Point2D from2D = this.getTileFactory().geoToPixel(new GeoPosition(fromToCoords[0].getY(),fromToCoords[0].getX()), this.getZoom());
							Point2D to2D = this.getTileFactory().geoToPixel(new GeoPosition(fromToCoords[1].getY(), fromToCoords[1].getX()), this.getZoom());
							
							int x1 = (int) (from2D.getX()-b.x);
							int y1 = (int) (from2D.getY()-b.y);
							int x2 = (int) (to2D.getX()-b.x);
							int y2 = (int) (to2D.getY()-b.y);
							
							int x =(x2-x1);
							int y =(y2-y1);
							
//							int maxVal = Math.max(Math.abs(x), Math.abs(y));
//							
//							float xN = (float)x/(float)maxVal;
//							float yN = (float)y/(float)maxVal;
							
							double xD = fromToCoords[0].getX()-fromToCoords[1].getX();
							double yD = fromToCoords[1].getY()-fromToCoords[0].getY();
							
							double maxVal = Math.max(Math.abs(xD), Math.abs(yD));
							
							float xN = (float)xD/(float)maxVal;
							float yN = (float)yD/(float)maxVal;
							
							int pivotX = (x1+x2)/2;
							int pivotY = (y1+y2)/2;
							
							
				    		
							
							if (currentMultipleLink==0)
								g.setColor(new Color(255,0,0,90));
							else if (currentMultipleLink==1)
								g.setColor(new Color(0,255,0,90));
							else if (currentMultipleLink==2)
								g.setColor(new Color(0,0,255,90));
							else
								g.setColor(new Color(127,255,0,90));
							
							g.drawLine(x1,y1,x2,y2);
							
							if (currentMultipleLink==0)
							{
								g.setColor(new Color(255,0,0));
								pivotX+=(int)(xN*15);
								pivotY+=(int)(yN*15);
//								System.out.println(pivotX+"|" + pivotY + " - mode:" + currentMultipleLink);
							}
							else if (currentMultipleLink==1)
							{
								g.setColor(new Color(0,255,0));
								pivotX-=(int)(xN*15);
								pivotY-=(int)(yN*15);
//								System.out.println(pivotX+"|" + pivotY + " - mode:" + currentMultipleLink);
							}
							else if (currentMultipleLink==2)
							{
								g.setColor(new Color(0,0,255));
							}
							else
							{
								g.setColor(new Color(255,128,0));
							}
							
							x1+=(-y)/8;
							y1+=x/8;
							x2+=(-y)/8;
							y2+=x/8;
							
//							int length = (int)Math.sqrt(Math.abs(x)*Math.abs(x) + Math.abs(y)*Math.abs(y));
//							if (length<10)
//							{
//								x*=10;
//								y*=10;
//							}
							
							int arrowX1 = pivotX - (int)(50*xN);
							int arrowY1 = pivotY - (int)(50*yN);
							int arrowX2 = pivotX + (int)(50*xN);
							int arrowY2 = pivotY + (int)(50*yN);
							
							x = (int)(20*xN);
							y = (int)(20*yN);
								
							
							int leftArrowX = arrowX2 - (-y) - x;
							int leftArrowY = arrowY2 - x  - y;
							int rightArrowX = arrowX2 + (-y) -  x;
							int rightArrowY = arrowY2 + x  - y;
//							int leftArrowX = x2 - (-y)/5 - x/5;
//							int leftArrowY = y2 - x/5  - y/5;
//							int rightArrowX = x2 + (-y)/5 -  x/5;
//							int rightArrowY = y2 + x/5  - y/5;
							
							g.drawLine(arrowX2,arrowY2,leftArrowX,leftArrowY);
							g.drawLine(arrowX2,arrowY2,rightArrowX,rightArrowY);
//							g.drawLine(x2,y2,leftArrowX,leftArrowY);
//							g.drawLine(x2,y2,rightArrowX,rightArrowY);
							g.drawLine(arrowX1,arrowY1,arrowX2,arrowY2);
							
							currentMultipleLink++;
			    		}
			    	}
					
					
//					g.fillOval((x1+x2)/2 - length/2, (y1+y2)/2 - length/2, length, length);
					
					
				}			    	
			    
			    
//			    for (int i = 0; i <currentHoverLinkIds.size(); i++)
//			    {
//			    	
////					if(tempFromToIds[0] == currentHoverLinkIds.get(i)[0])
//					System.out.println("elem " + i + ": " + currentHoverLinkIds.get(i)[0].toString() + "-" + currentHoverLinkIds.get(i)[1].toString() );
//			    }
			    
			
			
			
			if (p != null)
			{
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
				g.setColor(new Color(255,0,255,128));
				g.fillPolygon(x, y, p.getExteriorRing().getNumPoints());
				
				
			}
			else
			{
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
