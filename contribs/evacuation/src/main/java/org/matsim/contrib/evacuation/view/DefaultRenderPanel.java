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

package org.matsim.contrib.evacuation.view;

import java.awt.Graphics;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.matsim.contrib.evacuation.control.Controller;

/**
 * container class for the slippy map.
 * 
 * @author wdoering
 *
 */
public class DefaultRenderPanel extends JPanel implements ComponentListener
{
	private static final long serialVersionUID = 1L;
	private Visualizer visualizer;
	private int border;
	private BufferedImage image;

	public DefaultRenderPanel(Controller controller)
	{
		
		this(controller.getVisualizer(), (BufferedImage) controller.getImageContainer().getImage(), controller.getImageContainer().getBorderWidth());
	}

	public DefaultRenderPanel(Visualizer visualizer, BufferedImage image, int border)
	{
		this.visualizer = visualizer;
		this.image = image;
		this.border = border;
		this.addComponentListener(this);
	}

	@Override
	public void paint(Graphics g)
	{
		this.visualizer.paintLayers();
		g.drawImage((BufferedImage) this.image, this.border, this.border, null);
	}
	
	public void updateImageContainer()
	{
		this.image = visualizer.getBufferedImage();
		this.border = visualizer.getImageContainer().getBorderWidth();
	}

	@Override
	public void componentHidden(ComponentEvent e) {}
	@Override
	public void componentMoved(ComponentEvent e) {}
	@Override
	public void componentResized(ComponentEvent e)
	{
		updateImageContainer();
	}
	@Override
	public void componentShown(ComponentEvent e) {}
}