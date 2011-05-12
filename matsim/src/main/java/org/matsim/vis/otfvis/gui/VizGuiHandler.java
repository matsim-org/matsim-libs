/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.vis.otfvis.gui;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import javax.swing.event.MouseInputAdapter;

class VizGuiHandler extends MouseInputAdapter {
	private final OTFSwingDrawerContainer otfSwingDrawer;

	private MyNetVisScrollPane scrollPane;

	VizGuiHandler(OTFSwingDrawerContainer otfSwingDrawer, MyNetVisScrollPane scrollPane) {
		this.otfSwingDrawer = otfSwingDrawer;
		this.scrollPane = scrollPane;
	}

	public Point start = null;
	private int button = 0;

	public Rectangle currentRect = new Rectangle();;

	public void drawElements(Graphics2D g2) {
		g2.setColor(Color.GREEN);
		g2.draw(currentRect);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (currentRect.getHeight() > 10 && currentRect.getWidth() > 10) {
			float scale =  otfSwingDrawer.getScale();
			otfSwingDrawer.setScale(currentRect,scale);
			currentRect.setSize(0,0);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// It seems like the MouseEvent-Button is always 0 on Windows in the mouseDragged Event
		// so the button is stored in a variable when it is pressed
		if (button == 1) {
			currentRect.setFrameFromDiagonal(start, e.getPoint());
			otfSwingDrawer.repaint();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (e.getButton() == 1) {
			Point2D.Double origPoint = new Point2D.Double(e.getX(), e.getY());
			AffineTransform linkTransform = new AffineTransform( );
			linkTransform.concatenate(this.otfSwingDrawer.delegate.getBoxTransform());
			try {
				linkTransform = linkTransform.createInverse();
			} catch (NoninvertibleTransformException e1) {
				throw new RuntimeException(e1);
			}
			Point2D.Double transformedPoint = (Point2D.Double) linkTransform.transform(origPoint, null);
			this.otfSwingDrawer.handleClick(transformedPoint, e.getButton(), e);
			scrollPane.invalidate();
			scrollPane.repaint();
		}
	}

	private void pressed_ZOOM_OUT() {
		float scale = this.otfSwingDrawer.getScale() / 1.42f;
		if (scale > 0.02) {
			this.otfSwingDrawer.setScale(scale);
		}
	}

	private void pressed_ZOOM_IN() {
		float scale = this.otfSwingDrawer.getScale() * 1.42f;
		if ( scale < 100) {
			this.otfSwingDrawer.setScale(scale);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		int i = e.getWheelRotation();
		if (i>0) {
			pressed_ZOOM_OUT();
		} else if ( i<0) {
			pressed_ZOOM_IN();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		start = e.getPoint();
		button = e.getButton();
	}

}


class DragToScrollListener extends MouseAdapter {

	private MyNetVisScrollPane myNetVisScrollPane;

	private Point start = null;
	private int button = 0;

	public DragToScrollListener(MyNetVisScrollPane myNetVisScrollPane) {
		this.myNetVisScrollPane = myNetVisScrollPane;
	}

	@Override
	public void mousePressed(MouseEvent e) {
		System.out.println("pressed");
		start = e.getLocationOnScreen();
		button = e.getButton();
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// It seems like the MouseEvent-Button is always 0 on Windows in the mouseDragged Event
		// so the button is stored in a variable when it is pressed
		if (button == 2) {
			double deltax = start.x - e.getLocationOnScreen().getX();
			double deltay = start.y - e.getLocationOnScreen().getY();
			start = e.getLocationOnScreen();
			myNetVisScrollPane.moveNetwork((int) deltax, (int) deltay);
		}
	}

}