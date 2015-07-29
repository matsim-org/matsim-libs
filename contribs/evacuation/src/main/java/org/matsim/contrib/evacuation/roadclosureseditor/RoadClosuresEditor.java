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

package org.matsim.contrib.evacuation.roadclosureseditor;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.control.ShapeFactory;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.imagecontainer.BufferedImageContainer;
import org.matsim.contrib.evacuation.model.process.BasicProcess;
import org.matsim.contrib.evacuation.model.process.DisableLayersProcess;
import org.matsim.contrib.evacuation.model.process.EnableLayersProcess;
import org.matsim.contrib.evacuation.model.process.InitEvacShapeProcess;
import org.matsim.contrib.evacuation.model.process.InitMainPanelProcess;
import org.matsim.contrib.evacuation.model.process.InitMapLayerProcess;
import org.matsim.contrib.evacuation.model.process.InitMatsimConfigProcess;
import org.matsim.contrib.evacuation.model.process.InitSecondaryShapeLayerProcess;
import org.matsim.contrib.evacuation.model.process.InitShapeLayerProcess;
import org.matsim.contrib.evacuation.model.process.SetModuleListenerProcess;
import org.matsim.contrib.evacuation.model.process.SetToolBoxProcess;
import org.matsim.contrib.evacuation.view.DefaultWindow;

public class RoadClosuresEditor extends AbstractModule {

	public static void main(String[] args) {
		// set up controller and image interface
		final Controller controller = new Controller(args);
		BufferedImage image = new BufferedImage(width - border * 2, height
				- border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(
				image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate road closure editor
		AbstractModule roadClosureEditor = new RoadClosuresEditor(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		roadClosureEditor.start();
		frame.requestFocus();
	}

	public RoadClosuresEditor(Controller controller) {
		super(controller.getLocale().moduleRoadClosureEditor(),
				Constants.ModuleType.ROADCLOSURE, controller);

		// disable all layers
		this.processList.add(new DisableLayersProcess(controller));

		// initialize Matsim config
		this.processList.add(new InitMatsimConfigProcess(controller));

		// check if the default render panel is set
		this.processList.add(new InitMainPanelProcess(controller));

		// check if there is already a map viewer running, or just (re)set
		// center position
		this.processList.add(new InitMapLayerProcess(controller));

		// check if the default shape layer is set
		this.processList.add(new InitShapeLayerProcess(controller));

		// check if the secondary shape layer is set
		this.processList.add(new InitSecondaryShapeLayerProcess(controller));

		// set module listeners
		this.processList.add(new SetModuleListenerProcess(controller, this,
				new RCEEventListener(controller)));

		// load evacuation area shape
		this.processList.add(new InitEvacShapeProcess(controller));

		// add bounding box
		this.processList.add(new BasicProcess(controller) {
			@Override
			public void start() {

				int shapeRendererId = controller.getVisualizer()
						.getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(
						shapeRendererId, bbRect, true));
			}

		});

		// add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));

		// enable all layers
		this.processList.add(new EnableLayersProcess(controller));

	}

	@Override
	public AbstractToolBox getToolBox() {
		if (toolBox == null)
			toolBox = new RCEToolBox(this, this.controller);

		return toolBox;
	}

}
