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

package org.matsim.contrib.evacuation.simulation;

import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;

import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.AbstractToolBox;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.imagecontainer.BufferedImageContainer;
import org.matsim.contrib.evacuation.model.process.BasicProcess;
import org.matsim.contrib.evacuation.view.DefaultWindow;

public class SimulationComputation extends AbstractModule {

	private SimulationMask msgMask;

	public static void main(String[] args) {
		final Controller controller = new Controller();

		// set up controller and image interface
		BufferedImage image = new BufferedImage(width - border * 2, height
				- border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(
				image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate evacuation area selector
		SimulationComputation scenario = new SimulationComputation(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);

		// start the process chain
		scenario.start();
		frame.requestFocus();
	}

	public SimulationComputation(Controller controller) {
		super(controller.getLocale().moduleMatsimScenarioGenerator(),
				Constants.ModuleType.MATSIMSCENARIO, controller);

		this.processList.add(new BasicProcess(this, this.controller) {
			@Override
			public void start() {
				// in case this is only part of something bigger
				controller.disableAllRenderLayers();

				msgMask = new SimulationMask(controller);
				this.controller.setMainPanel(msgMask, false);

				this.controller.setToolBoxVisible(false);

				// check if Matsim config (including the OSM network) has been
				// loaded
				if (!controller.isMatsimConfigOpened())
					if (!controller.openMastimConfig()) {
						JOptionPane.showConfirmDialog(msgMask,
								"Not a matsim file. Maybe an evacuation config file.",
								"Fatal error. Exiting.",
								JOptionPane.WARNING_MESSAGE);
						exit("Not a matsim file. Maybe a evacuation config file.");
					}

				msgMask.readConfig();

				// finally: enable all layers
				controller.enableAllRenderLayers();

			}
		});
	}

	@Override
	public AbstractToolBox getToolBox() {
		return null;
	}

}
