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

package org.matsim.contrib.grips.scenariogenerator;

import java.awt.image.BufferedImage;

import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.model.AbstractModule;
import org.matsim.contrib.grips.model.AbstractToolBox;
import org.matsim.contrib.grips.model.Constants;
import org.matsim.contrib.grips.model.imagecontainer.BufferedImageContainer;
import org.matsim.contrib.grips.model.process.BasicProcess;
import org.matsim.contrib.grips.view.DefaultWindow;

public class RunMatsim extends AbstractModule {

	private RunMatsimToolBox msgMask;
	public static void main(String[] args) {
		final Controller controller = new Controller();

		// set up controller and image interface
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);

		// inform controller that this module is running stand alone
		controller.setStandAlone(true);

		// instantiate evacuation area selector
		RunMatsim scenario = new RunMatsim(controller);

		// create default window for running this module standalone
		DefaultWindow frame = new DefaultWindow(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(frame);

		// start the process chain
		scenario.start();
		frame.requestFocus();
	}

	public RunMatsim(Controller controller) {
		super(controller.getLocale().moduleMatsimScenarioGenerator(), Constants.ModuleType.MATSIMSCENARIO, controller);

		this.processList.add(new BasicProcess(this, this.controller) {
			@Override
			public void start() {
				// in case this is only part of something bigger
				controller.disableAllRenderLayers();

				msgMask = new RunMatsimToolBox(controller);
				this.controller.setMainPanel(msgMask, false);

				this.controller.setToolBoxVisible(false);

				// check if Matsim config (including the OSM network) has been
				// loaded
				if (!controller.isMatsimConfigOpened())
					if (!controller.openMastimConfig())
						exit(locale.msgOpenMatsimConfigFailed());

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
