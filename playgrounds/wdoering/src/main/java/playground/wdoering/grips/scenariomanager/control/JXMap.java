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

package playground.wdoering.grips.scenariomanager.control;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.List;

import org.jdesktop.swingx.JXMapViewer;

public class JXMap extends JXMapViewer
{

	private static final long serialVersionUID = 1L;

	private MouseListener m[];
	private MouseMotionListener mm[];
	private MouseWheelListener mw[];
	private KeyListener k[];
	private ArrayList<EventListener> listeners;

	private Controller controller;
	private boolean fromAbove = false;
	
	private boolean enabled = false;

	public JXMap(Controller controller)
	{
		super();
		this.controller = controller;
		resetListeners();
		setDoubleBuffered(false);
	}

	private void resetListeners()
	{
		this.m = super.getMouseListeners();
		this.mm = super.getMouseMotionListeners();
		this.mw = super.getMouseWheelListeners();
		this.k = super.getKeyListeners();
		
		listeners = new ArrayList<EventListener>();

		for (MouseListener l : this.m)
		{
			super.removeMouseListener(l);
			listeners.add(l);
		}
		for (MouseMotionListener m : this.mm)
		{
			super.removeMouseMotionListener(m);
			listeners.add(m);
		}
		for (MouseWheelListener mw : this.mw)
		{
			super.removeMouseWheelListener(mw);
			listeners.add(mw);
		}
		for (KeyListener k : this.k)
		{
			super.removeKeyListener(k);
			listeners.add(k);
		}

	}

	@Override
	public void paint(Graphics g)
	{
		if (enabled)
			super.paint(g);
	}

	@Override
	public void repaint()
	{
		if (enabled)
		{
			super.repaint();
			
			if (controller != null)
				controller.repaintParent();
		}
	}
	
	public ArrayList<EventListener> getEventListeners()
	{
		return listeners;
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

}
