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

package org.matsim.contrib.evacuation.run;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import org.matsim.contrib.evacuation.analysis.EvacuationAnalysis;
import org.matsim.contrib.evacuation.control.Controller;
import org.matsim.contrib.evacuation.evacuationareaselector.EvacuationAreaSelector;
import org.matsim.contrib.evacuation.evacuationptlineseditor.EvacuationPTLinesEditor;
import org.matsim.contrib.evacuation.model.AbstractModule;
import org.matsim.contrib.evacuation.model.Constants;
import org.matsim.contrib.evacuation.model.ScenarioManagerModuleChain;
import org.matsim.contrib.evacuation.model.Constants.ModuleType;
import org.matsim.contrib.evacuation.model.imagecontainer.BufferedImageContainer;
import org.matsim.contrib.evacuation.populationselector.PopulationAreaSelector;
import org.matsim.contrib.evacuation.roadclosureseditor.RoadClosuresEditor;
import org.matsim.contrib.evacuation.scenariogenerator.MatsimNetworkGenerator;
import org.matsim.contrib.evacuation.scenariomanager.ScenarioXMLEditor;
import org.matsim.contrib.evacuation.simulation.SimulationComputation;
import org.matsim.contrib.evacuation.view.DefaultWindow;
import org.matsim.contrib.evacuation.view.TabButton;

public class ScenarioManager extends DefaultWindow {
	private static final long serialVersionUID = 1L;
	static int width = 1024;
	static int height = 864;
	static int border = 10;
	public ConcurrentHashMap<ModuleType, TabButton> selectionButtons;
	private ConcurrentHashMap<ModuleType, AbstractModule> modules;

	public static void main(String[] args) {
		final Controller controller = new Controller(args);
		controller.setImageContainer(BufferedImageContainer.getImageContainer(
				width, height, border));
		controller.setMainFrameUndecorated(false);

		controller.setModuleChain(new ScenarioManagerModuleChain());
		controller.setStandAlone(false);
		ArrayList<AbstractModule> moduleChain = new ArrayList<AbstractModule>();

		// current work flow
		moduleChain.add(new ScenarioXMLEditor(controller));
		moduleChain.add(new EvacuationAreaSelector(controller));
		moduleChain.add(new PopulationAreaSelector(controller));
		moduleChain.add(new MatsimNetworkGenerator(controller));
		moduleChain.add(new RoadClosuresEditor(controller));
		moduleChain.add(new EvacuationPTLinesEditor(controller));
		moduleChain.add(new SimulationComputation(controller));
		moduleChain.add(new EvacuationAnalysis(controller));

		controller.addModuleChain(moduleChain);

		ScenarioManager manager = new ScenarioManager(controller);

		// set parent component to forward the (re)paint event
		controller.setParentComponent(manager);
		controller.setMainPanel(manager.getMainPanel(), true);

		manager.setVisible(true);
	}

	public ScenarioManager(Controller controller) {
		super(controller);

		selectionButtons = new ConcurrentHashMap<ModuleType, TabButton>();
		modules = new ConcurrentHashMap<ModuleType, AbstractModule>();

		JPanel tabPanel = new JPanel();
		JPanel tabSelectPanel = new JPanel();
		JPanel panels = new JPanel();
		panels.add(tabPanel);
		panels.setMaximumSize(new Dimension(width, 120));
		panels.setPreferredSize(new Dimension(width, 120));
		panels.setBackground(Color.darkGray);
		tabPanel.setMinimumSize(new Dimension(width, 95));
		tabPanel.setPreferredSize(new Dimension(width, 120));

		tabSelectPanel.setMaximumSize(new Dimension(width, 20));
		tabPanel.setBackground(Color.darkGray);

		int i = 0;
		for (AbstractModule module : controller.getModules()) {

			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(80, 100));
			TabButton button = new TabButton(module.getModuleType(), panel, 80,
					80);

			button.setIcon(new ImageIcon(Constants.getModuleImage(module
					.getModuleType())));

			button.setFocusPainted(false);

			ModuleButtonListener l = new ModuleButtonListener(this,
					module.getModuleType());
			button.addActionListener(l);
			button.addMouseMotionListener(l);
			button.addMouseListener(l);

			button.setColor(Constants.getModuleColor(module.getModuleType()));
			button.setHoverColor(button.getColor().brighter());

			if (i > 0) {
				button.setEnabled(false);
				module.setEnabled(false);
			} else
				module.setEnabled(true);

			i++;

			selectionButtons.put(module.getModuleType(), button);
			modules.put(module.getModuleType(), module);

			panel.add(button);
			tabPanel.add(panel);
		}
		this.controller.setActiveModuleType(this.controller.getModules().get(0)
				.getModuleType());

		this.add(panels, BorderLayout.NORTH);

	}

	private class ModuleButtonListener implements MouseInputListener,
			ActionListener {
		private ScenarioManager manager;
		private ModuleType moduleType;

		public ModuleButtonListener(ScenarioManager manager,
				ModuleType moduleType) {
			this.manager = manager;
			this.moduleType = moduleType;
		}

		@Override
		public void mouseDragged(MouseEvent e) {
		}

		@Override
		public void mouseMoved(MouseEvent e) {
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			this.manager.controller.setActiveModuleType(this.moduleType);
			this.manager.modules.get(this.moduleType).start();
			AbstractModule module = this.manager.controller
					.getModuleByType(this.moduleType);
			this.manager.setTitle(module.getTitle());
		}

		@Override
		public void mouseClicked(MouseEvent e) {
		}

		@Override
		public void mousePressed(MouseEvent e) {
		}

		@Override
		public void mouseReleased(MouseEvent e) {
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			updateHoverColors(true);
			if (this.manager.selectionButtons.get(this.moduleType).isEnabled())
				this.manager.setCursor(Cursor
						.getPredefinedCursor(Cursor.HAND_CURSOR));
		}

		@Override
		public void mouseExited(MouseEvent e) {
			updateHoverColors(false);
			this.manager.setCursor(Cursor.getDefaultCursor());
			// if
			// (this.manager.selectionButtons.get(this.moduleType).isEnabled())
		}

		private void updateHoverColors(boolean enter) {
			for (TabButton button : selectionButtons.values()) {
				TabButton buttonFromList = this.manager.selectionButtons
						.get(button.getModuleType());

				if (enter) {
					if (buttonFromList.isEnabled())
						buttonFromList.hover(button.getModuleType().equals(
								this.moduleType));
				} else
					buttonFromList.hover(false);
			}
		}

	}

	@Override
	public void updateMask() {
		for (AbstractModule module : modules.values())
			this.selectionButtons.get(module.getModuleType()).setEnabled(
					module.isEnabled());
	}

}
