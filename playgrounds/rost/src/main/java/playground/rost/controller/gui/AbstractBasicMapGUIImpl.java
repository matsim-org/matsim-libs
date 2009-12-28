/******************************************************************************
 *project: org.matsim.*
 * AbstractBasicMapGUIImpl.java
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


package playground.rost.controller.gui;

import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import playground.rost.controller.map.BasicMap;
import playground.rost.controller.map.BasicMapImpl;
import playground.rost.controller.vismodule.VisModuleContainer;
import playground.rost.controller.vismodule.VisModuleContainerImpl;

public abstract class AbstractBasicMapGUIImpl extends JInternalFrame implements BasicMapGUI, InternalFrameListener{

	protected VisModuleContainer vMContainer;
	protected BasicMap map;
	protected JPanel ownContainer;
	
	public AbstractBasicMapGUIImpl(String title)
	{
		super(title, true, true, true, true);
		
		this.setLayout(new BorderLayout());
		
		ownContainer = new JPanel();
		
		this.setSize(1000,600);
        super.setDefaultCloseOperation(JInternalFrame.EXIT_ON_CLOSE);
        System.out.println("fuck: " + this.getDefaultCloseOperation());
        setVisible(true);
		vMContainer = new VisModuleContainerImpl(this);
		map = new BasicMapImpl();
		this.addInternalFrameListener(this);
	}
	
	public void buildUI()
	{
		this.add(ownContainer, BorderLayout.NORTH);
		this.add(map.getContainer(), BorderLayout.CENTER);
		this.add(vMContainer.getContainer(), BorderLayout.EAST);
		this.setVisible(false);
		this.setVisible(true);
	}
	
	public BasicMap getMap() {
		return map;
	}

	public VisModuleContainer getVisModuleContainer() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public Container getCustomContainer()
	{
		return this.ownContainer;
	}
	
	public void UIChange()
	{
		this.map.UIChange();
		this.repaint();
	}
	
	public void internalFrameClosing(InternalFrameEvent e) {
		this.dispose();
		System.out.println("fuck!");
	}
	
	public void internalFrameClosed(InternalFrameEvent e) {
	}
	
	public void internalFrameOpened(InternalFrameEvent e) {
	}
	
	public void internalFrameIconified(InternalFrameEvent e) {
	}
	
	public void internalFrameDeiconified(InternalFrameEvent e) {
	}
	
	public void internalFrameActivated(InternalFrameEvent e) {
	}
	
	public void internalFrameDeactivated(InternalFrameEvent e) {
	}


}
