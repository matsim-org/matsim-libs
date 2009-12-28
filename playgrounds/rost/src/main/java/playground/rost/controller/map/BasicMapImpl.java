/******************************************************************************
 *project: org.matsim.*
 * BasicMapImpl.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.map;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkLayer;

import playground.rost.graph.BoundingBox;
import playground.rost.graph.GraphAlgorithms;

public class BasicMapImpl extends JPanel implements BasicMap{
	public NetworkLayer network;
	
	double minX, minY, maxX, maxY;
	
	int borderX = 10;
	int borderY = 30;
	
	double zoom = 1.0;
	
	double xRange;
	double yRange;
	int draw_width;
	int draw_height;
	
	int xMousePressed;
	int yMousePressed;
	
	JLabel statusInfo;
	
	BoundingBox bBox = null;
	
	Set<MapPaintCallback> paintCallbacks = new HashSet<MapPaintCallback>();
	
	public void setBoundingBox(BoundingBox bBox)
	{
		this.bBox = bBox;
		resetView();
	}
	
	public void handleMousePressed(MouseEvent event)
	{
		xMousePressed = event.getX();
		yMousePressed = event.getY();
	}
	
	public void handleMouseReleased(MouseEvent event)
	{
		int xMouseDiff = event.getX() - xMousePressed;
		int yMouseDiff = event.getY() - yMousePressed;
		double xDiff = (xRange / (this.getWidth()-borderX))*xMouseDiff;
		double yDiff = (yRange / (this.getHeight()-borderY))*yMouseDiff;
		maxX -=xDiff;
		minX -=xDiff;
		maxY +=yDiff;
		minY +=yDiff;
		UIChange();
	}
	
	public void handleMouseClick(MouseEvent event)
	{
		if(event.getButton() == MouseEvent.BUTTON1)
		{
			//bestimme x und y koordinaten
			double xClicked = getX(event.getX());
			double yClicked = getY(event.getY());
			minX = xClicked - xRange / 2.;
			maxX = xClicked + xRange / 2.;
			minY = yClicked - yRange / 2.;
			maxY = yClicked + yRange / 2.;
		}
		UIChange();
	}
	
	public void handleMouseWheelEvent(MouseWheelEvent event)
	{
		if(event.getWheelRotation() > 0)
		{
			double xRangeNew = xRange / Math.pow(1.2, event.getWheelRotation());
			double yRangeNew = yRange / Math.pow(1.2, event.getWheelRotation());
			double xDiff = xRange - xRangeNew;
			double yDiff = yRange - yRangeNew;
			minX += xDiff / 2;
			maxX -= xDiff / 2;
			minY += yDiff / 2;
			maxY -= yDiff / 2;
			xRange = xRangeNew;
			yRange = yRangeNew;
			zoom *= Math.pow(1.2, event.getWheelRotation());
		}
		else
		{
			double xRangeNew = xRange * Math.pow(1.2, -event.getWheelRotation());
			double yRangeNew = yRange * Math.pow(1.2, -event.getWheelRotation());
			double xDiff = xRangeNew-xRange;
			double yDiff = yRangeNew-yRange;
			minX -= xDiff / 2;
			maxX += xDiff / 2;
			minY -= yDiff / 2;
			maxY += yDiff / 2;
			xRange = xRangeNew;
			yRange = yRangeNew;
			zoom /= Math.pow(1.2, -event.getWheelRotation());	
		}
		UIChange();
	}
	
	public void resetView()
	{
		if(this.bBox == null)
			return;
		this.zoom = 1;
		maxX = bBox.getMaxX(); 
		minX = bBox.getMinX();
		maxY = bBox.getMaxY();
		minY = bBox.getMinY();
		adaptToAspectRatio();
	}
	
//	protected void adaptToAspectRatio()
//	{
//		draw_width = this.getWidth() - 2*borderX;
//		draw_height = this.getHeight()- 2*borderY;
//		double quoDraw = (double)draw_width / (double)draw_height;
//		xRange = maxX - minX;
//		yRange = maxY - minY;
//		double quoReal = yRange / xRange;
//		if(quoDraw / quoReal > 1)
//		{
//			double newXRange = (GraphAlgorithms.dX / GraphAlgorithms.dY)* xRange / (quoDraw / quoReal);
//			minX = minX + (newXRange-xRange)/2;
//			maxX = maxX - (newXRange-xRange)/2;
//		}
//		else if(quoDraw / quoReal < 1)
//		{
//			double newYRange = yRange / (quoDraw / quoReal);
//			minY = minY - (newYRange-yRange)/2;
//			maxY = maxY + (newYRange-yRange)/2;
//		}
//		quoReal = xRange / yRange * (GraphAlgorithms.dY / GraphAlgorithms.dX);
//		xRange = maxX - minX;
//		yRange = maxY - minY;
//
//	}

	protected void adaptToAspectRatio()
	{
		draw_width = this.getWidth() - 2*borderX;
		draw_height = this.getHeight()- 2*borderY;
//		if(draw_width < draw_height)
//			draw_height = draw_width;
//		else
//			draw_width = draw_height;
		xRange = maxX - minX;
		yRange = maxY - minY;
		double quoDraw = (double)draw_width / (double)draw_height;
		if(quoDraw < 1)
		{	
			double newXRange = yRange * GraphAlgorithms.dY / GraphAlgorithms.dX* quoDraw;
			maxX += (newXRange - xRange) / 2;
			minX -= (newXRange - xRange) / 2;
			xRange = maxX - minX;
		}
		else
		{
			double newYRange = xRange * GraphAlgorithms.dX/ GraphAlgorithms.dY / quoDraw;
			maxY += (newYRange - yRange) / 2;
			minY -= (newYRange - yRange) / 2;
			yRange = maxY - minY;
		}
		UIChange();
	}

	
	public void setStatusInfo(String str)
	{
		statusInfo.setText(str);
	}
	
	public BasicMapImpl()
	{
		statusInfo = new JLabel();
		this.setLayout(new BorderLayout());
		this.add(statusInfo, BorderLayout.SOUTH);
		
		resetView();
		
		//setup Listeners
		this.addMouseListener(
				new MouseListener(){
					public void mouseClicked(MouseEvent event)
					{
						handleMouseClick(event);
					}
					
					public void mouseExited(MouseEvent event)
					{ //do nothing
					}
					
					public void mouseEntered(MouseEvent event)
					{//do nothing
					}
					
					public void mouseReleased(MouseEvent event)
					{
						handleMouseReleased(event);
					}
					
					public void mousePressed(MouseEvent event)
					{
						handleMousePressed(event);
					}
				}
			);
		
		this.addMouseWheelListener( new MouseWheelListener(){
			public void mouseWheelMoved(MouseWheelEvent e)
			{
				handleMouseWheelEvent(e);	
			}
		}
		);
		
		this.addComponentListener(new ComponentAdapter() 
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				adaptToAspectRatio();
				UIChange();
			}
		});
	}
	
	public void UIChange()
	{
		this.repaint();
		setStatusInfo("  X: [" + minX + "," + maxX + "] | Y: [" + minY + "," + maxY + "]");
	}
	
	public int getXonPanel(double x)
	{
		int resultX;
		resultX = (int)(((x - minX) / xRange)*draw_width);
		resultX += borderX;
		return resultX;
	}
	
	public int getYonPanel(double y)
	{
		int resultY;
		resultY = (int)(((y - minY) / yRange)*draw_height);
		resultY += borderY;
		resultY = this.getHeight() - resultY;
		return resultY;
	}
	
	public int getXonPanel(Node node)
	{
		int resultX;
		resultX = (int)(((node.getCoord().getX() - minX) / xRange)*draw_width);
		resultX += borderX;
		return resultX;
	}
	
	public int getYonPanel(Node node)
	{
		int resultY;
		resultY = (int)(((node.getCoord().getY() - minY) / yRange)*draw_height);
		resultY += borderY;
		resultY = this.getHeight() - resultY;
		return resultY;
	}
	
	public double getX(int x)
	{	
		double result;
		x -= borderX;
		result = ((double)x / (double)draw_width)*xRange + minX;
		return result;
	}
	
	public double getY(int y)
	{
		double result;
		y += borderY;
		y = this.getHeight() - y;
		result = ((double)y / (double)draw_height)*yRange + minY;
		return result;
	}
	
	public void addMapPaintCallback(MapPaintCallback callback)
	{
		paintCallbacks.add(callback);
	}
	
	public void removeMapPaintCallback(MapPaintCallback callback)
	{
		paintCallbacks.remove(callback);
	}
	
	public Container getContainer()
	{
		return this;
	}
	
	public double getZoom()
	{
		return zoom;
	}
	
	@Override
	public synchronized void paintComponent( Graphics g )
	{
		g.clearRect(0, 0, this.getWidth(),this.getHeight());
		super.paintComponents(g);
		for(MapPaintCallback callback : paintCallbacks)
		{
			callback.paint(this, g);
		}
		g.clearRect(0, this.getHeight()-borderY, this.getWidth(), borderY);
		g.clearRect(0, 0, this.getWidth(), borderY);
		
		g.clearRect(this.getWidth()-borderX, 0, borderX, this.getHeight());
		g.clearRect(0, 0, borderX, this.getHeight());
		
		g.setColor(Color.black);
		
		g.drawRect(borderX / 2, borderY / 2, this.getWidth()-borderX, this.getHeight()-borderY);
	}
	

	public boolean isVisible(Node node)
	{
		if(node.getCoord().getX() < minX || node.getCoord().getX() > maxX || node.getCoord().getY() < minY || node.getCoord().getY() > maxY)
			return false;
		return true;	
	}
	
	public boolean isVisible(Link link)
	{
		return isVisible(link.getFromNode()) || isVisible(link.getToNode());
	}
	
	public boolean isVisible(double x, double y)
	{
		if(x < minX || x > maxX || y < minY || y > maxY)
			return false;
		return true;	

	}
}
