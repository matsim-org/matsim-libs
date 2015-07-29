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

package org.matsim.contrib.evacuation.evacuationareaselector;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.ShapeFactory;
import org.matsim.contrib.evacuation.control.eventlistener.AbstractListener;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.Constants.SelectionMode;
import org.matsim.contrib.evacuation.model.shape.CircleShape;
import org.matsim.contrib.evacuation.model.shape.LineShape;
import org.matsim.contrib.evacuation.model.shape.PolygonShape;
import org.matsim.contrib.evacuation.model.shape.Shape;

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
	private ArrayList<Point2D> points;

	public EvacEventListener(Controller controller) {
		super(controller);
		this.border = controller.getImageContainer().getBorderWidth();
		this.offsetX = this.border;
		this.offsetY = this.border;
		this.points = new ArrayList<Point2D>();
		this.controller.setInSelection(false);
		

	}

	@Override
	public void mousePressed(MouseEvent e) {
		
		if (this.controller.getSelectionMode().equals(SelectionMode.CIRCLE))
		{
		
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
	
			}
		}
		else if ((e.getButton() == e.BUTTON1) && (this.controller.getSelectionMode().equals(SelectionMode.POLYGONAL)))
		{
			Point2D point = this.controller.pixelToGeo(getGeoPoint(e.getPoint()));
			
			if (!this.controller.isInSelection())
			{
				this.points.clear();
				this.controller.setInSelection(true);
			}
			points.add(point);
			if (points.size()>3)
				setEvacPoly(points);
			
			controller.paintLayers();
		}
		super.mousePressed(e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (this.controller.getSelectionMode().equals(SelectionMode.CIRCLE))
		{
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
		}
		else if (this.controller.getSelectionMode().equals(SelectionMode.POLYGONAL))
		{
			
		}
		super.mouseDragged(e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		this.fixed = false;
		if (this.pressedButton == MouseEvent.BUTTON1) {
			Shape shape = controller.getShapeById(Constants.ID_EVACAREAPOLY);
			if (shape instanceof CircleShape)
			{
				CircleShape circle = (CircleShape) shape;
				PolygonShape polygon = this.controller.getShapeUtils().getPolygonFromCircle(circle);
				this.controller.addShape(polygon);
				this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(polygon);
				controller.getActiveToolBox().setGoalAchieved(true);
			}
			else if (shape instanceof PolygonShape)
			{
				if (points.size()>4)
					controller.getActiveToolBox().setGoalAchieved(true);
			}
			controller.paintLayers();
			controller.setUnsavedChanges(true);
				
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
	
	public void setEvacPoly(ArrayList<Point2D> points) {
		
			
		Shape evacPoly = ShapeFactory.getEvacPoly(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), points);
		controller.addShape(evacPoly);
		
		
		this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(evacPoly);
		
		
		
	}
	
	

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		super.mouseWheelMoved(e);
		controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(true);
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		super.keyPressed(e);
	}

}