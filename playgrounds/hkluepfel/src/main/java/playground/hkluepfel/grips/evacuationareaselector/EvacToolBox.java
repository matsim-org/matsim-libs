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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.matsim.contrib.grips.control.Controller;
import org.matsim.contrib.grips.control.ShapeFactory;
import org.matsim.contrib.grips.model.AbstractModule;
import org.matsim.contrib.grips.model.AbstractToolBox;
import org.matsim.contrib.grips.model.Constants;
import org.matsim.contrib.grips.model.shape.PolygonShape;
import org.matsim.contrib.grips.model.shape.Shape;

/**
 * the evacuation area selector tool box
 * 
 * - open button: opens the grips configuration file - save button: saves the
 * shape according to the destination given in the configuration
 * 
 * 
 * @author wdoering
 * 
 */
class EvacToolBox extends AbstractToolBox {
	private static final long serialVersionUID = 1L;
	private JButton openBtn;
	private JButton saveButton;

	EvacToolBox(AbstractModule module, Controller controller) {
		super(module, controller);

		this.setLayout(new BorderLayout());

		JPanel buttonPanel = new JPanel();

		this.openBtn = new JButton(locale.btOpen());
		this.saveButton = new JButton(locale.btSave());
		this.saveButton.setEnabled(false);

		this.openBtn.addActionListener(this);
		this.saveButton.addActionListener(this);

		if (this.controller.isStandAlone())
			buttonPanel.add(this.openBtn);

		buttonPanel.add(this.saveButton);

		this.add(buttonPanel, BorderLayout.SOUTH);

	}

	@Override
	public void setGoalAchieved(boolean goalAchieved) {
		this.saveButton.setEnabled(goalAchieved);
		super.setGoalAchieved(goalAchieved);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();

		if (cmd.equals(locale.btOpen())) {
			if (this.controller.openGripsConfig()) {
				this.controller.disableAllRenderLayers();

				// add network bounding box shape
				int shapeRendererId = controller.getVisualizer().getPrimaryShapeRenderLayer().getId();
				Rectangle2D bbRect = controller.getBoundingBox();
				controller.addShape(ShapeFactory.getNetBoxShape(shapeRendererId, bbRect, false));

				// deactivate evacuation shape
				Shape evacuationShape = this.controller.getShapeById(Constants.ID_EVACAREAPOLY);
				if (evacuationShape != null)
					evacuationShape.setVisible(false);

				this.controller.getVisualizer().getActiveMapRenderLayer().setPosition(this.controller.getCenterPosition());
				this.saveButton.setEnabled(false);
				this.controller.enableAllRenderLayers();
			}
		} else if (cmd.equals(locale.btSave())) {
			Shape shape = controller.getShapeById(Constants.ID_EVACAREAPOLY);

			if (shape instanceof PolygonShape) {
				this.goalAchieved = controller.saveShape(shape, controller.getGripsConfigModule().getEvacuationAreaFileName());
				this.controller.setGoalAchieved(this.goalAchieved);
				 
				//this.controller.setActiveModuleType(this.controller.getNextModules(module.getModuleType()).get(0));
						// (module.getModuleType()));
						// getModules().get(0).getModuleType());

				this.saveButton.setEnabled(false);
			}
		}

	}

}