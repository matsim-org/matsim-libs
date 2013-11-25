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

package org.matsim.contrib.grips.evacuationareaselector;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.control.ShapeFactory;
import org.matsim.contrib.grips.model.AbstractModule;
import org.matsim.contrib.grips.model.AbstractToolBox;
import org.matsim.contrib.grips.model.Constants;
import org.matsim.contrib.grips.model.imagecontainer.BufferedImageContainer;
import org.matsim.contrib.grips.model.process.BasicProcess;
import org.matsim.contrib.grips.model.process.DisableLayersProcess;
import org.matsim.contrib.grips.model.process.EnableLayersProcess;
import org.matsim.contrib.grips.model.process.InitGripsConfigProcess;
import org.matsim.contrib.grips.model.process.InitMainPanelProcess;
import org.matsim.contrib.grips.model.process.InitMapLayerProcess;
import org.matsim.contrib.grips.model.process.InitShapeLayerProcess;
import org.matsim.contrib.grips.model.process.SetModuleListenerProcess;
import org.matsim.contrib.grips.model.process.SetToolBoxProcess;
import org.matsim.contrib.grips.model.shape.CircleShape;
import org.matsim.contrib.grips.view.DefaultWindow;

/**
 * 
 * <code>InitProcess</code> <code>InnerEvacAreaListener</code>
 * 
 * @author wdoering
 * 
 */
public class EvacuationAreaSelector extends AbstractModule {

	public static void main(String[] args) {
		// set up controller and image interface
		final Controller controller = new Controller(args);
		controller.setImageContainer(BufferedImageContainer.getImageContainer(width, height, border));

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate evacuation area selector
		AbstractModule evacAreaSelector = new EvacuationAreaSelector(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		evacAreaSelector.start();
		frame.requestFocus();

	}

	public EvacuationAreaSelector(Controller controller) {
		super(controller.getLocale().moduleEvacAreaSelector(), Constants.ModuleType.EVACUATION, controller);

		// disable all layers
		this.processList.add(new DisableLayersProcess(controller));

		// initialize GRIPS config
		this.processList.add(new InitGripsConfigProcess(controller));

		// check if the default render panel is set
		this.processList.add(new InitMainPanelProcess(controller));

		// check if there is already a map viewer running, or just (re)set
		// center position
		this.processList.add(new InitMapLayerProcess(controller));

		// set module listeners
		this.processList.add(new SetModuleListenerProcess(controller, this, new EvacEventListener(controller)));

		// check if there is already a primary shape layer
		this.processList.add(new InitShapeLayerProcess(controller));

		// add bounding box
		this.processList.add(new BasicProcess(controller) {
			@Override
			public void start() {
				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, false));
			}

		});

		// add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));

		// enable all layers
		this.processList.add(new EnableLayersProcess(controller));

	}

	@Override
	public AbstractToolBox getToolBox() {
		if (this.toolBox == null)
			this.toolBox = new EvacToolBox(this, this.controller);

		return this.toolBox;
	}

	public Point getGeoPoint(Point mousePoint) {
		Rectangle viewPortBounds = this.controller.getViewportBounds();
		return new Point(mousePoint.x + viewPortBounds.x - offsetX, mousePoint.y + viewPortBounds.y - offsetY);
	}

	public void setEvacCircle(Point2D c0, Point2D c1) {
		CircleShape evacCircle = ShapeFactory.getEvacCircle(controller.getVisualizer().getPrimaryShapeRenderLayer().getId(), c0, c1);
		controller.addShape(evacCircle);
		this.controller.getVisualizer().getPrimaryShapeRenderLayer().updatePixelCoordinates(evacCircle);

	}

}
