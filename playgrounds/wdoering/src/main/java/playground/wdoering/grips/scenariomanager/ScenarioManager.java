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


package playground.wdoering.grips.scenariomanager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.MouseInputListener;

import playground.wdoering.grips.scenariomanager.control.Controller;
import playground.wdoering.grips.scenariomanager.model.AbstractModule;
import playground.wdoering.grips.scenariomanager.model.Constants;
import playground.wdoering.grips.scenariomanager.model.Constants.ModuleType;
import playground.wdoering.grips.scenariomanager.model.imagecontainer.BufferedImageContainer;
import playground.wdoering.grips.scenariomanager.view.DefaultWindow;
import playground.wdoering.grips.scenariomanager.view.TabButton;
import playground.wdoering.grips.v2.analysis.EvacuationAnalysis;
import playground.wdoering.grips.v2.evacareaselector.EvacAreaSelector;
import playground.wdoering.grips.v2.popareaselector.PopAreaSelector;
import playground.wdoering.grips.v2.ptlines.PTLEditor;
import playground.wdoering.grips.v2.roadclosures.RoadClosureEditor;
import playground.wdoering.grips.v2.scenariogenerator.MatsimScenarioGenerator;
import playground.wdoering.grips.v2.scenariogenerator.ScenarioGenerator;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

public class ScenarioManager extends DefaultWindow
{
	static int width = 1024;
	static int height = 720;
	static int border = 10;
	public ConcurrentHashMap<ModuleType, TabButton> selectionButtons;
	private ConcurrentHashMap<ModuleType, AbstractModule> modules;

	public static void main(String[] args)
	{
		final Controller controller = new Controller();
		BufferedImage image = new BufferedImage(width - border * 2, height - border * 2, BufferedImage.TYPE_INT_ARGB);
		BufferedImageContainer imageContainer = new BufferedImageContainer(image, border);
		controller.setImageContainer(imageContainer);
		controller.setMainFrameUndecorated(false);
		
		controller.setStandAlone(false);
		
		ArrayList<AbstractModule> moduleChain = new ArrayList<AbstractModule>();
		//current work flow
		moduleChain.add(new EvacAreaSelector(controller));
		moduleChain.add(new PopAreaSelector(controller));
		moduleChain.add(new ScenarioGenerator(controller));
		moduleChain.add(new RoadClosureEditor(controller));
		moduleChain.add(new PTLEditor(controller));
		moduleChain.add(new MatsimScenarioGenerator(controller));
		moduleChain.add(new EvacuationAnalysis(controller));
		
		controller.addModuleChain(moduleChain);
		
		ScenarioManager manager = new ScenarioManager(controller);
		
		// set parent component to forward the (re)paint event
		controller.setParentComponent(manager);		
		controller.setMainPanel(manager.getMainPanel(), true);
		
		
		manager.setVisible(true);
	}
	
	public ScenarioManager(Controller controller)
	{
		super(controller);
		
		selectionButtons = new ConcurrentHashMap<ModuleType, TabButton>();
		modules = new ConcurrentHashMap<ModuleType, AbstractModule>();
		
		Random random = new Random();
		JPanel tabPanel = new JPanel();
		JPanel tabSelectPanel = new JPanel();
		JPanel panels = new JPanel();
		panels.add(tabPanel);
//		panels.add(tabSelectPanel);
		panels.setMaximumSize(new Dimension(width, 120));
		panels.setPreferredSize(new Dimension(width, 120));
		panels.setBackground(Color.darkGray);
		tabPanel.setMinimumSize(new Dimension(width, 95));
		tabPanel.setPreferredSize(new Dimension(width, 120));
		
		tabSelectPanel.setMaximumSize(new Dimension(width, 20));
		tabPanel.setBackground(Color.darkGray);
		
		int i = 0;
		for (AbstractModule module : controller.getModules())
		{
			JPanel panel = new JPanel();
			panel.setPreferredSize(new Dimension(80,100));
			TabButton button = new TabButton(module.getModuleType(),panel,80,80);
			
			button.setIcon(new ImageIcon(Constants.getModuleImage(module.getModuleType())));
			
			button.setFocusPainted(false);
			
			ModuleButtonListener l = new ModuleButtonListener(this, module.getModuleType());
			button.addActionListener(l);
			button.addMouseMotionListener(l);
			button.addMouseListener(l);
			
			button.setColor(Constants.getModuleColor(module.getModuleType()));
			button.setHoverColor(button.getColor().brighter());
			
			if (i>0)
			{
				button.setEnabled(false);
				module.setEnabled(false);
			}
			else
				module.setEnabled(true);
			
			i++;
			
			selectionButtons.put(module.getModuleType(), button);
			modules.put(module.getModuleType(), module);
			
			panel.add(button);
			tabPanel.add(panel);
		}
		this.controller.setActiveModuleType(this.controller.getModules().get(0).getModuleType());
		
		this.add(panels,BorderLayout.NORTH);
		
		
	}
	
	private class ModuleButtonListener implements MouseInputListener, ActionListener
	{
		private ScenarioManager manager;
		private ModuleType moduleType;
		
		public ModuleButtonListener(ScenarioManager manager, ModuleType moduleType)
		{
			this.manager = manager;
			this.moduleType = moduleType;
		}
		@Override
		public void mouseDragged(MouseEvent e) {}
		@Override
		public void mouseMoved(MouseEvent e) {}
		@Override
		public void actionPerformed(ActionEvent e)
		{
			this.manager.controller.setActiveModuleType(this.moduleType);
			this.manager.modules.get(this.moduleType).start();
		}
		@Override
		public void mouseClicked(MouseEvent e) {}
		@Override
		public void mousePressed(MouseEvent e) {}
		@Override
		public void mouseReleased(MouseEvent e) {}
		@Override
		public void mouseEntered(MouseEvent e)
		{
			updateHoverColors(true);
			if (this.manager.selectionButtons.get(this.moduleType).isEnabled())
				this.manager.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		@Override
		public void mouseExited(MouseEvent e)
		{
			updateHoverColors(false);
			this.manager.setCursor(Cursor.getDefaultCursor());
//			if (this.manager.selectionButtons.get(this.moduleType).isEnabled())
		}
		
		private void updateHoverColors(boolean enter)
		{
			for (TabButton button : selectionButtons.values())
			{
				TabButton buttonFromList = this.manager.selectionButtons.get(button.getModuleType());
				
				if (enter)
				{
					if (buttonFromList.isEnabled())
						buttonFromList.hover(button.getModuleType().equals(this.moduleType));
				}
				else
					buttonFromList.hover(false);
			}
		}
		
	}
	
	@Override
	public void updateMask()
	{
		for (AbstractModule module : modules.values())
		{
			System.out.println(module.getModuleType() + " is enabled: " + module.isEnabled());
			this.selectionButtons.get(module.getModuleType()).setEnabled(module.isEnabled());
		}
	}
	
}

