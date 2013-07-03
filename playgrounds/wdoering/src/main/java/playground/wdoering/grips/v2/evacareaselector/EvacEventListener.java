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

package playground.wdoering.grips.v2.evacareaselector;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.control.ShapeFactory;
import playground.wdoering.grips.scenariomanager.control.eventlistener.AbstractListener;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.shape.CircleShape;
import playground.wdoering.grips.scenariomanager.model.shape.PolygonShape;
import playground.wdoering.grips.scenariomanager.model.shape.Shape;

/**
 * the map event listeners
 * 
 * @author wdoering
 * 
 */
class EvacEventListener extends AbstractListener {
	private Rectangle viewPortBounds;
	private int border;
	private int offsetX;
	private int offsetY;

	public EvacEventListener(Controller controller) {
		super(controller);
		this.border = controller.getImageContainer().getBorderWidth();
		this.offsetX = this.border;
		this.offsetY = this.border;

	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			// get origin
			this.controller.c0 = this.controller.pixelToGeo(getGeoPoint(e.getPoint()));
			this.controller.c1 = this.controller.c0;

			// set evacuation circle
			setEvacCircle(this.controller.c0, this.controller.c1);

			// drawing a circle (goal is not achieved yet), repaint and fix view
			controller.getActiveToolBox().setGoalAchieved(false);
			controller.paintLayers();
			this.fixed = true;

			// System.out.println("shapes" +
			// controller.getActiveShapes().size());

		}
		super.mousePressed(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.pressedButton == MouseEvent.BUTTON1) {
			// get destination
			this.controller.c1 = this.controller.pixelToGeo(getGeoPoint(e.getPoint()));

			// update circle
			CircleShape circle = (CircleShape) controller.getShapeById(Constants.ID_EVACAREAPOLY);
			circle.setDestination(this.controller.c1);
			this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(circle);

			// repaint
			controller.paintLayers();
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
				PolygonShape polygon = this.controller.getShapeUtils().getPolygonFromCircle(circle);
				this.controller.addShape(polygon);
				this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(polygon);
				controller.getActiveToolBox().setGoalAchieved(true);
				controller.paintLayers();
			}
		}

		super.mouseReleased(e);
	}

	public Point getGeoPoint(Point mousePoint) {
		viewPortBounds = this.controller.getViewportBounds();
		return new Point(mousePoint.x + viewPortBounds.x - offsetX, mousePoint.y + viewPortBounds.y - offsetY);
	}

	public void setEvacCircle(Point2D c0, Point2D c1) {
		CircleShape evacCircle = ShapeFactory.getEvacCircle(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), c0, c1);
		controller.addShape(evacCircle);
		this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(evacCircle);

	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		super.mouseWheelMoved(e);
		controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(true);
	}
}