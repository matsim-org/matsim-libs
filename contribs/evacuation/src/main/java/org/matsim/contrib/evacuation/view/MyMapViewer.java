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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import org.jdesktop.swingx.JXMapViewer;

public class MyMapViewer extends JXMapViewer implements MouseListener, MouseWheelListener, KeyListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;
	
	private MouseListener m [];
	private MouseMotionListener mm [];
	private MouseWheelListener mw [];
	private KeyListener k [];

	public MyMapViewer()
	{
		super();
		resetListeners();
		setListeners();
	}

	private void resetListeners()
	{
		this.m = super.getMouseListeners();
		this.mm = super.getMouseMotionListeners();
		this.mw = super.getMouseWheelListeners();
		this.k = super.getKeyListeners();
		
		for (MouseListener l : this.m)
			super.removeMouseListener(l);
		for (MouseMotionListener m : this.mm)
			super.removeMouseMotionListener(m);
		for (MouseWheelListener mw : this.mw)
			super.removeMouseWheelListener(mw);
		for (KeyListener k : this.k)
			super.removeKeyListener(k);
	}

	private void setListeners()
	{
		this.addMouseListener(this);
		this.addMouseMotionListener(this);
		this.addMouseWheelListener(this);
		this.addKeyListener(this);
	}

	
	
	@Override
	public void mouseClicked(MouseEvent e) {}
	@Override
	public void mouseEntered(MouseEvent e) {}
	@Override
	public void mouseExited(MouseEvent e) {}
	@Override
	public void mousePressed(MouseEvent e) {}
	@Override
	public void mouseReleased(MouseEvent e) {}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {}
	@Override
	public void keyPressed(KeyEvent e) {}
	@Override
	public void keyReleased(KeyEvent e) {}
	@Override
	public void keyTyped(KeyEvent e) {}
	@Override
	public void mouseDragged(MouseEvent arg0) {}
	@Override
	public void mouseMoved(MouseEvent arg0) {}
	@Override
	public void paint(Graphics g) {}

}
