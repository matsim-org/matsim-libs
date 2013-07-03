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

package playground.wdoering.grips.v2.ptlines;

import java.awt.image.BufferedImage;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.AbstractToolBox;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.model.process.BasicProcess;
import playground.wdoering.grips.scenariomanager.model.process.DisableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.EnableLayersProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitBBShapeProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitEvacShapeProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMainPanelProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMapLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitMatsimConfigProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitSecondaryShapeLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.InitShapeLayerProcess;
import playground.wdoering.grips.scenariomanager.model.process.SetModuleListenerProcess;
import playground.wdoering.grips.scenariomanager.model.process.SetToolBoxProcess;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;

public class PTLEditor extends AbstractModule {

	public static void main(String[] args) {
		// set up controller and image interface
		final Controller controller = new Controller();
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate evacuation area selector
		AbstractModule publicTransitLinesEditor = new PTLEditor(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);
		controller.setMainPanel(frame.getMainPanel(), true);

		// start the process chain
		publicTransitLinesEditor.start();
		frame.requestFocus();
	}

	public PTLEditor(Controller controller) {
		super(controller.getLocale().modulePTLEditor(), Constants.ModuleType.BUSSTOPS, controller);

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
		this.processList.add(new SetModuleListenerProcess(controller, this, new PTLEventListener(controller)));

		// load evacuation area shape
		this.processList.add(new InitEvacShapeProcess(controller));

		this.processList.add(new BasicProcess(controller) {
			@Override
			public void start() {
				if (!controller.isPopulationFileOpened())
					controller.openPopulationFile();

				controller.openNetworkChangeEvents();
			}
		});

		// add bounding box
		this.processList.add(new InitBBShapeProcess(controller));

		// add toolbox
		this.processList.add(new SetToolBoxProcess(controller, getToolBox()));

		// enable all layers
		this.processList.add(new EnableLayersProcess(controller));

	}

	@Override
	public AbstractToolBox getToolBox() {
		return new PTLToolBox(this, this.controller);
	}


}
