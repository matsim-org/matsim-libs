/******************************************************************************
 *project: org.matsim.*
 * BasicMap.java
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

import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import playground.rost.graph.BoundingBox;

public interface BasicMap {
		
		public void resetView();
		
		public void setStatusInfo(String str);
		
		public void UIChange();
		
		public int getXonPanel(double x);
		
		public int getYonPanel(double y);	
		
		public int getXonPanel(Node node);
		
		public int getYonPanel(Node node);
		
		public double getX(int x);
		
		public double getY(int y);
		
		public void setBoundingBox(BoundingBox bBox);
		
		public void addMapPaintCallback(MapPaintCallback callback);
		
		public void removeMapPaintCallback(MapPaintCallback callback);
		
		public Container getContainer();
		
		public double getZoom();
		
		public boolean isVisible(Node n);
		
		public boolean isVisible(Link l);
		
		public boolean isVisible(double x, double y);
		
		public void handleMouseClick(MouseEvent event);
		
		public void handleMouseReleased(MouseEvent event);

		public void handleMousePressed(MouseEvent event);
		
		public void handleMouseWheelEvent(MouseWheelEvent event);
}

