/******************************************************************************
 *project: org.matsim.*
 * VisModuleContainerImpl.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller.vismodule;

import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import playground.rost.controller.gui.BasicMapGUI;
import playground.rost.controller.map.BasicMap;
import playground.rost.controller.vismodule.VisModule.MoveLayerDirection;

public class VisModuleContainerImpl implements
		VisModuleContainer {

	protected JPanel panel;
	protected JButton btnUpdate;
	protected BasicMapGUI gui;
	
	List<VisModule> modules = new LinkedList<VisModule>();
	
	public VisModuleContainerImpl(BasicMapGUI gui)
	{
		this.panel = new JPanel();
		this.gui = gui;
	}
	
	public Container getContainer()
	{
		this.buildUI();
		return this.panel;
	}
	
	public void addVisModule(VisModule vM) {
		modules.add(vM);
	}

	public List<VisModule> getVisModuleOrder() {
		return modules;
	}

	public void paintMap(BasicMap map, Graphics g) {
		for(VisModule vM : modules)
		{
			vM.paintGraphics(map, g);
		}
	}

	public void removeVisModule(VisModule vM) {
		modules.remove(vM);
	}

	public void setVisModuleOrder(List<VisModule> newVMOrder) {
		modules = newVMOrder;
	}
	
	protected void buildUI()
	{
		this.panel.removeAll();
		this.panel.setLayout(new BoxLayout(this.panel, BoxLayout.Y_AXIS));
		for(VisModule vM : modules)
		{
			this.panel.add(vM.getUI());
		}
		btnUpdate = new JButton("update");
		panel.add(btnUpdate);
		btnUpdate.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				handleButtonClick(e);
			}
		});
	}
	
	public void handleButtonClick(ActionEvent e)
	{
		for(VisModule vM : modules)
		{
			vM.refreshAttributes();
		}
		gui.UIChange();
	}

	public void paint(BasicMap map, Graphics g) {
		for(VisModule vM : modules)
		{
			vM.paintGraphics(map, g);
		}
		
	}
	
	public void requestMoveLayer(VisModule vM, MoveLayerDirection direction)
	{
		int index = modules.indexOf(vM);
		if(index != -1)
		{
			if(direction == MoveLayerDirection.Up)
			{
				if(index == 0)
					return;
				modules.remove(index);
				modules.add(index-1, vM);
			}
			else
			{
				if(index == modules.size() -1)
					return;
				modules.remove(index);
				modules.add(index+1, vM);
			}
			
		}
		buildUI();
		this.panel.setVisible(false);
		this.panel.setVisible(true);
		gui.UIChange();
	}

}
