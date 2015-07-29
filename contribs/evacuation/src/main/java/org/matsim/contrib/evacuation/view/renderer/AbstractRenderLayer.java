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

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.imagecontainer.ImageContainerInterface;

/**
 * abstrat class defining render layer basics
 * 
 * @author wdoering
 *
 */
public abstract class AbstractRenderLayer
{
	protected int id;
	protected final Controller controller;
	protected final ImageContainerInterface imageContainer;
	protected boolean enabled = false;
	
	protected static int currentId = -1;
	
	public AbstractRenderLayer(Controller controller)
	{
		this.controller = controller;
		this.imageContainer = controller.getImageContainer();
		this.id = ++currentId;
	}
	
	public ImageContainerInterface getImageContainer()
	{
		return imageContainer;
	}
	
	public static int getCurrentId()
	{
		return currentId;
	}
	
	public int getId()
	{
		return id;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}
	
	public void updatePixelCoordinates(boolean all)
	{
		return;
	}

	public synchronized void paintLayer() {}
}
