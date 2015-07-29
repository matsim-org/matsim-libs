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

package org.matsim.contrib.evacuation.view.renderer;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.EventListener;

import org.matsim.contrib.evacuation.control.Controller;

/**
 * abstract class for slippy map renderers
 * 
 * @author wdoering
 *
 */

public abstract class AbstractSlippyMapRenderLayer extends AbstractRenderLayer
{

	public AbstractSlippyMapRenderLayer(Controller controller)
	{
		super(controller);
	}
	
	public ArrayList<EventListener> getInheritedEventListeners()
	{
		return null;
	}
	
	public Rectangle getViewportBounds()
	{
		return null;
	}

	public int getZoom()
	{
		return 0;
	}


	public Point2D pixelToGeo(Point2D point)
	{
		return null;
	}

	public Point geoToPixel(Point2D point)
	{
		return null;
	}

	public void setZoom(int zoom)
	{
				
	}
	
	public Rectangle getBounds()
	{
		return null;

	}

	public void setPosition(Point2D position)
	{
		
	}

		
	public void updateMapImage() {
	}

}
