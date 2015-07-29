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

package org.matsim.contrib.evacuation.control.eventlistener;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.event.MouseInputListener;

import org.matsim.contrib.evacuation.control.Controller;

/**
 * class combining all needed listeners for the
 * interaction with the slippy map
 * 
 * @author wdoering
 *
 */
public class AbstractListener implements KeyListener, MouseWheelListener, MouseInputListener
{
	protected Controller controller;
	protected int pressedButton = -1;
	protected boolean fixed = false;
	
	public AbstractListener(Controller controller)
	{
		this.controller = controller;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mouseClicked(e);
	}

	@Override
	public void mousePressed(MouseEvent e)
	{

		this.pressedButton = e.getButton();
		
		if (this.pressedButton==MouseEvent.BUTTON3)
			controller.getParentComponent().setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		
		for (MouseListener m : controller.getMouseListener())
			m.mousePressed(e);
	}

	@Override
	public void mouseReleased(MouseEvent e)
	{
		this.pressedButton = -1;
		controller.getParentComponent().setCursor(Cursor.getDefaultCursor());
		
		for (MouseListener m : controller.getMouseListener())
			m.mouseReleased(e);		
	}

	@Override
	public void mouseEntered(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mouseEntered(e);		
	}

	@Override
	public void mouseExited(MouseEvent e)
	{
		for (MouseListener m : controller.getMouseListener())
			m.mouseExited(e);		
	}

	@Override
	public void mouseDragged(MouseEvent e)
	{
		updateMousePosition(e.getPoint());
		
		if ((!fixed) && (this.pressedButton == MouseEvent.BUTTON3))
			for (MouseMotionListener mm : controller.getMouseMotionListener())
				mm.mouseDragged(e);		
	}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		updateMousePosition(e.getPoint());
		
		for (MouseMotionListener mm : controller.getMouseMotionListener())
			mm.mouseMoved(e);		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
		if (!fixed)
			for (MouseWheelListener mw : controller.getMouseWheelListener())
				mw.mouseWheelMoved(e);
	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		for (KeyListener k : controller.getKeyListener())
			k.keyTyped(e);		
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		for (KeyListener k : controller.getKeyListener())
			k.keyPressed(e);
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		for (KeyListener k : controller.getKeyListener())
			k.keyReleased(e);
	}
	
	public KeyListener getKeyListener()
	{
		return this;
	}
	
	public MouseMotionListener getMouseMotionListener()
	{
		return this;
	}

	public MouseWheelListener getMouseWheelListener()
	{
		return this;
	}
	
	public void updateMousePosition(Point point)
	{
		this.controller.setMousePosition(point);
	}
	
	public void init()
	{
		
	}

	
	
}
