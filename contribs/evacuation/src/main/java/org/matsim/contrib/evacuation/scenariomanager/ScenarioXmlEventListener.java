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

package org.matsim.contrib.evacuation.scenariomanager;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.eventlistener.AbstractListener;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.Constants.SelectionMode;
import org.matsim.contrib.evacuation.model.shape.CircleShape;
import org.matsim.contrib.evacuation.model.shape.PolygonShape;
import org.matsim.contrib.evacuation.model.shape.Shape;

/**
 * the map event listeners
 * 
 * @author wdoering
 * 
 */
class ScenarioXmlEventListener extends AbstractListener {
	private Rectangle viewPortBounds;
	private int border;
	private int offsetX;
	private int offsetY;
	private ArrayList<Point2D> points;

	public ScenarioXmlEventListener(Controller controller) {
		super(controller);
		this.border = controller.getImageContainer().getBorderWidth();
		this.offsetX = this.border;
		this.offsetY = this.border;
		this.points = new ArrayList<Point2D>();
		this.controller.setInSelection(false);

	}

	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.controller.getSelectionMode().equals(SelectionMode.CIRCLE)) {
			if (this.pressedButton == MouseEvent.BUTTON1) {
				// repaint
				controller.paintLayers();
			}
		} else if (this.controller.getSelectionMode().equals(
				SelectionMode.POLYGONAL)) {

		}
		super.mouseDragged(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.fixed = false;
		if (this.pressedButton == MouseEvent.BUTTON1) {
			Shape shape = controller.getShapeById(Constants.ID_EVACAREAPOLY);
			if (shape instanceof CircleShape) {
				CircleShape circle = (CircleShape) shape;
				PolygonShape polygon = this.controller.getShapeUtils()
						.getPolygonFromCircle(circle);
				this.controller.addShape(polygon);
				this.controller.getVisualizer().getPrimaryShapeRenderLayer()
						.updatePixelCoordinates(polygon);
				controller.getActiveToolBox().setGoalAchieved(true);
			} else if (shape instanceof PolygonShape) {
				if (points.size() > 4)
					controller.getActiveToolBox().setGoalAchieved(true);
			}
			controller.paintLayers();

		}

		super.mouseReleased(e);
	}

	@Override
	public void keyPressed(KeyEvent e) {
		super.keyPressed(e);
	}

}